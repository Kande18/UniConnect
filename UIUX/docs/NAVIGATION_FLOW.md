```mermaid
flowchart TD
    Welcome -->|Student login| StudentLogin
    Welcome -->|Student register| StudentRegister
    Welcome -->|Lecturer login| LecturerLogin
    StudentLogin --> StudentHome
    StudentRegister --> StudentLogin
    StudentHome --> Profile
    StudentHome --> EditProfile
    StudentHome --> Group
    StudentHome --> GroupChangeReq
    LecturerLogin --> LecturerDashboard
    LecturerDashboard --> StudentRoster
    LecturerDashboard --> Search
    LecturerDashboard --> Filters
    LecturerDashboard --> StudentEditor
    LecturerDashboard --> GroupAssignment
    LecturerDashboard --> DeleteConfirm
```

**Navigation flow description**
- The UI presents the screens in the order above.
- Back navigation follows the reverse arrows; the Architecture team will wire it with the chosen navigation framework.
- All destination names (e.g., `StudentLogin`, `StudentHome`) are *logical* identifiers; concrete fragment/activity class names will be defined later.
