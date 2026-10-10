# UniConnect — Part 5: Local Storage & Synchronization Handover Document

> **Target Audience:** Local Storage and Synchronization Team (Janet Chirwa, Victor, et al.)  
> **Prepared by:** Backend and Database / Android Architecture Team  
> **Status:** Live Backend Endpoints Tested (100% Pass Rate), Dynamic UI Ready for Observation  
> **Repository Branch:** `main` / `Backend-and-databases`

---

## 1. Executive Summary & Code Review of Commit `5ec331e`

Janet Chirwa recently committed the initial WorkManager setup (`5ec331e`):
* `app/build.gradle`: Added `androidx.work:work-runtime:2.9.1`.
* `SyncScheduler.java`: Basic scheduler with `OneTimeWorkRequest`, `NetworkType.CONNECTED` constraint, exponential backoff (10s), and `ExistingWorkPolicy.APPEND_OR_REPLACE`.
* `SyncWorker.java`: Placeholder worker returning `Result.success()`.

### 1.1 What Janet Implemented Well:
1. **WorkManager Runtime Dependency:** Correctly integrated AndroidX WorkManager `2.9.1`.
2. **Network Constraint:** Accurately enforced `NetworkType.CONNECTED`, preventing battery-draining sync attempts while offline.
3. **Backoff Policy:** Correctly set `BackoffPolicy.EXPONENTIAL` (10 seconds) so retries back off gracefully during server downtime.
4. **Unique Work Naming:** Used `enqueueUniqueWork("sync-" + accountId, ...)` to prevent duplicate jobs running concurrently for the same user.

### 1.2 Required Adjustments & Gaps Against Lecturer's Requirements:
| Item | Janet's Current Code | Lecturer Requirement / Recommended Fix | Rationale |
| :--- | :--- | :--- | :--- |
| **Work Policy** | `ExistingWorkPolicy.APPEND_OR_REPLACE` | Use `ExistingWorkPolicy.KEEP` for auto-sync, and `ExistingWorkPolicy.REPLACE` for `syncNow()` | `APPEND_OR_REPLACE` can queue endless worker chains and is deprecated in modern WorkManager. |
| **Periodic Background Sync** | Only `OneTimeWorkRequest` | Add `PeriodicWorkRequest` (e.g., every 15–30 min) in `SyncScheduler` | Lecturer Instructions (§15, §16, §25) mandate automatic periodic synchronization alongside manual triggers. |
| **Authentication Token** | Passes only `accountId` string | Pass or retrieve the JWT Bearer Token | `/api/sync/pull` and `/api/sync/push` require `Authorization: Bearer <jwt_token>`. Worker must retrieve this from `UserManager` or `SharedPreferences`. |
| **Local Mutation Queue (Victor's part)** | Not yet implemented | Room database table `sync_mutations` must be created | Worker needs a persistent queue to read offline changes and send them to the server. |
| **Worker Execution Logic** | Returns stub `Result.success()` | Implement 2-Phase Sync: (1) Push local mutations $\to$ (2) Pull server deltas $\to$ (3) Save to Room | Real delta synchronization and conflict handling. |

---

## 2. Backend Sync API Specification (The Contract)

The backend provides two dedicated, production-ready synchronization endpoints under `/api/sync`. Both endpoints are secured with JWT authentication.

### 2.1 Endpoint 1: Delta Pull (`GET /api/sync/pull`)
Pulls all student records that were created, updated, or soft-deleted since the last known sync timestamp, along with live laboratory group enrollment tallies.

* **URL:** `GET http://<SERVER_HOST>:5000/api/sync/pull` (or live Render URL)
* **Headers:**
  ```http
  Authorization: Bearer <JWT_TOKEN>
  Accept: application/json
  ```
* **Query Parameters:**
  | Parameter | Type | Required | Description |
  | :--- | :--- | :--- | :--- |
  | `since` | String (ISO-8601) | Optional | e.g. `2026-10-10T08:30:00.000Z`. If omitted or null, returns **all** active records (used for initial full sync). |

* **Successful Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "serverTimestamp": "2026-10-10T12:00:00.000Z",
      "updatedStudents": [
        {
          "studentNumber": "202400101",
          "name": "Alice Banda",
          "programme": "CS",
          "labGroup": "G01",
          "version": 2,
          "updatedAt": "2026-10-10T11:45:00.000Z"
        },
        {
          "studentNumber": "202400102",
          "name": "Bob Phiri",
          "programme": "IT",
          "labGroup": "Unassigned",
          "version": 1,
          "updatedAt": "2026-10-10T09:15:00.000Z"
        }
      ],
      "deletedStudentNumbers": [
        "202400199"
      ],
      "groupCounts": {
        "G01": 14,
        "G02": 10,
        "G03": 9,
        "G04": 15
      }
    }
  }
  ```

* **Client Processing Rules:**
  1. Insert or update all items in `updatedStudents` into the local Room `students` table. Reset their `isSyncPending = false` and `hasConflict = false`.
  2. For student numbers in `deletedStudentNumbers`, either remove them from the Room database or mark them `isDeleted = true` so they don't appear in active rosters.
  3. Update local group capacity tallies using `groupCounts`.
  4. Store `serverTimestamp` in Android `SharedPreferences` as `KEY_LAST_SYNC_TIMESTAMP`. Pass this value into the `?since=` query parameter on the next run.

---

### 2.2 Endpoint 2: Mutation Push (`POST /api/sync/push`)
Processes a batch of offline client changes (profile edits, lab group reassignments, deletions) using **Server Authority Optimistic Concurrency Control**.

* **URL:** `POST http://<SERVER_HOST>:5000/api/sync/push`
* **Headers:**
  ```http
  Authorization: Bearer <JWT_TOKEN>
  Content-Type: application/json
  Accept: application/json
  ```
* **Request Body:**
  ```json
  {
    "mutations": [
      {
        "mutationId": "mut-550e8400-e29b-41d4-a716-446655440000",
        "entity": "STUDENT",
        "entityId": "202400101",
        "action": "UPDATE",
        "clientVersion": 1,
        "payload": {
          "name": "Alice Banda",
          "programme": "CS",
          "labGroup": "G02"
        }
      },
      {
        "mutationId": "mut-660f9511-f30c-52e5-b827-557766551111",
        "entity": "STUDENT",
        "entityId": "202400199",
        "action": "DELETE",
        "clientVersion": 2
      }
    ]
  }
  ```

* **Successful Response (`200 OK`):**
  ```json
  {
    "success": true,
    "data": {
      "applied": [
        "mut-550e8400-e29b-41d4-a716-446655440000"
      ],
      "conflicts": [
        {
          "mutationId": "mut-660f9511-f30c-52e5-b827-557766551111",
          "entityId": "202400199",
          "reason": "SYNC_CONFLICT",
          "serverVersion": 3,
          "clientVersion": 2,
          "serverState": {
            "name": "Charlie Tembo",
            "programme": "IT",
            "labGroup": "G01"
          }
        }
      ]
    }
  }
  ```

* **Conflict Resolution Rules (§17 & §25 of Instructions):**
  * **Server Authority Rule:** The server checks `server.version > clientVersion`. If a lecturer or another device modified the record while this device was offline, the mutation is **rejected** and returned in `conflicts`.
  * **Room Handling:**
    * For `applied` IDs: Delete the mutation record from the local `sync_mutations` table. Mark the local entity as synced (`isSyncPending = false`).
    * For `conflicts` IDs: Do NOT overwrite the server. Mark local entity with `hasConflict = true`. The UI will surface the conflict badge: *"Your saved change vs Current server value [Review]"*.

---

## 3. Local Storage Architecture (For Victor)

To establish Room as the **Single Source of Truth (SSOT)**, Victor should implement the following three entities and DAOs.

### 3.1 Entity 1: `StudentEntity.java`
```java
package com.uniconnect.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "students")
public class StudentEntity {
    @PrimaryKey
    @NonNull
    public String studentNumber; // e.g. "202400101"

    public String fullName;
    public String programmeCode; // "CS", "IT", "DS"
    public String labGroupId;    // "G01", "G02", etc., or null
    public int version;          // Optimistic locking version
    public boolean isSyncPending;// True if edited offline
    public boolean isDeleted;    // Soft-delete flag
    public boolean hasConflict;  // True if server rejected push
    public long lastUpdated;     // Epoch millisecond timestamp
}
```

### 3.2 Entity 2: `SyncMutationEntity.java` (The Offline Outbox Queue)
```java
package com.uniconnect.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_mutations")
public class SyncMutationEntity {
    @PrimaryKey
    @NonNull
    public String mutationId;    // UUID.randomUUID().toString()

    public String entity;        // "STUDENT"
    public String entityId;      // studentNumber
    public String action;        // "UPDATE" or "DELETE"
    public int clientVersion;    // The version number when user edited
    public String payloadJson;   // JSON string: {"name":"...","programme":"...","labGroup":"..."}
    public long createdAt;       // System.currentTimeMillis()
    public String status;        // "PENDING", "FAILED"
}
```

### 3.3 Entity 3: `LabGroupEntity.java`
```java
package com.uniconnect.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lab_groups")
public class LabGroupEntity {
    @PrimaryKey
    @NonNull
    public String groupId;       // "G01", "G02", "G03", "G04"
    public String groupName;     // "Laboratory Group 1"
    public int capacity;         // 15
    public int enrolledCount;    // Current student tally
}
```

### 3.4 Room DAOs: `StudentDao.java` & `SyncMutationDao.java`
```java
package com.uniconnect.app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface StudentDao {
    @Query("SELECT * FROM students WHERE isDeleted = 0 ORDER BY fullName ASC")
    LiveData<List<StudentEntity>> getAllActiveStudents();

    @Query("SELECT * FROM students WHERE studentNumber = :number LIMIT 1")
    StudentEntity getStudentDirect(String number);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(StudentEntity student);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<StudentEntity> students);

    @Query("UPDATE students SET isDeleted = 1 WHERE studentNumber = :number")
    void markDeleted(String number);
}
```

```java
@Dao
public interface SyncMutationDao {
    @Query("SELECT * FROM sync_mutations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    List<SyncMutationEntity> getPendingMutations();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void enqueueMutation(SyncMutationEntity mutation);

    @Query("DELETE FROM sync_mutations WHERE mutationId IN (:mutationIds)")
    void deleteAppliedMutations(List<String> mutationIds);

    @Query("UPDATE sync_mutations SET status = 'FAILED' WHERE mutationId = :mutationId")
    void markFailed(String mutationId);
}
```

---

## 4. WorkManager Execution Sequence (For Janet)

Here is the exact implementation structure for `SyncWorker.java` to fulfill the 2-Way Sync contract:

```java
package com.uniconnect.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.uniconnect.app.data.local.AppDatabase;
import com.uniconnect.app.data.local.StudentEntity;
import com.uniconnect.app.data.local.SyncMutationEntity;

import java.util.List;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String token = UserManager.getInstance(context).getAuthToken();
        if (token == null || token.isEmpty()) {
            // Cannot sync without valid JWT session
            return Result.failure();
        }

        AppDatabase db = AppDatabase.getInstance(context);

        try {
            // ==========================================
            // PHASE 1: PUSH OFFLINE MUTATIONS FIRST
            // ==========================================
            List<SyncMutationEntity> pendingMutations = db.syncMutationDao().getPendingMutations();
            if (!pendingMutations.isEmpty()) {
                // 1. Build POST /api/sync/push payload
                // 2. Execute Retrofit/OkHttp call with Bearer token
                // 3. For successfully applied mutations:
                //    db.syncMutationDao().deleteAppliedMutations(appliedIds);
                // 4. For conflicts:
                //    flag student record with hasConflict = true;
            }

            // ==========================================
            // PHASE 2: PULL SERVER DELTAS SECOND
            // ==========================================
            SharedPreferences prefs = context.getSharedPreferences("uniconnect_prefs", Context.MODE_PRIVATE);
            String lastSync = prefs.getString("last_sync_timestamp", null);

            // 1. Call GET /api/sync/pull?since={lastSync}
            // 2. Parse updatedStudents, deletedStudentNumbers, groupCounts, serverTimestamp
            // 3. In a db transaction:
            //    - db.studentDao().upsertAll(mappedUpdatedStudents);
            //    - Mark deletedStudentNumbers in Room
            //    - Update group occupancy cards
            // 4. Save new serverTimestamp to SharedPreferences:
            //    prefs.edit().putString("last_sync_timestamp", newServerTimestamp).apply();

            // ==========================================
            // PHASE 3: UPDATE UI STATUS
            // ==========================================
            // Broadcast or update LiveData: SyncStatus = SYNCED ("✓ Synced")

            return Result.success();
        } catch (Exception e) {
            // If network fails midway, WorkManager will retry with exponential backoff
            return Result.retry();
        }
    }
}
```

### 4.1 Recommended Update to `SyncScheduler.java`:
```java
package com.uniconnect.app;

import android.content.Context;
import androidx.work.*;
import java.util.concurrent.TimeUnit;

public class SyncScheduler {

    public static final String KEY_ACCOUNT = "account_id";

    // 1. Immediate One-Time Sync (e.g. "Sync Now" button, post-login, or after saving an edit)
    public static void syncNow(Context context, String accountId) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(new Data.Builder().putString(KEY_ACCOUNT, accountId).build())
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "sync-now-" + accountId,
                ExistingWorkPolicy.REPLACE, // REPLACE ensures immediate execution
                request
        );
    }

    // 2. Periodic Background Sync (runs every 15 minutes when connected to internet)
    public static void schedulePeriodicSync(Context context, String accountId) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .setInputData(new Data.Builder().putString(KEY_ACCOUNT, accountId).build())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync-periodic-" + accountId,
                ExistingWorkPolicy.KEEP, // KEEP prevents resetting interval if already scheduled
                periodicRequest
        );
    }
}
```

---

## 5. UI Integration Map for Sync States

The UI components built in Phases 1–5 already provide user-facing status indicators. Here is how the sync team connects to them:

| UI Status Indicator | Visual Element | Accessible State | Trigger Condition |
| :--- | :--- | :--- | :--- |
| **Offline Mode** | Top status banner / snackbar | `"Offline. Changes saved locally."` | Device disconnected (`NetworkType.NOT_REQUIRED`) |
| **Pending Sync** | Status chip / icon (`⏳ Pending`) | `"Pending sync"` | Mutation queued in `sync_mutations` table |
| **Syncing** | Progress spinner / (`Syncing...`) | `"Syncing in progress"` | `SyncWorker` is actively executing `doWork()` |
| **Synced** | Green badge (`✓ Synced`) | `"Synchronized with server"` | Pull and Push finished with 0 pending mutations |
| **Conflict** | Amber warning (`Action required`) | `"Change conflict. Server value differs."` | Server returned `SYNC_CONFLICT` during push |

---

## 6. Actionable Implementation Checklist for Janet & Victor

### For Victor (Local Room Storage):
- [ ] Add Room compiler and runtime to `app/build.gradle` if not already present.
- [ ] Create `StudentEntity.java`, `LabGroupEntity.java`, and `SyncMutationEntity.java` in `com.uniconnect.app.data.local`.
- [ ] Create `StudentDao.java` and `SyncMutationDao.java`.
- [ ] Create `AppDatabase.java` singleton with Room database builder.
- [ ] Implement offline repository method `saveStudentOffline(...)`: writes student to Room with `isSyncPending = true` and enqueues record into `sync_mutations`.

### For Janet (WorkManager & Networking):
- [ ] Update `SyncScheduler.java` to support both `syncNow()` (`ExistingWorkPolicy.REPLACE`) and `schedulePeriodicSync()` (`PeriodicWorkRequest`, 15 min).
- [ ] Update `SyncWorker.java` to retrieve JWT from `UserManager.getInstance(context)`.
- [ ] Implement HTTP client calls in `SyncWorker.doWork()` for `POST /api/sync/push` and `GET /api/sync/pull`.
- [ ] Test offline behavior:
  1. Turn off WiFi/Mobile Data on emulator/phone.
  2. Make an edit or lab transfer in the app. Confirm it saves instantly to Room.
  3. Turn WiFi back on.
  4. Confirm WorkManager wakes up, pushes mutations, pulls deltas, and updates the status to **✓ Synced**.
