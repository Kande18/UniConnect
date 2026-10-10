package com.uniconnect.app;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    public static final String KEY_ACCOUNT = "account_id";

    // Call this after any change is saved, and after login
    public static void schedule(Context context, String accountId) {

        // Rule 1: only run when the phone has internet
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // The job card: run SyncWorker, follow the rule, retry if it fails
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(new Data.Builder().putString(KEY_ACCOUNT, accountId).build())
                .build();

        // One sync queue per account, so two workers never run at once
        WorkManager.getInstance(context).enqueueUniqueWork(
                "sync-" + accountId, ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }

    // The "Sync now" button calls this
    public static void syncNow(Context context, String accountId) {
        schedule(context, accountId);
    }
}