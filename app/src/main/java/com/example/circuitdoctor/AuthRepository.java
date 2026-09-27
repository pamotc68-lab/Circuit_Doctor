package com.example.circuitdoctor;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthRepository {

    private static final String PREFS_NAME = "circuit_doctor_prefs";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_PASSWORD = "key_password";
    private static final String KEY_IS_LOGGED_IN = "key_is_logged_in";
    private static final String KEY_IS_GUEST = "key_is_guest";

    private final SharedPreferences prefs;

    public AuthRepository(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** สมัครสมาชิกใหม่ แล้วจดจำไว้อัตโนมัติสำหรับการเข้าแอพครั้งต่อไป */
    public void register(String email, String password) {
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST, false)
                .apply();
    }

    /** ตรวจสอบอีเมล/รหัสผ่านกับข้อมูลที่จำไว้ */
    public boolean login(String email, String password) {
        String savedEmail = prefs.getString(KEY_EMAIL, null);
        String savedPassword = prefs.getString(KEY_PASSWORD, null);

        boolean matched = savedEmail != null
                && savedEmail.equals(email)
                && savedPassword != null
                && savedPassword.equals(password);

        if (matched) {
            prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putBoolean(KEY_IS_GUEST, false)
                    .apply();
        }
        return matched;
    }

    /** เข้าใช้งานแบบผู้เยี่ยมชม (ไม่ต้องมีบัญชี) */
    public void loginAsGuest() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putBoolean(KEY_IS_GUEST, true)
                .apply();
    }

    public boolean hasRegisteredAccount() {
        return prefs.getString(KEY_EMAIL, null) != null;
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    public void logout() {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .putBoolean(KEY_IS_GUEST, false)
                .apply();
    }
    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }
}