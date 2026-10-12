package com.uniconnect.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyncWorker extends Worker {

    private static final String PUSH_URL =
            "https://uniconnect-backend-0l2w.onrender.com/api/sync/push";

    private static final int OUTCOME_OK = 0;
    private static final int OUTCOME_CONFLICT = 1;
    private static final int OUTCOME_AUTH = 2;
    private static final int OUTCOME_RETRY = 3;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        String accountId = getInputData().getString(SyncScheduler.KEY_ACCOUNT);
        if (accountId == null) {
            return Result.success();
        }

        // No login token: pause quietly (do not fail, do not retry)
        String token = UserManager.getInstance(context).getAuthToken();
        if (token == null || token.isEmpty()) {
            return Result.success();
        }

        PendingOperationDao dao = AppDatabase.getDatabase(context).pendingOperationDao();

        // Already in the order they were saved (ORDER BY rowid)
        List<PendingOperation> operations = dao.getPendingOperationsForAccount(accountId);

        // Students whose queue is blocked by a conflict
        Set<String> blockedStudents = new HashSet<>();

        for (PendingOperation op : operations) {
            String status = op.getSyncStatus();
            String id = op.getOperationId();

            if (SyncState.SYNCED.name().equals(status)) {
                continue;
            }
            if (SyncState.ACTION_REQUIRED.name().equals(status)) {
                blockedStudents.add(op.getStudentId());
                continue;
            }
            if (blockedStudents.contains(op.getStudentId())) {
                continue;
            }

            dao.updateSyncStatus(id, SyncState.SYNCING.name());

            int outcome;
            try {
                // SAME operationId on every retry
                outcome = push(op, token);
            } catch (IOException | JSONException e) {
                dao.updateSyncStatus(id, SyncState.PENDING.name());
                return Result.retry();
            }

            if (outcome == OUTCOME_OK) {
                dao.updateSyncStatus(id, SyncState.SYNCED.name());
            } else if (outcome == OUTCOME_CONFLICT) {
                dao.updateSyncStatus(id, SyncState.ACTION_REQUIRED.name());
                blockedStudents.add(op.getStudentId());
            } else if (outcome == OUTCOME_AUTH) {
                dao.updateSyncStatus(id, SyncState.PENDING.name());
                return Result.success(); // session problem: pause, do not retry
            } else {
                dao.updateSyncStatus(id, SyncState.PENDING.name());
                return Result.retry(); // stop here so order is kept
            }
        }

        return Result.success();
    }

    private int push(PendingOperation op, String token) throws IOException, JSONException {
        JSONObject mutation = new JSONObject();
        mutation.put("mutationId", op.getOperationId());
        mutation.put("entity", "STUDENT");
        mutation.put("entityId", op.getStudentId());
        mutation.put("action", op.getOperationType());
        mutation.put("clientVersion", op.getBaseVersion());
        try {
            mutation.put("payload", new JSONObject(op.getPayload()));
        } catch (JSONException | NullPointerException e) {
            mutation.put("payload", op.getPayload());
        }

        JSONArray list = new JSONArray();
        list.put(mutation);
        JSONObject body = new JSONObject();
        body.put("mutations", list);

        HttpURLConnection conn = (HttpURLConnection) new URL(PUSH_URL).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000); // free servers can be slow to wake up
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String text = readAll(code >= 400 ? conn.getErrorStream() : conn.getInputStream());

            if (code == 200 || code == 201) {
                JSONObject root = new JSONObject(text);
                JSONObject data = root.optJSONObject("data");
                if (data == null) {
                    return OUTCOME_RETRY;
                }
                if (containsId(data.optJSONArray("applied"), op.getOperationId())) {
                    return OUTCOME_OK;
                }
                // listed as a conflict, or ignored by the server: needs a human
                return OUTCOME_CONFLICT;
            }

            if (code == 409) {
                return OUTCOME_CONFLICT;
            }
            if (code == 401 || code == 403) {
                return OUTCOME_AUTH;
            }
            if (code == 408 || code == 429 || code >= 500) {
                return OUTCOME_RETRY;
            }
            return OUTCOME_CONFLICT; // other 4xx: needs a human, do not retry forever
        } finally {
            conn.disconnect();
        }
    }

    private static boolean containsId(JSONArray array, String id) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length(); i++) {
            Object item = array.opt(i);
            if (item instanceof JSONObject) {
                if (id.equals(((JSONObject) item).optString("mutationId"))) {
                    return true;
                }
            } else if (id.equals(String.valueOf(item))) {
                return true;
            }
        }
        return false;
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
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