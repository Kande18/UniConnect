package com.uniconnect.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages active user session, local credential cache, and backend authentication.
 * Guarantees proper credential validation, dynamic user profiling, and offline resilience.
 */
public class UserManager {

    private static final String PREF_NAME = "uniconnect_user_prefs";
    private static final String KEY_LOGGED_IN = "key_logged_in";
    private static final String KEY_STUDENT_NUMBER = "key_student_number";
    private static final String KEY_FULL_NAME = "key_full_name";
    private static final String KEY_PROGRAMME = "key_programme";
    private static final String KEY_LAB_GROUP = "key_lab_group";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_AUTH_TOKEN = "key_auth_token";
    private static final String KEY_LOCAL_ACCOUNTS = "key_local_accounts";

    // Default backend URL (supports Android Emulator and local development)
    private static final String BACKEND_BASE_URL = "http://10.0.2.2:5000/api";

    private static UserManager instance;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onSuccess(String studentNumber, String fullName, String programme, String labGroup);
        void onError(String errorMessage);
    }

    private UserManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        seedDefaultAccountsIfEmpty();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    /**
     * Seeds initial known accounts corresponding to backend database seeds.
     */
    private void seedDefaultAccountsIfEmpty() {
        if (!prefs.contains(KEY_LOCAL_ACCOUNTS)) {
            try {
                JSONArray accounts = new JSONArray();

                accounts.put(createAccountJson("202400123", "Peculiar Banda", "CS", "G01", "Password123!", "peculiar@uniconnect.edu"));
                accounts.put(createAccountJson("202400456", "Amara Dlamini", "IT", "G01", "Password123!", "amara@uniconnect.edu"));
                accounts.put(createAccountJson("202400789", "Tendai Moyo", "DS", "G02", "Password123!", "tendai@uniconnect.edu"));
                accounts.put(createAccountJson("202401011", "Nkosi Zulu", "CS", "G02", "Password123!", "nkosi@uniconnect.edu"));
                accounts.put(createAccountJson("202401234", "Fatima Patel", "IT", "G03", "Password123!", "fatima@uniconnect.edu"));
                accounts.put(createAccountJson("202401567", "Sipho Ndlovu", "DS", "G03", "Password123!", "sipho@uniconnect.edu"));
                accounts.put(createAccountJson("202401890", "Grace Mwangi", "CS", "G04", "Password123!", "grace@uniconnect.edu"));

                prefs.edit().putString(KEY_LOCAL_ACCOUNTS, accounts.toString()).apply();
            } catch (Exception ignored) {}
        }
    }

    private JSONObject createAccountJson(String studentNumber, String fullName, String programme, String labGroup, String password, String email) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("studentNumber", studentNumber);
        obj.put("fullName", fullName);
        obj.put("programme", programme);
        obj.put("labGroup", labGroup);
        obj.put("password", password);
        obj.put("email", email);
        return obj;
    }

    /**
     * Attempts authentication with backend API, falling back to verified local accounts.
     * NEVER accepts arbitrary or random credentials.
     */
    public void attemptLogin(String studentNumber, String password, AuthCallback callback) {
        new Thread(() -> {
            boolean networkSuccess = false;

            try {
                URL url = new URL(BACKEND_BASE_URL + "/auth/student-login");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(2500);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("studentNumber", studentNumber);
                body.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(sb.toString());
                    if (res.optBoolean("success", true) && res.has("data")) {
                        JSONObject data = res.getJSONObject("data");
                        String token = data.optString("token", "");
                        JSONObject user = data.getJSONObject("user");
                        String name = user.optString("name", "Student");
                        String prog = user.optString("programme", "CS");
                        String group = user.optString("labGroup", "G01");

                        saveSession(studentNumber, name, prog, group, "", token);
                        networkSuccess = true;
                        mainHandler.post(() -> callback.onSuccess(studentNumber, name, prog, group));
                        return;
                    }
                } else if (code == 400 || code == 401 || code == 403) {
                    // Backend explicitly rejected the credentials
                    mainHandler.post(() -> callback.onError("Wrong credentials. Please check your student number and password."));
                    return;
                }
            } catch (Exception ignored) {
                // Network unreachable or timeout -> fall back to verified local store
            }

            if (!networkSuccess) {
                // Verify against verified local store
                JSONObject matched = findLocalAccount(studentNumber, password);
                if (matched != null) {
                    String name = matched.optString("fullName", "Student");
                    String prog = matched.optString("programme", "CS");
                    String group = matched.optString("labGroup", "G01");
                    String email = matched.optString("email", "");

                    saveSession(studentNumber, name, prog, group, email, "local-verified-token");
                    mainHandler.post(() -> callback.onSuccess(studentNumber, name, prog, group));
                } else {
                    mainHandler.post(() -> callback.onError("Wrong credentials. Please check your student number and password."));
                }
            }
        }).start();
    }

    /**
     * Registers a new student account locally and syncs to backend.
     */
    public boolean registerAccount(String studentNumber, String fullName, String programme, String claimCode, String password, String email) {
        try {
            String existingStr = prefs.getString(KEY_LOCAL_ACCOUNTS, "[]");
            JSONArray accounts = new JSONArray(existingStr);

            // Check duplicate
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                if (studentNumber.equals(acc.optString("studentNumber"))) {
                    return false; // Duplicate student number
                }
            }

            // Assign default group based on programme
            String group = "G01";
            if ("IT".equals(programme)) group = "G02";
            else if ("DS".equals(programme)) group = "G03";

            JSONObject newAcc = createAccountJson(studentNumber, fullName, programme, group, password, email);
            accounts.put(newAcc);
            prefs.edit().putString(KEY_LOCAL_ACCOUNTS, accounts.toString()).apply();

            // Fire-and-forget sync to backend
            new Thread(() -> {
                try {
                    URL url = new URL(BACKEND_BASE_URL + "/auth/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(3000);
                    conn.setDoOutput(true);

                    JSONObject body = new JSONObject();
                    body.put("studentNumber", studentNumber);
                    body.put("name", fullName);
                    body.put("programme", programme);
                    body.put("claimCode", claimCode);
                    body.put("password", password);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    conn.getResponseCode();
                } catch (Exception ignored) {}
            }).start();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a Google email is linked to any existing student.
     */
    public JSONObject findAccountByEmail(String email) {
        if (TextUtils.isEmpty(email)) return null;
        try {
            String existingStr = prefs.getString(KEY_LOCAL_ACCOUNTS, "[]");
            JSONArray accounts = new JSONArray(existingStr);
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                if (email.equalsIgnoreCase(acc.optString("email"))) {
                    return acc;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private JSONObject findLocalAccount(String studentNumber, String password) {
        try {
            String existingStr = prefs.getString(KEY_LOCAL_ACCOUNTS, "[]");
            JSONArray accounts = new JSONArray(existingStr);
            for (int i = 0; i < accounts.length(); i++) {
                JSONObject acc = accounts.getJSONObject(i);
                if (studentNumber.equals(acc.optString("studentNumber")) &&
                    password.equals(acc.optString("password"))) {
                    return acc;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void saveSession(String studentNumber, String fullName, String programme, String labGroup, String email, String token) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_STUDENT_NUMBER, studentNumber)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_PROGRAMME, programme)
                .putString(KEY_LAB_GROUP, labGroup)
                .putString(KEY_EMAIL, email)
                .putString(KEY_AUTH_TOKEN, token)
                .apply();
    }

    public void updateProfile(String fullName, String programme) {
        prefs.edit()
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_PROGRAMME, programme)
                .apply();
    }

    public void updateLabGroup(String labGroup) {
        prefs.edit().putString(KEY_LAB_GROUP, labGroup).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getStudentNumber() {
        return prefs.getString(KEY_STUDENT_NUMBER, "202400123");
    }

    public String getFullName() {
        return prefs.getString(KEY_FULL_NAME, "Student");
    }

    public String getProgramme() {
        return prefs.getString(KEY_PROGRAMME, "CS");
    }

    public String getLabGroup() {
        return prefs.getString(KEY_LAB_GROUP, "G01");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, "");
    }

    public void logout() {
        prefs.edit()
                .remove(KEY_LOGGED_IN)
                .remove(KEY_STUDENT_NUMBER)
                .remove(KEY_FULL_NAME)
                .remove(KEY_PROGRAMME)
                .remove(KEY_LAB_GROUP)
                .remove(KEY_EMAIL)
                .remove(KEY_AUTH_TOKEN)
                .apply();
    }
}
