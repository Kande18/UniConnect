package com.uniconnect.app;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
@Entity(tableName = "conflicts",
        indices = {@Index("accountId")})
public class ConflictRecord {
    @PrimaryKey
    @NonNull
    public String operationId = "";
    public String accountId;
    public String studentId;
    public String localPayloadJson;   // your proposal, never overwritten
    public String serverRecordJson;   // current server version
    public int serverVersion;
    public long detectedAt;

    public ConflictRecord() {}

    @Ignore
    public ConflictRecord(@NonNull String operationId, String accountId, String studentId,
                          String localPayloadJson, String serverRecordJson, int serverVersion) {
        this.operationId = operationId;
        this.accountId = accountId;
        this.studentId = studentId;
        this.localPayloadJson = localPayloadJson;
        this.serverRecordJson = serverRecordJson;
        this.serverVersion = serverVersion;
        this.detectedAt = System.currentTimeMillis();
    }
}