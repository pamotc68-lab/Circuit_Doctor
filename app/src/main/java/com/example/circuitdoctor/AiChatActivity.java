package com.example.circuitdoctor;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AiChatActivity extends AppCompatActivity {

    private ImageView btnChatBack;
    private RecyclerView recyclerChatMessages;
    private EditText etChatInput;
    private TextView btnChatSend;
    private ProgressBar progressChatLoading;

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private final GeminiChatClient chatClient = new GeminiChatClient();
    private boolean waitingForReply = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        new UsageLogRepository(this).logVisit("แชทกับ AI");

        bindViews();
        setupRecycler();
        setupClickListeners();

        addMessage(new ChatMessage(ChatMessage.ROLE_AI, getString(R.string.chat_greeting)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chatClient.shutdown();
    }

    private void bindViews() {
        btnChatBack = findViewById(R.id.btnChatBack);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        etChatInput = findViewById(R.id.etChatInput);
        btnChatSend = findViewById(R.id.btnChatSend);
        progressChatLoading = findViewById(R.id.progressChatLoading);
    }

    private void setupRecycler() {
        adapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // ให้ข้อความล่าสุดอยู่ล่างสุดเสมอ
        recyclerChatMessages.setLayoutManager(layoutManager);
        recyclerChatMessages.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnChatBack.setOnClickListener(v -> finish());
        btnChatSend.setOnClickListener(v -> handleSend());

        // กดปุ่ม Send บนคีย์บอร์ดก็ส่งได้
        etChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSend();
                return true;
            }
            return false;
        });
    }

    private void handleSend() {
        if (waitingForReply) return;

        String text = etChatInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        addMessage(new ChatMessage(ChatMessage.ROLE_USER, text));
        etChatInput.setText("");
        setWaiting(true);

        chatClient.sendMessage(new ArrayList<>(messages), new GeminiChatClient.Callback() {
            @Override
            public void onSuccess(String replyText) {
                if (isFinishing() || isDestroyed()) return;
                setWaiting(false);
                addMessage(new ChatMessage(ChatMessage.ROLE_AI, replyText));
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                setWaiting(false);

                // เอาข้อความที่ส่งไม่สำเร็จกลับไปไว้ในช่องพิมพ์ ให้กดส่งใหม่ได้
                removeLastUserMessage();
                etChatInput.setText(text);
                etChatInput.setSelection(text.length());

                Toast.makeText(AiChatActivity.this,
                        getString(R.string.chat_error_prefix) + " " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerChatMessages.scrollToPosition(messages.size() - 1);
    }

    private void removeLastUserMessage() {
        int last = messages.size() - 1;
        if (last >= 0 && messages.get(last).role == ChatMessage.ROLE_USER) {
            messages.remove(last);
            adapter.notifyItemRemoved(last);
        }
    }

    private void setWaiting(boolean waiting) {
        waitingForReply = waiting;
        progressChatLoading.setVisibility(waiting ? View.VISIBLE : View.GONE);
        btnChatSend.setEnabled(!waiting);
        btnChatSend.setAlpha(waiting ? 0.5f : 1f);
    }
}