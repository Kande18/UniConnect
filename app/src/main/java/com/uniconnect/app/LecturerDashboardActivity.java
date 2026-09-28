package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * LecturerDashboardActivity  (§14.2)
 * ====================================
 * Landing screen after a successful Lecturer Sign In.
 *
 * What this screen shows:
 *   - Greeting header with lecturer name and sync status chip
 *   - Group Summary section: Total, G01, G02, G03, G04, Unassigned counts
 *   - Quick Actions: View Roster button, Add Student button
 *   - Sign Out button in the toolbar
 *
 * Integration note for the Android Architecture team:
 *   Replace the populateGroupCards() call with a ViewModel observer.
 *   All TextViews and ProgressBars are accessible via the group card view IDs.
 *   The navigation targets (LecturerRosterActivity, etc.) are ready to receive
 *   search/filter parameters via Intent extras.
 */
public class LecturerDashboardActivity extends AppCompatActivity {

    // ─── Toolbar ───────────────────────────────────────────────────────────
    private MaterialToolbar toolbarDashboard;

    // ─── Quick Action Buttons ───────────────────────────────────────────────
    private MaterialButton btnViewRoster;
    private MaterialButton btnAddStudent;

    // ─── Group Stat Card views (accessed via cardG0x.findViewById) ─────────
    private MaterialCardView cardG01;
    private MaterialCardView cardG02;
    private MaterialCardView cardG03;
    private MaterialCardView cardG04;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        initViews();
        setupToolbar();
        populateGroupCards();   // TODO: Replace with ViewModel observer
        setupButtons();
    }

    // ─── Initialise view references ────────────────────────────────────────
    private void initViews() {
        toolbarDashboard = findViewById(R.id.toolbarDashboard);
        btnViewRoster    = findViewById(R.id.btnViewRoster);
        btnAddStudent    = findViewById(R.id.btnAddStudent);
        cardG01          = findViewById(R.id.cardG01);
        cardG02          = findViewById(R.id.cardG02);
        cardG03          = findViewById(R.id.cardG03);
        cardG04          = findViewById(R.id.cardG04);
    }

    // ─── Toolbar: title + sign-out menu ────────────────────────────────────
    private void setupToolbar() {
        setSupportActionBar(toolbarDashboard);
        toolbarDashboard.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sign_out) {
                signOut();
                return true;
            }
            return false;
        });
    }

    /**
     * Populates the four group stat cards with mock data.
     * TODO: Replace mock data with ViewModel state once the
     *       Backend/Android Architecture team connects their ViewModel.
     *
     * Mock data:
     *   G01 → 14 / 15  (93%)
     *   G02 → 10 / 15  (67%)
     *   G03 →  9 / 15  (60%)
     *   G04 → 13 / 15  (87%)
     */
    private void populateGroupCards() {
        bindGroupCard(cardG01, "G01", 14, 15);
        bindGroupCard(cardG02, "G02", 10, 15);
        bindGroupCard(cardG03, "G03",  9, 15);
        bindGroupCard(cardG04, "G04", 13, 15);
    }

    /**
     * Binds label, count, capacity, and progress bar for one group stat card.
     *
     * @param card     The MaterialCardView containing the group stat layout.
     * @param label    Group label string, e.g. "G01".
     * @param count    Current number of enrolled students.
     * @param capacity Maximum group capacity.
     */
    private void bindGroupCard(@NonNull MaterialCardView card,
                               @NonNull String label,
                               int count,
                               int capacity) {
        TextView tvLabel    = card.findViewById(R.id.tvGroupLabel);
        TextView tvCount    = card.findViewById(R.id.tvGroupCount);
        TextView tvCapacity = card.findViewById(R.id.tvGroupCapacity);
        ProgressBar progress = card.findViewById(R.id.progressGroup);

        if (tvLabel != null)    tvLabel.setText(label);
        if (tvCount != null)    tvCount.setText(String.valueOf(count));
        if (tvCapacity != null) tvCapacity.setText(getString(R.string.label_occupancy_of, capacity));
        if (progress != null) {
            int percent = capacity > 0 ? (count * 100) / capacity : 0;
            progress.setProgress(percent);
            // Visually highlight full groups in warning orange
            if (count >= capacity) {
                progress.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                getResources().getColor(R.color.status_warning, getTheme())
                        )
                );
            }
        }

        // Accessibility: describe the card fully for TalkBack
        card.setContentDescription(
                getString(R.string.a11y_group_count, label, count)
        );
    }

    // ─── Button navigation ──────────────────────────────────────────────────
    private void setupButtons() {
        // View Roster → navigates to Lecturer Student Roster (Phase 5)
        btnViewRoster.setOnClickListener(v -> navigateToRoster());

        // Add Student → navigates to Student Editor in "add" mode (Phase 5)
        btnAddStudent.setOnClickListener(v -> navigateToAddStudent());
    }

    private void navigateToRoster() {
        Intent intent = new Intent(this, LecturerRosterActivity.class);
        startActivity(intent);
    }

    private void navigateToAddStudent() {
        Intent intent = new Intent(this, LecturerStudentEditorActivity.class);
        intent.putExtra(LecturerStudentEditorActivity.EXTRA_MODE,
                        LecturerStudentEditorActivity.MODE_ADD);
        startActivity(intent);
    }

    /**
     * Sign out: clear the entire back stack and return to Welcome screen.
     * This ensures no lecturer data remains on-screen for the next user.
     */
    private void signOut() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
