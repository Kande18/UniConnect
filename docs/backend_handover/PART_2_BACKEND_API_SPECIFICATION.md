# UniConnect — Part 2: Backend REST API Specification

> **Document Version:** 1.0.0  
> **Protocol:** HTTP/1.1 or HTTP/2 over TLS (HTTPS)  
> **Standard Format:** JSON (`application/json; charset=utf-8`)  
> **Default Port:** `5000` (configurable via `PORT` environment variable)

---

## 1. Global API Conventions

### 1.1 Base URL
```
Development: http://localhost:5000/api
Android Emulator Host: http://10.0.2.2:5000/api
Production: https://api.uniconnect.edu.zm/api
```

### 1.2 Authentication Header
All protected routes require an HTTP `Authorization` header containing the JSON Web Token (JWT):
```http
Authorization: Bearer <jwt_access_token>
```

### 1.3 Uniform Response Formats

#### Success Response
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully."
}
```

#### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Student number must contain exactly 9 numeric digits.",
    "field": "studentNumber",
    "details": []
  }
}
```

### 1.4 Standard Error Codes
| HTTP Status | Error Code | Description |
| :--- | :--- | :--- |
| `400 Bad Request` | `VALIDATION_ERROR` | Required fields missing or malformed input. |
| `400 Bad Request` | `INVALID_CLAIM_CODE` | Provided claim code does not exist or has expired. |
| `401 Unauthorized` | `UNAUTHORIZED` | Token missing, invalid, or expired. |
| `403 Forbidden` | `FORBIDDEN` | Insufficient permissions (e.g. Student trying to call Lecturer route). |
| `404 Not Found` | `NOT_FOUND` | Student, lab group, or request record does not exist. |
| `409 Conflict` | `DUPLICATE_STUDENT_NUMBER` | Student number is already registered in the university system. |
| `409 Conflict` | `GROUP_FULL` | Target laboratory group has reached its 15-student capacity. |
| `409 Conflict` | `SYNC_CONFLICT` | Server record has been updated since client's last cached timestamp. |
| `500 Internal Error`| `SERVER_ERROR` | Unexpected database or server exception. |

---

## 2. Authentication & Authorization Endpoints

### 2.1 Student Registration
Registers a new student, validates their department claim code, and hashes their password.

- **Route:** `POST /api/auth/register`
- **Access:** Public

#### Request Body
```json
{
  "studentNumber": "202400123",
  "name": "Peculiar Banda",
  "programme": "CS",
  "claimCode": "CLAIM-CS-2024",
  "password": "Password123!"
}
```

#### Validation Rules
- `studentNumber`: Required, exactly 9 digits `^\d{9}$`.
- `name`: Required, 2 to 100 characters.
- `programme`: Required, one of `["CS", "IT", "DS"]`.
- `claimCode`: Required, must exist in `claim_codes` table and `is_used = FALSE`.
- `password`: Required, minimum 6 characters.

#### Response (`201 Created`)
```json
{
  "success": true,
  "data": {
    "studentNumber": "202400123",
    "name": "Peculiar Banda",
    "programme": "CS",
    "assignedGroup": null,
    "status": "REGISTERED",
    "createdAt": "2026-10-08T14:30:00.000Z"
  },
  "message": "Student registration completed. You can now sign in."
}
```

---

### 2.2 Student Sign In
Authenticates a student using their 9-digit student number and password.

- **Route:** `POST /api/auth/student-login`
- **Access:** Public

#### Request Body
```json
{
  "studentNumber": "202400123",
  "password": "Password123!"
}
```

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "user": {
      "role": "STUDENT",
      "studentNumber": "202400123",
      "name": "Peculiar Banda",
      "programme": "CS",
      "labGroup": "G01"
    }
  }
}
```

---

### 2.3 Lecturer Sign In
Authenticates academic staff using their staff identifier or university email.

- **Route:** `POST /api/auth/lecturer-login`
- **Access:** Public

#### Request Body
```json
{
  "staffId": "STF-2024-88",
  "password": "LecturerPass2026!"
}
```

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "user": {
      "role": "LECTURER",
      "staffId": "STF-2024-88",
      "name": "Dr. A. Banda",
      "department": "Computer Science & IT",
      "title": "Senior Lab Coordinator"
    }
  }
}
```

---

## 3. Student Portal Endpoints (Student Role)

### 3.1 Get Student Profile
Retrieves the logged-in student's academic profile and lab group status.

- **Route:** `GET /api/students/profile`
- **Access:** Protected (`STUDENT`)

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "studentNumber": "202400123",
    "name": "Peculiar Banda",
    "programme": "CS",
    "labGroup": "G01",
    "groupOccupancy": 14,
    "groupCapacity": 15,
    "isGroupConfirmed": true,
    "syncStatus": "SYNCED",
    "version": 3
  }
}
```

---

### 3.2 Update Student Profile
Allows student to update allowed fields (name and programme only; student number is immutable).

- **Route:** `PUT /api/students/profile`
- **Access:** Protected (`STUDENT`)

#### Request Body
```json
{
  "name": "Peculiar C. Banda",
  "programme": "CS"
}
```

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "studentNumber": "202400123",
    "name": "Peculiar C. Banda",
    "programme": "CS",
    "updatedAt": "2026-10-08T14:45:00.000Z",
    "version": 4
  },
  "message": "Profile updated successfully."
}
```

---

### 3.3 Get Student's Lab Group Details
Returns full scheduling, venue, and instructor details for the student's assigned group.

- **Route:** `GET /api/students/my-group`
- **Access:** Protected (`STUDENT`)

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "groupId": "G01",
    "groupName": "Lab Group 01",
    "schedule": "Monday 08:00 – 10:00 AM",
    "venue": "Lab 3B (Systems Lab)",
    "instructor": "Prof. Davis",
    "enrolledCount": 14,
    "maxCapacity": 15,
    "status": "ALMOST_FULL",
    "availableSeats": 1
  }
}
```

---

### 3.4 Submit Group Change Request
Submits a transfer request to change from the current lab group to a target group.

- **Route:** `POST /api/requests/change-group`
- **Access:** Protected (`STUDENT`)

#### Request Body
```json
{
  "targetGroupId": "G02",
  "reason": "Timetable clash with CS302 Advanced Algorithms lecture on Monday mornings."
}
```

#### Response (`201 Created`)
```json
{
  "success": true,
  "data": {
    "requestId": "REQ-2026-1001",
    "studentNumber": "202400123",
    "currentGroupId": "G01",
    "targetGroupId": "G02",
    "reason": "Timetable clash with CS302 Advanced Algorithms lecture on Monday mornings.",
    "status": "PENDING",
    "submittedAt": "2026-10-08T15:00:00.000Z"
  },
  "message": "Your transfer request has been submitted for lecturer review."
}
```

---

## 4. Lecturer Portal Endpoints (Lecturer Role)

### 4.1 Get Lecturer Dashboard Overview
Returns key metrics, overall capacity, and summary counts for G01–G04.

- **Route:** `GET /api/lecturer/dashboard`
- **Access:** Protected (`LECTURER`)

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "metrics": {
      "totalStudents": 46,
      "unassignedCount": 3,
      "fullGroupsCount": 1,
      "pendingRequestsCount": 2,
      "totalCapacity": 60,
      "totalOccupied": 48,
      "occupancyPercentage": 80
    },
    "groupSummaries": [
      { "id": "G01", "enrolled": 14, "capacity": 15, "status": "ALMOST_FULL", "venue": "Lab 3B" },
      { "id": "G02", "enrolled": 10, "capacity": 15, "status": "AVAILABLE", "venue": "Lab 1A" },
      { "id": "G03", "enrolled": 9,  "capacity": 15, "status": "AVAILABLE", "venue": "Lab 2C" },
      { "id": "G04", "enrolled": 15, "capacity": 15, "status": "FULL", "venue": "Lab 3B" }
    ],
    "recentActivity": [
      {
        "id": "ACT-1",
        "title": "Transfer requested by Peculiar Banda",
        "description": "From G01 to G02",
        "timestamp": "2026-10-08T14:45:00.000Z"
      }
    ]
  }
}
```

---

### 4.2 Get Student Roster (with Search, Filter & Pagination)
Returns the list of students matching search query, programme, and group filters.

- **Route:** `GET /api/lecturer/roster`
- **Access:** Protected (`LECTURER`)
- **Query Parameters:**
  - `query` (optional): Student name or 9-digit student number
  - `programme` (optional): `All` | `CS` | `IT` | `DS`
  - `group` (optional): `All` | `G01` | `G02` | `G03` | `G04` | `Unassigned`
  - `page` (default: 1), `limit` (default: 50)

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "totalCount": 46,
    "filteredCount": 8,
    "page": 1,
    "limit": 50,
    "students": [
      {
        "studentNumber": "202400123",
        "name": "Peculiar Banda",
        "programme": "CS",
        "labGroup": "G01",
        "status": "ACTIVE",
        "updatedAt": "2026-10-08T12:00:00.000Z"
      },
      {
        "studentNumber": "202402123",
        "name": "Chidi Okafor",
        "programme": "IT",
        "labGroup": "Unassigned",
        "status": "ACTIVE",
        "updatedAt": "2026-10-08T12:00:00.000Z"
      }
    ]
  }
}
```

---

### 4.3 Create Student (Lecturer Manual Add)
- **Route:** `POST /api/lecturer/students`
- **Access:** Protected (`LECTURER`)

#### Request Body
```json
{
  "studentNumber": "202405555",
  "name": "Kondwani Tembo",
  "programme": "DS",
  "labGroup": "G03"
}
```

---

### 4.4 Update Student Record (Lecturer Student Editor)
- **Route:** `PUT /api/lecturer/students/:studentNumber`
- **Access:** Protected (`LECTURER`)

#### Request Body
```json
{
  "name": "Kondwani Tembo",
  "programme": "DS",
  "labGroup": "G02"
}
```

---

### 4.5 Deactivate / Delete Student (Delete Confirmation §14.8)
Soft-deletes the student record, deactivating their account and freeing their group slot.

- **Route:** `DELETE /api/lecturer/students/:studentNumber`
- **Access:** Protected (`LECTURER`)

#### Response (`200 OK`)
```json
{
  "success": true,
  "message": "Student record 202405555 deactivated and removed from active roster."
}
```

---

### 4.6 Get Pending Transfer Requests
Returns all student group change requests awaiting review.

- **Route:** `GET /api/lecturer/requests`
- **Access:** Protected (`LECTURER`)

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "pendingCount": 2,
    "requests": [
      {
        "id": "REQ-1001",
        "studentNumber": "202400123",
        "studentName": "Peculiar Banda",
        "programme": "CS",
        "currentGroup": "G01",
        "currentGroupCount": 14,
        "targetGroup": "G02",
        "targetGroupCount": 10,
        "maxCapacity": 15,
        "reason": "Timetable conflict with CS302 Advanced Algorithms lecture on Monday mornings.",
        "status": "PENDING",
        "submittedAt": "2026-10-08T09:15:00.000Z"
      }
    ]
  }
}
```

---

### 4.7 Approve or Decline Transfer Request
- **Approve Route:** `POST /api/lecturer/requests/:id/approve`
- **Decline Route:** `POST /api/lecturer/requests/:id/decline`
- **Access:** Protected (`LECTURER`)

#### Approve Behavior:
Atomically performs:
1. Re-verifies target group has `< 15` members. If full, returns `409 Conflict: GROUP_FULL`.
2. Decrements current group count, increments target group count.
3. Updates `students.lab_group_id` to target group.
4. Sets `group_change_requests.status = 'APPROVED'`.

---

## 5. Offline Delta Synchronization Endpoints

### 5.1 Pull Server Deltas (`/api/sync/pull`)
Allows the Android app to download all records modified since a specific timestamp.

- **Route:** `GET /api/sync/pull?since={ISO_TIMESTAMP}`
- **Access:** Protected

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "serverTimestamp": "2026-10-08T16:00:00.000Z",
    "updatedStudents": [ ... ],
    "deletedStudentNumbers": [ "202409999" ],
    "groupCounts": {
      "G01": 14,
      "G02": 10,
      "G03": 9,
      "G04": 15
    }
  }
}
```

---

### 5.2 Push Offline Mutations (`/api/sync/push`)
Allows the Android client to submit a batch of locally queued edits created while offline.

- **Route:** `POST /api/sync/push`
- **Access:** Protected

#### Request Body
```json
{
  "mutations": [
    {
      "mutationId": "MUT-LOCAL-001",
      "entity": "STUDENT",
      "entityId": "202400123",
      "action": "UPDATE",
      "clientVersion": 3,
      "payload": {
        "name": "Peculiar Banda",
        "programme": "CS"
      }
    }
  ]
}
```

#### Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "applied": [ "MUT-LOCAL-001" ],
    "conflicts": []
  }
}
```
If a conflict is detected (`serverVersion > clientVersion`), the server returns the conflict payload for the Android UI to display the §17 Conflict Resolution Screen.
