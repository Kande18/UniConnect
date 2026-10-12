package com.uniconnect.app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ConflictDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ConflictRecord record);

    @Query("SELECT * FROM conflicts WHERE accountId = :accountId ORDER BY detectedAt DESC")
    LiveData<List<ConflictRecord>> observeForAccount(String accountId);

    @Query("SELECT * FROM conflicts WHERE operationId = :operationId AND accountId = :accountId")
    ConflictRecord getByOperationId(String operationId, String accountId);

    @Query("DELETE FROM conflicts WHERE operationId = :operationId AND accountId = :accountId")
    void delete(String operationId, String accountId);

    @Query("DELETE FROM conflicts WHERE accountId = :accountId")
    void deleteAllForAccount(String accountId);
}