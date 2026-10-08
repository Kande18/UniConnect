# UI State Contracts

This document describes the **presentation‑only** UI states that each screen must be able to display. The UI does **not** implement any business logic; it simply reacts to a state object supplied by the ViewModel/Architecture layer.

---

## Common State Definitions (used on many screens)
| State | Visual representation | Message (string resource) | Controls enabled? | Accessibility (TalkBack) |
|-------|----------------------|---------------------------|-------------------|--------------------------|
| **Loading** | Full‑screen or inline progress indicator (Material `CircularProgressIndicator`). | `@string/state_loading` | All interactive controls disabled. | Announces "Loading…" via live region. |
| **Content / Loaded** | Normal UI layout with data populated. | – | Normal interaction. | No special announcement. |
| **Empty** | Empty‑state illustration + text. | `@string/state_empty` | Controls disabled (e.g., refresh button may stay enabled). | Announces "No data found." |
| **Validation Error** | Inline error text below the offending field (Material `TextInputLayout` helper). | Specific error string (e.g., `@string/error_student_number_invalid`). | Form submission button disabled until error cleared. | Announces "Error: …" via `android:contentDescription`. |
| **Network Error** | Full‑screen error view with retry button. | `@string/error_network` | Retry button enabled, others disabled. | Announces "Network error. Tap to retry." |
| **Offline** | Banner or toast indicating offline mode; UI still usable. | `@string/error_offline` | All input fields remain enabled; submit actions will be queued. | Announces "No internet connection. Changes will be saved locally." |
| **Saved Locally** | Small snackbar/toast confirming local save. | `@string/state_saved_locally` | UI remains interactive. | Announces "Saved locally." |
| **Pending Verification** | Inline banner stating the request is pending. | `@string/state_pending` | UI remains interactive, but actions that would duplicate the request are hidden/disabled. | Announces "Pending verification." |
| **Syncing** | Progress indicator with message. | `@string/state_syncing` | UI controls disabled that could modify the same entity. | Announces "Syncing…" |
| **Synced** | Check‑mark icon with text. | `@string/state_synced` | All controls enabled. | Announces "Synced." |
| **Action Required** | Prominent banner with an action button (e.g., "Resolve" or "Retry"). | `@string/state_action_required` | Action button enabled; others may stay enabled. | Announces "Action required." |
| **Conflict** | Conflict view showing **Local proposal** vs **Server value** side‑by‑side, with a **Review** button. | `@string/state_conflict` | Only the Review button enabled. | Announces "Conflict detected. Press Review to continue." |
| **Group Full** | Disabled selection option with label “Full”. | `@string/error_group_full` | Selection disabled for that group. | Announces "Group full." |
| **Session Expired** | Full‑screen dialog forcing sign‑in again. | `@string/state_session_expired` | Only sign‑in button enabled. | Announces "Session expired. Please sign in again." |

---

## Screen‑Specific Contracts
Below are the screens that need the above states and any additional UI nuances.

### Student Registration
- Supports **Offline**, **Saved Locally**, **Pending**, **Syncing**, **Synced**, **Action Required** (e.g., duplicate student number after sync).
- Validation errors for each field are shown inline.

### Student Login / Lecturer Login
- Loading, Network Error, Offline (show banner), Session Expired.

### Student Home → Profile
- Loading, Content, Offline (show banner), Syncing (if a background update arrives), Conflict (if server record changed), Action Required.

### Edit Profile / Student Editor / Group Assignment
- All states listed above, plus **Group Full** handling when a group option is presented as disabled.

### Lecturer Dashboard → Student Roster / Search / Filter
- Loading, Empty, Content, Network Error, Offline.

### Delete Confirmation Dialog
- Simple confirmation UI; no special state beyond the normal Content.

---

**Implementation note for developers**: The UI should expose a single `LiveData<UiState>` (or equivalent) per screen. The `UiState` enum/value must contain enough information to select an entry from this table and to provide any auxiliary data (e.g., error message, conflict details). The UI layer must **only** react to the state – no internal calculations such as group capacity.
