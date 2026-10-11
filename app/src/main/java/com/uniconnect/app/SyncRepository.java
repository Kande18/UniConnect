package com.uniconnect.app;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncRepository {

    private final AppDatabase db;
    private final PendingOperationDao pendingOperationDao;
    private final ExecutorService executorService;

    public SyncRepository(Context context) {
        db = AppDatabase.getDatabase(context);
        pendingOperationDao = db.pendingOperationDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Executes both the edit operation and pending operation logging inside a single Room transaction.
     */
    public void saveEditWithPendingOperation(PendingOperation operation, Runnable localEditAction) {
        executorService.execute(() -> {
            db.runInTransaction(() -> {
                // Execute the local UI / entity update
                if (localEditAction != null) {
                    localEditAction.run();
                }
                // Save/insert the sync tracking operation in the same transaction
                pendingOperationDao.insertPendingOperation(operation);
            });
        });
    }

    public void updateSyncStatus(String operationId, String status) {
        executorService.execute(() -> pendingOperationDao.updateSyncStatus(operationId, status));
    }
}