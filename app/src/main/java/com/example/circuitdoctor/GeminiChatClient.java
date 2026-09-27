package com.example.circuitdoctor;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiChatClient {

    // อ่าน key จาก local.properties ผ่าน BuildConfig (ห้ามใส่ " " ครอบ)
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    // ลองรุ่นแรกก่อน ถ้าเซิร์ฟเวอร์ไม่ว่างจะสลับไปรุ่นถัดไป
    private static final String[] MODELS = {
            "gemini-flash-latest",
            "gemini-flash-lite-latest"
    };

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // จำนวนครั้งที่ลองใหม่ต่อ 1 รุ่น (รอ 1, 2, 4 วินาทีตามลำดับ)
    private static final int RETRIES_PER_MODEL = 3;
    private static final long BASE_DELAY_MS = 1000;

    // ส่งประวัติแชทไปแค่ข้อความล่าสุดจำนวนนี้
    private static final int MAX_HISTORY = 20;

    private static final String SYSTEM_INSTRUCTION =
            "คุณคือผู้ช่วย AI ในแอพ Circuit Doctor ตอบคำถามเกี่ยวกับวงจรไฟฟ้า อิเล็กทรอนิกส์ " +
                    "และการซ่อม/ต่อวงจรเบื้องต้น ตอบเป็นภาษาไทย กระชับ เข้าใจง่าย";

    public interface Callback {
        void onSuccess(String replyText);
        void onError(String message);
    }

    /** error จาก API ที่เก็บ status code ไว้ ใช้ตัดสินว่าควรลองใหม่หรือสลับรุ่น */
    private static class ApiException extends IOException {
        final int status;

        ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        boolean isTemporary() {
            return status == 429 || status == 500 || status == 503 || status == 504;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public void sendMessage(List<ChatMessage> history, Callback callback) {
        if (TextUtils.isEmpty(API_KEY)) {
            callback.onError("ยังไม่ได้ตั้งค่า GEMINI_API_KEY ใน local.properties");
            return;
        }

        executor.execute(() -> {
            try {
                String reply = callWithFallback(history);
                mainHandler.post(() -> callback.onSuccess(reply));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // ผู้ใช้ออกจากหน้าแชทแล้ว
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "เกิดข้อผิดพลาดที่ไม่ทราบสาเหตุ";
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    /** เรียกตอนปิดหน้าแชท เพื่อหยุด thread เบื้องหลัง */
    public void shutdown() {
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private String callWithFallback(List<ChatMessage> history)
            throws IOException, InterruptedException {
        String requestBody = buildRequestBody(history);
        ApiException lastError = null;

        for (String model : MODELS) {
            for (int attempt = 0; attempt < RETRIES_PER_MODEL; attempt++) {
                try {
                    return callGemini(model, requestBody);
                } catch (ApiException e) {
                    lastError = e;

                    // รุ่นนี้ใช้ไม่ได้กับ key นี้ → ข้ามไปรุ่นถัดไปเลย
                    if (e.status == 404) break;

                    // error ถาวร เช่น key ผิด → ลองใหม่ก็ไม่หาย หยุดเลย
                    if (!e.isTemporary()) throw e;

                    // รอนานขึ้นเรื่อยๆ: 1s, 2s, 4s
                    if (attempt < RETRIES_PER_MODEL - 1) {
                        Thread.sleep(BASE_DELAY_MS * (1L << attempt));
                    }
                }
            }
        }

        if (lastError != null) throw lastError;
        throw new IOException("ไม่สามารถติดต่อ AI ได้");
    }

    private String callGemini(String model, String requestBody) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASE_URL + model + ":generateContent");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("x-goog-api-key", API_KEY);
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            boolean ok = status >= 200 && status < 300;
            InputStream stream = ok ? connection.getInputStream() : connection.getErrorStream();
            String responseText = readStream(stream);

            if (!ok) {
                throw new ApiException(status, friendlyError(status, responseText));
            }

            return parseReply(responseText);
        } catch (java.net.SocketTimeoutException e) {
            // หมดเวลารอ ถือว่าเซิร์ฟเวอร์ไม่ว่าง ให้ลองใหม่ได้
            throw new ApiException(504, "AI ตอบช้าเกินไป ลองใหม่อีกครั้ง");
        } catch (java.net.UnknownHostException e) {
            throw new IOException("ไม่มีการเชื่อมต่ออินเทอร์เน็ต");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String buildRequestBody(List<ChatMessage> history) throws IOException {
        try {
            JSONArray contents = new JSONArray();

            int start = Math.max(0, history.size() - MAX_HISTORY);
            boolean foundFirstUser = false;

            for (int i = start; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                boolean isUser = msg.role == ChatMessage.ROLE_USER;

                // Gemini ต้องการให้ข้อความแรกเป็นของ user
                if (!foundFirstUser && !isUser) continue;
                foundFirstUser = true;

                if (TextUtils.isEmpty(msg.text)) continue;

                JSONObject part = new JSONObject();
                part.put("text", msg.text);

                JSONObject content = new JSONObject();
                content.put("role", isUser ? "user" : "model");
                content.put("parts", new JSONArray().put(part));
                contents.put(content);
            }

            JSONObject sysPart = new JSONObject();
            sysPart.put("text", SYSTEM_INSTRUCTION);
            JSONObject systemInstruction = new JSONObject();
            systemInstruction.put("parts", new JSONArray().put(sysPart));

            JSONObject body = new JSONObject();
            body.put("contents", contents);
            body.put("system_instruction", systemInstruction);
            return body.toString();
        } catch (Exception e) {
            throw new IOException("สร้างคำขอไม่สำเร็จ: " + e.getMessage());
        }
    }

    private String parseReply(String responseText) throws IOException {
        try {
            JSONObject json = new JSONObject(responseText);
            JSONArray candidates = json.optJSONArray("candidates");

            if (candidates == null || candidates.length() == 0) {
                throw new IOException("AI ไม่สามารถตอบคำถามนี้ได้ ลองเปลี่ยนคำถามดู");
            }

            JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
            JSONArray parts = content != null ? content.optJSONArray("parts") : null;
            if (parts == null || parts.length() == 0) {
                throw new IOException("AI ไม่ได้ส่งคำตอบกลับมา ลองใหม่อีกครั้ง");
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                sb.append(parts.getJSONObject(i).optString("text", ""));
            }
            return sb.toString().trim();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("อ่านผลลัพธ์จาก AI ไม่สำเร็จ");
        }
    }

    /** แปลง error จาก API เป็นข้อความที่ผู้ใช้อ่านเข้าใจ (มีเลข status ไว้ช่วยหาสาเหตุ) */
    private String friendlyError(int status, String responseText) {
        String apiMessage = "";
        try {
            JSONObject error = new JSONObject(responseText).optJSONObject("error");
            if (error != null) apiMessage = error.optString("message", "");
        } catch (Exception ignored) {
        }

        switch (status) {
            case 400:
                if (apiMessage.contains("API key")) {
                    return "API Key ไม่ถูกต้อง ตรวจสอบค่าใน local.properties";
                }
                return "คำขอไม่ถูกต้อง (400): " + apiMessage;
            case 403:
                return "API Key ไม่มีสิทธิ์ใช้งาน (403)";
            case 404:
                return "ไม่พบรุ่น AI ที่เลือก (404)";
            case 429:
                return "ใช้งานถี่เกินไปหรือโควตาหมด รอสักครู่แล้วลองใหม่ (429)";
            case 500:
            case 503:
            case 504:
                return "เซิร์ฟเวอร์ AI ไม่ว่างในขณะนี้ ลองใหม่อีกครั้ง (" + status + ")";
            default:
                return "เกิดข้อผิดพลาด (" + status + ") " + apiMessage;
        }
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}