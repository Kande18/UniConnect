package com.uniconnect.app;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    public static final String KEY_ACCOUNT = "account_id";

    // Rule: only run when the phone has internet
    private static Constraints connectedOnly() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }

    private static Data accountData(String accountId) {
        return new Data.Builder().putString(KEY_ACCOUNT, accountId).build();
    }

    // Background sync: call after a change is saved, and after login.
    // KEEP = if a sync is already waiting or running, leave it alone.
    public static void schedule(Context context, String accountId) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(connectedOnly())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(accountData(accountId))
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "sync-" + accountId, ExistingWorkPolicy.KEEP, request);
    }

    // The "Sync now" button calls this.
    // REPLACE = start a fresh sync right away.
    public static void syncNow(Context context, String accountId) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(connectedOnly())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(accountData(accountId))
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "sync-" + accountId, ExistingWorkPolicy.REPLACE, request);
    }

    // Automatic sync every 15 minutes (15 minutes is the shortest Android allows)
    public static void
    schedulePeriodicSync(Context context, String accountId) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(connectedOnly())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(accountData(accountId))
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync-periodic-" + accountId, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}