package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

/**
 * GroupAssignmentActivity  (§14.7)
 * ==================================
 * Shows 4 group rows so a lecturer can pick a new group for a student.
 *
 * What this screen shows:
 *   - Student name + current group chip (from intent extras)
 *   - Authority warning note
 *   - 4 radio-style group rows (G01–G04) with occupancy and FULL badge
 *   - Confirm Assignment button — returns selected group to editor
 *
 * Navigation pattern:
 *   Started via startActivityForResult() from LecturerStudentEditorActivity.
 *   On confirm, calls setResult(RESULT_OK, intent with RESULT_GROUP).
 *
 * Integration note:
 *   Replace bindGroupRow mock data with ViewModel group occupancy LiveData.
 */
public class GroupAssignmentActivity extends AppCompatActivity {

    /** Intent extra key for the group selected by lecturer. */
    public static final String RESULT_GROUP    = "selected_group";
    public static final String EXTRA_CURRENT   = "current_group";

    // Mock group data (TODO: Replace with ViewModel)
    private static final int[] GROUP_COUNTS    = {14, 10,  9, 15};
    private static final int   GROUP_CAPACITY  = 15;
    private static final String[] GROUP_LABELS = {"G01", "G02", "G03", "G04"};

    // ─── Views ────────────────────────────────────────────────────────────
    private MaterialToolbar toolbarGroupAssign;
    private TextView tvAssignStudentName;
    private Chip chipCurrentGroup;
    private RadioGroup radioGroupAssign;
    private MaterialButton btnConfirmAssignment;

    // Row include views
    private View rowG01, rowG02, rowG03, rowG04;

    // Currently selected group label
    private String selectedGroupLabel = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_assignment);

        initViews();
        setupToolbar();
        populateStudentInfo();
        bindGroupRows();   // TODO: Replace with ViewModel observer
        setupConfirmButton();
    }

    private void initViews() {
        toolbarGroupAssign   = findViewById(R.id.toolbarGroupAssign);
        tvAssignStudentName  = findViewById(R.id.tvAssignStudentName);
        chipCurrentGroup     = findViewById(R.id.chipCurrentGroup);
        radioGroupAssign     = findViewById(R.id.radioGroupAssign);
        btnConfirmAssignment = findViewById(R.id.btnConfirmAssignment);
        rowG01 = findViewById(R.id.rowG01);
        rowG02 = findViewById(R.id.rowG02);
        rowG03 = findViewById(R.id.rowG03);
        rowG04 = findViewById(R.id.rowG04);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarGroupAssign);
        toolbarGroupAssign.setNavigationOnClickListener(v -> finish());
    }

    private void populateStudentInfo() {
        // TODO: Replace with ViewModel.getStudent().observe(...)
        if (tvAssignStudentName != null)
            tvAssignStudentName.setText("Peculiar Banda");

        String currentGroup = getIntent().getStringExtra(EXTRA_CURRENT);
        if (chipCurrentGroup != null && currentGroup != null)
            chipCurrentGroup.setText(currentGroup);
    }

    /**
     * Binds mock occupancy data to each group row.
     * TODO: Replace GROUP_COUNTS with ViewModel group-occupancy LiveData.
     */
    private void bindGroupRows() {
        View[] rows = {rowG01, rowG02, rowG03, rowG04};

        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == null) continue;

            RadioButton radio    = rows[i].findViewById(R.id.radioGroup);
            TextView occupancy   = rows[i].findViewById(R.id.tvGroupOccupancy);
            TextView fullBadge   = rows[i].findViewById(R.id.tvFullBadge);

            String label = GROUP_LABELS[i];
            int count    = GROUP_COUNTS[i];
            boolean full = count >= GROUP_CAPACITY;

            if (radio != null) {
                radio.setText(label);
                radio.setEnabled(!full);
                radio.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) selectedGroupLabel = label;
                });
            }
            if (occupancy != null) {
                occupancy.setText(count + " / " + GROUP_CAPACITY + " students");
                occupancy.setTextColor(getResources().getColor(
                        full ? R.color.status_warning : R.color.tagline_charcoal,
                        getTheme()));
            }
            if (fullBadge != null) {
                fullBadge.setVisibility(full ? View.VISIBLE : View.GONE);
            }

            // Accessibility description
            rows[i].setContentDescription(full
                    ? getString(R.string.desc_group_full_row, label, count, GROUP_CAPACITY)
                    : getString(R.string.desc_group_row, label, count, GROUP_CAPACITY));
        }
    }

    private void setupConfirmButton() {
        btnConfirmAssignment.setOnClickListener(v -> {
            if (selectedGroupLabel == null) {
                // Prompt user to select a group
                btnConfirmAssignment.setError("Please select a group");
                return;
            }
            Intent result = new Intent();
            result.putExtra(RESULT_GROUP, selectedGroupLabel);
            setResult(RESULT_OK, result);
            finish();
        });
    }
}
