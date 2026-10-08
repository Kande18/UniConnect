# UniConnect — Part 4: Android Integration & Architecture Guide

> **Target Audience:** Android Architecture Team, Local Storage & Synchronization Team  
> **Pattern:** MVVM (Model-View-ViewModel) with Single Source of Truth (SSOT) via Room Database  
> **UI Status:** Native Java/XML Frontend (Phases 1–5 complete, compiling cleanly)

---

## 1. Clean Architecture Stack

```
[ Android UI Layer ]
  Activities (StudentProfileActivity, LecturerDashboardActivity, etc.)
  Fragments (StudentProfileFragment, LecturerRosterFragment, etc.)
       ▲
       │ Observes LiveData<UiState<T>>
       ▼
[ Presentation / ViewModel Layer ]
  StudentViewModel, LecturerViewModel, AuthViewModel
       ▲
       │ Exposes Clean Domain Objects
       ▼
[ Repository Layer (NetworkBoundResource) ]
  StudentRepository, LecturerRepository, SyncRepository
       ▲                              ▲
       │ Local Cache (SSOT)           │ Remote HTTP
       ▼                              ▼
[ Room Database (SQLite) ]    [ Retrofit 2 API Service ]
  StudentDao, GroupDao, SyncDao    UniConnectApiService
```

---

## 2. Recommended Android Dependencies (`app/build.gradle`)

Add these production-standard libraries to `app/build.gradle`:

```groovy
dependencies {
    // AndroidX & Material (Already included)
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Lifecycle, ViewModel & LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.4'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.4'

    // Room Database (Offline Cache)
    def room_version = "2.6.1"
    implementation "androidx.room:room-runtime:$room_version"
    annotationProcessor "androidx.room:room-compiler:$room_version"

    // Networking (Retrofit 2 + OkHttp 3 + Gson)
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // Background Synchronization
    implementation 'androidx.work:work-runtime:2.9.1'
}
```

---

## 3. Retrofit API Service Interface (`UniConnectApiService.java`)

```java
package com.uniconnect.app.data.remote;

import retrofit2.Call;
import retrofit2.http.*;

public interface UniConnectApiService {

    // --- Authentication ---
    @POST("auth/register")
    Call<ApiResponse<StudentDto>> registerStudent(@Body RegisterRequest request);

    @POST("auth/student-login")
    Call<ApiResponse<AuthResponse>> studentLogin(@Body LoginRequest request);

    @POST("auth/lecturer-login")
    Call<ApiResponse<AuthResponse>> lecturerLogin(@Body LecturerLoginRequest request);

    // --- Student Endpoints ---
    @GET("students/profile")
    Call<ApiResponse<StudentProfileDto>> getStudentProfile();

    @PUT("students/profile")
    Call<ApiResponse<StudentProfileDto>> updateStudentProfile(@Body UpdateProfileRequest request);

    @GET("students/my-group")
    Call<ApiResponse<GroupDetailDto>> getMyGroup();

    @POST("requests/change-group")
    Call<ApiResponse<ChangeRequestDto>> submitChangeRequest(@Body ChangeGroupRequest request);

    // --- Lecturer Endpoints ---
    @GET("lecturer/dashboard")
    Call<ApiResponse<DashboardSummaryDto>> getDashboardSummary();

    @GET("lecturer/roster")
    Call<ApiResponse<RosterResponseDto>> getStudentRoster(
        @Query("query") String query,
        @Query("programme") String programme,
        @Query("group") String group
    );

    @POST("lecturer/students")
    Call<ApiResponse<StudentDto>> createStudent(@Body CreateStudentRequest request);

    @PUT("lecturer/students/{studentNumber}")
    Call<ApiResponse<StudentDto>> updateStudent(
        @Path("studentNumber") String studentNumber,
        @Body UpdateStudentRequest request
    );

    @DELETE("lecturer/students/{studentNumber}")
    Call<ApiResponse<Void>> deleteStudent(@Path("studentNumber") String studentNumber);

    @GET("lecturer/requests")
    Call<ApiResponse<RequestsResponseDto>> getPendingRequests();

    @POST("lecturer/requests/{id}/approve")
    Call<ApiResponse<Void>> approveRequest(@Path("id") String requestId);

    @POST("lecturer/requests/{id}/decline")
    Call<ApiResponse<Void>> declineRequest(@Path("id") String requestId);

    // --- Delta Synchronization ---
    @GET("sync/pull")
    Call<ApiResponse<SyncDeltaDto>> pullDeltas(@Query("since") String isoTimestamp);

    @POST("sync/push")
    Call<ApiResponse<SyncPushResultDto>> pushMutations(@Body SyncPushRequest request);
}
```

---

## 4. Room Database Entities & DAO

### 4.1 Local Student Entity (`StudentEntity.java`)
```java
package com.uniconnect.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "students")
public class StudentEntity {
    @PrimaryKey
    @NonNull
    public String studentNumber; // e.g. "202400123"

    public String fullName;
    public String programmeCode;
    public String labGroupId;    // nullable ("G01" or null for unassigned)
    public int version;
    public boolean isSyncPending;
    public boolean isDeleted;
    public long lastUpdated;
}
```

### 4.2 Student DAO (`StudentDao.java`)
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
    LiveData<StudentEntity> getStudentByNumber(String number);

    @Query("SELECT * FROM students WHERE (fullName LIKE '%' || :query || '%' OR studentNumber LIKE '%' || :query || '%') AND isDeleted = 0")
    LiveData<List<StudentEntity>> searchStudents(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(List<StudentEntity> students);

    @Query("UPDATE students SET isDeleted = 1, isSyncPending = 1 WHERE studentNumber = :number")
    void markStudentDeleted(String number);
}
```

---

## 5. UI Integration Map (Replacing Mock Data)

The Android UI built in Phases 1–5 already exposes the exact View IDs needed for ViewModel observation. Below is the mapping for each screen:

| Screen / Fragment | View ID in Layout | Data Contract Field | ViewModel / Repository LiveData |
| :--- | :--- | :--- | :--- |
| **Student Profile** | `tvStudentName`, `tvStudentNumber` | `name`, `studentNumber` | `studentViewModel.getProfile().observe(...)` |
| **Student Profile** | `chipProgrammeBadge` | `programme` (CS, IT, DS) | `profile.programmeCode` |
| **Student Profile** | `tvGroupBadge`, `progressOccupancy` | `labGroup`, `occupancy` | `profile.labGroupId`, `profile.occupancy` |
| **Student Group** | `tvGroupHeroName`, `tvGroupOccupancy` | `cohort`, `enrolled / max` | `studentViewModel.getGroupDetails().observe(...)` |
| **Group Change** | `radioGroupG02`, `btnSubmitRequest` | `targetGroupId`, `reason` | `studentViewModel.submitTransferRequest(...)` |
| **Lecturer Dashboard**| `tvTotalCount`, `tvUnassignedCount` | `metrics.total`, `unassigned`| `lecturerViewModel.getDashboardSummary().observe(...)` |
| **Lecturer Dashboard**| `cardG01`, `cardG02`, `cardG03`, `cardG04` | `groupSummaries` | Binds count and capacity into included cards |
| **Lecturer Roster** | `recyclerStudents`, `etFragSearch` | `studentList` | `lecturerViewModel.getFilteredStudents().observe(...)` |
| **Lecturer Requests**| `cardRequest1`, `btnApprove1`, `btnDecline1` | `requests` list | `lecturerViewModel.approveRequest(id)` |
| **Student Editor** | `etEditorName`, `etEditorStudentNumber` | Form fields | `lecturerViewModel.saveStudent(dto)` |

---

## 6. Integration Checklist for Architecture Team

- [ ] Add Retrofit, OkHttp, Room, and Lifecycle dependencies to `app/build.gradle`.
- [ ] Initialize `RetrofitClient` singleton with `AuthInterceptor` storing JWT in `EncryptedSharedPreferences`.
- [ ] Create `AppDatabase` Room instance extending `RoomDatabase`.
- [ ] Connect `StudentProfileFragment` to `StudentViewModel` to replace mock strings.
- [ ] Connect `LecturerRosterFragment` to `LecturerViewModel` and bind `StudentRosterAdapter.submitList()`.
- [ ] Implement `WorkManager` `SyncWorker` to periodically call `/api/sync/pull` and flush locally queued offline edits.
