# Data Contracts per Screen

## Welcome Screen
- No data required beyond static strings.

## Student Registration Screen
- **Input fields** (user‑entered):
  - `studentNumber` (String) – entered text preserved on validation error.
  - `name` (String)
  - `programme` (Enum: CS, IT, DS)
  - `password` (String)
  - `confirmPassword` (String)
- **UI‑state fields** supplied by ViewModel:
  - `registrationState` (enum) – one of: `Idle`, `Loading`, `SavedLocally`, `PendingVerification`, `Syncing`, `Synced`, `ActionRequired`, `Error`.
  - `errorMessage` (String?) – optional server‑side error (e.g., duplicate student number).

## Student Login Screen
- `studentNumber` (String)
- `password` (String)
- `loginState` (enum) – `Idle`, `Loading`, `Error`, `NetworkError`, `Offline`.

## Student Home Screen
- `studentProfile` object containing:
  - `name`, `studentNumber`, `programme`, `labGroup`, `groupOccupancy`, `syncStatus`.
- `homeState` – `Loading`, `Content`, `Error`, `Offline`.

## Profile Screen (read‑only)
- Same fields as `studentProfile` above.
- `profileState` – `Loading`, `Content`, `Error`.

## Edit Profile Screen
- Editable fields: `name`, `programme`.
- `editState` – `Idle`, `Loading`, `ValidationError`, `SavedLocally`, `Pending`, `Syncing`, `Synced`, `ActionRequired`, `Error`.

## Group Change Request Screen
- `currentGroup` (String)
- `availableGroups` (list of objects: `{label, occupancy, isFull}`) – UI only displays `isFull` flag supplied by ViewModel.
- `requestState` – `Idle`, `Loading`, `SavedLocally`, `Pending`, `Syncing`, `Synced`, `ActionRequired`, `Error`.

## Lecturer Dashboard Screen
- Summary numbers: `totalStudents`, `assignedStudents`, `unassignedStudents`, `groupCounts` (map of group label → count).
- `dashboardState` – `Loading`, `Content`, `Error`, `Offline`.

## Student Roster / Search / Filters Screen
- `studentList` (list of student summary objects).
- `listState` – `Loading`, `Empty`, `Content`, `Error`, `Offline`.

## Student Editor Screen
- Editable fields: `name`, `programme`, `labGroup`.
- `editorState` – same set as Edit Profile plus `Conflict` if server reports a conflict.

## Group Assignment Screen
- `studentId` (String) – identifies which student is being edited.
- `currentGroup` (String)
- `availableGroups` (list with `isFull` flag).
- `assignmentState` – `Idle`, `Loading`, `SavedLocally`, `Pending`, `Syncing`, `Synced`, `ActionRequired`, `Error`.

## Delete Confirmation Dialog
- `studentName` (String) for display only.
- No UI‑state beyond the dialog visibility.

> **Note:** All data structures above are *illustrative only*. The Architecture team will define the concrete model classes and expose them via LiveData/StateFlow. The UI merely consumes the fields listed.
