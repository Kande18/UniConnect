package com.uniconnect.app;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_operations")
public class PendingOperation {

    @PrimaryKey
    @NonNull
    private String operationId;

    private String studentId;
    private String accountId;
    private String operationType;
    private String payload;
    private int baseVersion;
    private String syncStatus;

    public PendingOperation(@NonNull String operationId, String studentId, String accountId,
                            String operationType, String payload, int baseVersion, String syncStatus) {
        this.operationId = operationId;
        this.studentId = studentId;
        this.accountId = accountId;
        this.operationType = operationType;
        this.payload = payload;
        this.baseVersion = baseVersion;
        this.syncStatus = syncStatus;
    }

    @NonNull
    public String getOperationId() { return operationId; }
    public void setOperationId(@NonNull String operationId) { this.operationId = operationId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public int getBaseVersion() { return baseVersion; }
    public void setBaseVersion(int baseVersion) { this.baseVersion = baseVersion; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}