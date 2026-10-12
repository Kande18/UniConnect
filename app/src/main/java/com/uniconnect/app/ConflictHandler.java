package com.uniconnect.app;

import androidx.room.RoomDatabase;

import java.util.UUID;

public class ConflictHandler {
    private final RoomDatabase db;
    private final ConflictDao conflictDao;
    private final PendingOperationDao opDao;

    public ConflictHandler(RoomDatabase db, ConflictDao conflictDao, PendingOperationDao opDao) {
        this.db = db; this.conflictDao = conflictDao; this.opDao = opDao;
    }

    /** Server said 409: keep the local proposal, store the server copy, flag the op. */
    public void onConflict(PendingOperation op, String serverRecordJson, int serverVersion) {
        db.runInTransaction(() -> {
            conflictDao.insert(new ConflictRecord(op.getOperationId(), op.getAccountId(),
                    op.getStudentId(), op.getPayload(), serverRecordJson, serverVersion));
            opDao.updateSyncStatus(op.getOperationId(), SyncState.ACTION_REQUIRED.name());
        });
    }

    /** User chose "Keep mine": re-queue the same edit on top of the server version. */
    public void keepMine(ConflictRecord c, PendingOperation oldOp) {
        db.runInTransaction(() -> {
            opDao.insertPendingOperation(new PendingOperation(
                    UUID.randomUUID().toString(), c.studentId, c.accountId,
                    oldOp.getOperationType(), c.localPayloadJson,
                    c.serverVersion, SyncState.PENDING.name()));
            opDao.deleteOperation(oldOp.getOperationId());
            conflictDao.delete(c.operationId, c.accountId);
        });
    }

    /** User chose "Use server's": drop the local edit. */
    public void useServer(ConflictRecord c) {
        db.runInTransaction(() -> {
            opDao.deleteOperation(c.operationId);
            conflictDao.delete(c.operationId, c.accountId);
            // TODO: write c.serverRecordJson into the local student row once that table exists
        });
    }
}