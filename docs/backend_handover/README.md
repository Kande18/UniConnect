# UniConnect — Complete Backend Handover Package

Welcome to the **UniConnect** Backend Development & Database Architecture handover package.
This package contains everything needed for the backend developer (and their AI coding assistant, such as Antigravity) to design, build, and deploy the server and database infrastructure for the UniConnect platform.

---

## 📁 Package Directory Structure

```
docs/backend_handover/
├── README.md                                          <-- You are here (Handover Index & Quickstart)
├── PART_1_SYSTEM_ARCHITECTURE_AND_PLANNING.md         <-- System Blueprint, Boundaries & Master Agent Prompt
├── PART_2_BACKEND_API_SPECIFICATION.md                <-- Complete REST API Contracts & Endpoint Schemas
├── PART_3_DATABASE_SCHEMA_AND_MIGRATIONS.md           <-- MySQL Relational DDL, ERD & Seed Data Scripts
├── PART_4_ANDROID_INTEGRATION_AND_DATA_LAYER.md       <-- Android MVVM, Retrofit, Room & ViewModel Mappings
└── original_specs/                                    <-- Original Assignment & Architecture Inputs
    ├── INSTRUCTIONS.md                                <-- 1,400+ Line Master Assignment Instructions
    ├── DATA_CONTRACTS.md                              <-- Initial Data Contracts per Screen
    ├── NAVIGATION_FLOW.md                             <-- Screen Navigation Flowchart (Mermaid)
    └── UI_STATE_CONTRACTS.md                          <-- Common & Screen-Specific Presentation States
```

---

## 🎯 What Has Been Built (Android Client Status)
The UI/UX Team has completed the entire native Android user interface in Java & XML (tested and verified with Gradle build passing):
1. **Welcome & Gateway Screen:** Branded launchpad into Student or Lecturer portals.
2. **Student Authentication:** Registration with validation, claim code entry, and login.
3. **Student Portal (4-Button Bottom Nav):**
   - **Profile:** Academic info, student number, lab group.
   - **My Group:** Schedule, venue, instructor, occupancy progress.
   - **Edit Profile:** Name and programme editing (preserves student number).
   - **Change Request:** Radio selection to request transfer to another cohort.
4. **Lecturer Portal (4-Button Bottom Nav):**
   - **Overview:** Executive KPI cards, live group gauges, recent activity feed.
   - **Roster:** Real-time search, filter chips, student cards, "+ Add Student" FAB.
   - **Lab Groups:** Total lab capacity meter, schedule/venue cards, unassigned pool.
   - **Requests:** Interactive transfer approval/decline panel with status feedback.
5. **Student Editor & Group Assignment:** Add/Edit modes with delete confirmation dialog.

---

## 🛠️ What the Backend Team Needs to Build
Your team is responsible for the **Server & Data Layer**:
1. **Node.js + Express REST API:**
   - Stateless backend service running on port `5000`.
   - JWT authentication (`/api/auth/student-login`, `/api/auth/lecturer-login`, `/api/auth/register`).
   - Role-Based Access Control (`STUDENT` vs `LECTURER`).
2. **MySQL 8.0 Relational Database:**
   - Execute DDL migrations from `PART_3_DATABASE_SCHEMA_AND_MIGRATIONS.md`.
   - Execute the seed data script to populate default cohorts (`G01`–`G04`), staff accounts (`Dr. A. Banda`), and sample students.
3. **Enforce Core Business Invariants (§5, §25):**
   - Hard cap of **15 students per lab group**.
   - Server-side atomic locking (`SELECT ... FOR UPDATE`) during group assignments and transfer approvals.
   - 9-digit student number format preservation.
   - Department claim code verification.
4. **Sync Endpoints:**
   - `/api/sync/pull` and `/api/sync/push` for offline delta synchronization.

---

## 🚀 Quickstart for the Backend PC / Antigravity Agent

1. **Install Node.js & MySQL 8.0** on the development machine.
2. **Execute the Database Script:**
   - Run `PART_3_DATABASE_SCHEMA_AND_MIGRATIONS.md` in MySQL to initialize tables and seed data.
3. **Prompt Your AI Assistant:**
   - Open `PART_1_SYSTEM_ARCHITECTURE_AND_PLANNING.md` Section 4, copy the **Master AI Agent Prompt**, and paste it into Antigravity or your AI tool.
4. **Follow the API Specification:**
   - Reference `PART_2_BACKEND_API_SPECIFICATION.md` for exact request/response schemas.
