package com.uniconnect.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public abstract class PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertPendingOperation(PendingOperation operation);

    @Query("SELECT * FROM pending_operations WHERE accountId = :accountId ORDER BY rowid ASC")
    public abstract List<PendingOperation> getPendingOperationsForAccount(String accountId);

    @Query("UPDATE pending_operations SET syncStatus = :status WHERE operationId = :operationId")
    public abstract void updateSyncStatus(String operationId, String status);

    @Query("DELETE FROM pending_operations WHERE operationId = :operationId")
    public abstract void deleteOperation(String operationId);

    @Transaction
    public void saveEditWithTransaction(PendingOperation operation) {
        insertPendingOperation(operation);
    }
}