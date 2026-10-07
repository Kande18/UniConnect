package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

/**
 * LecturerStudentEditorActivity  (§14.6)
 * =========================================
 * Add or Edit a student record.
 *
 * Modes (set via EXTRA_MODE):
 *   MODE_ADD  — shows "Add Student" title, hides delete button
 *   MODE_EDIT — shows "Edit Student" title, shows delete button, pre-fills fields
 *
 * Navigation:
 *   - cardGroupAssign tapped → opens GroupAssignmentActivity for result
 *   - On GROUP_SELECTED result → updates tvCurrentGroup
 *   - btnEditorSave → validates and shows success banner
 *   - btnEditorDelete → shows confirmation dialog (§14.8)
 *   - btnEditorCancel → finish()
 *
 * Integration note:
 *   Replace mock field pre-fill with ViewModel.getStudent(studentId).observe(...)
 *   Connect btnEditorSave click to ViewModel.saveStudent(...)
 *   Observe saveResult LiveData to show success/error banner.
 */
public class LecturerStudentEditorActivity extends AppCompatActivity {

    public static final String EXTRA_MODE        = "mode";
    public static final String EXTRA_STUDENT_ID  = "student_id";
    public static final String MODE_ADD          = "add";
    public static final String MODE_EDIT         = "edit";

    // Request code for group assignment result
    private static final int REQUEST_GROUP_ASSIGN = 1001;

    // ─── Views ────────────────────────────────────────────────────────────
    private MaterialToolbar toolbarEditor;
    private MaterialTextView tvEditorSubtitle;
    private TextInputLayout tilEditorName;
    private TextInputEditText etEditorName;
    private TextInputLayout tilEditorStudentNumber;
    private TextInputEditText etEditorStudentNumber;
    private AutoCompleteTextView actvEditorProgramme;
    private MaterialCardView cardGroupAssign;
    private MaterialTextView tvCurrentGroup;
    private MaterialButton btnEditorSave;
    private MaterialButton btnEditorCancel;
    private MaterialButton btnEditorDelete;
    private MaterialCardView bannerEditorSuccess;
    private MaterialCardView bannerEditorError;
    private LinearLayout progressSaving;

    private static final String STATE_GROUP = "state_group";

    private TextInputLayout tilEditorProgramme;

    private String currentMode = MODE_ADD;

    // Snapshot of the form when it was loaded/saved, used to detect unsaved changes
    private String initialName = "";
    private String initialNumber = "";
    private String initialProgramme = "";
    private String initialGroup = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_student_editor);

        currentMode = getIntent().getStringExtra(EXTRA_MODE);
        if (currentMode == null) currentMode = MODE_ADD;

        initViews();
        setupToolbar();
        setupProgrammeDropdown();
        configureForMode();
        setupButtons();

        if (MODE_EDIT.equals(currentMode)) {
            prefillMockData(); // TODO: Replace with ViewModel.getStudent(studentId)
        }

        captureBaseline();
        setupErrorClearing();
        setupBackNavigation();

        // Keep the group chosen on the assignment screen after rotation
        if (savedInstanceState != null) {
            String savedGroup = savedInstanceState.getString(STATE_GROUP);
            if (savedGroup != null && tvCurrentGroup != null) tvCurrentGroup.setText(savedGroup);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_GROUP, getGroupText());
    }

    private void initViews() {
        toolbarEditor          = findViewById(R.id.toolbarEditor);
        tvEditorSubtitle       = findViewById(R.id.tvEditorSubtitle);
        tilEditorName          = findViewById(R.id.tilEditorName);
        etEditorName           = findViewById(R.id.etEditorName);
        tilEditorStudentNumber = findViewById(R.id.tilEditorStudentNumber);
        tilEditorProgramme     = findViewById(R.id.tilEditorProgramme);
        etEditorStudentNumber  = findViewById(R.id.etEditorStudentNumber);
        actvEditorProgramme    = findViewById(R.id.actvEditorProgramme);
        cardGroupAssign        = findViewById(R.id.cardGroupAssign);
        tvCurrentGroup         = findViewById(R.id.tvCurrentGroup);
        btnEditorSave          = findViewById(R.id.btnEditorSave);
        btnEditorCancel        = findViewById(R.id.btnEditorCancel);
        btnEditorDelete        = findViewById(R.id.btnEditorDelete);
        bannerEditorSuccess    = findViewById(R.id.bannerEditorSuccess);
        bannerEditorError      = findViewById(R.id.bannerEditorError);
        progressSaving         = findViewById(R.id.progressSaving);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarEditor);
        toolbarEditor.setNavigationOnClickListener(v -> handleExit());
    }

    private void setupProgrammeDropdown() {
        String[] programmes = getResources().getStringArray(R.array.programme_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, programmes);
        actvEditorProgramme.setAdapter(adapter);
    }

    /** Adjust title, subtitle, button label and delete-button visibility by mode. */
    private void configureForMode() {
        if (MODE_ADD.equals(currentMode)) {
            toolbarEditor.setTitle(getString(R.string.editor_title_add));
            tvEditorSubtitle.setText(R.string.editor_subtitle_add);
            btnEditorSave.setText(R.string.editor_btn_add);
            btnEditorDelete.setVisibility(View.GONE);
        } else {
            toolbarEditor.setTitle(getString(R.string.editor_title_edit));
            tvEditorSubtitle.setText(R.string.editor_subtitle_edit);
            btnEditorSave.setText(R.string.editor_btn_save);
            btnEditorDelete.setVisibility(View.VISIBLE);
        }
    }

    private void setupButtons() {
        // Group assignment card — navigate to GroupAssignmentActivity
        cardGroupAssign.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupAssignmentActivity.class);
            intent.putExtra("current_group", getGroupText());
            startActivityForResult(intent, REQUEST_GROUP_ASSIGN);
        });

        // Save — validate then show success banner
        btnEditorSave.setOnClickListener(v -> attemptSave());

        // Cancel — go back (asks first if there are unsaved changes)
        btnEditorCancel.setOnClickListener(v -> handleExit());

        // Delete — show confirmation dialog (§14.8)
        btnEditorDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    // ─── Group assignment result ──────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GROUP_ASSIGN && resultCode == RESULT_OK && data != null) {
            String selectedGroup = data.getStringExtra(GroupAssignmentActivity.RESULT_GROUP);
            if (selectedGroup != null && tvCurrentGroup != null) {
                tvCurrentGroup.setText(selectedGroup);
            }
        }
    }

    // ─── Validation & Save ────────────────────────────────────────────────
    private void attemptSave() {
        boolean valid = true;
        View firstInvalid = null;
        tilEditorName.setError(null);
        tilEditorStudentNumber.setError(null);
        tilEditorProgramme.setError(null);

        String name   = etEditorName.getText() != null
                ? etEditorName.getText().toString().trim() : "";
        String number = etEditorStudentNumber.getText() != null
                ? etEditorStudentNumber.getText().toString().trim() : "";

        String programme = actvEditorProgramme.getText() != null
                ? actvEditorProgramme.getText().toString().trim() : "";

        if (name.length() < 2 || name.length() > 100) {
            tilEditorName.setError(getString(R.string.error_name_required));
            firstInvalid = etEditorName;
            valid = false;
        }
        if (!number.matches("\\d{9}")) {
            tilEditorStudentNumber.setError(getString(R.string.error_student_number_invalid));
            if (firstInvalid == null) firstInvalid = etEditorStudentNumber;
            valid = false;
        }
        if (programme.isEmpty()) {
            tilEditorProgramme.setError(getString(R.string.error_programme_required));
            if (firstInvalid == null) firstInvalid = actvEditorProgramme;
            valid = false;
        }

        if (!valid) {
            // Typed values stay exactly as they are; focus jumps to the first problem for TalkBack
            if (firstInvalid != null) firstInvalid.requestFocus();
            return;
        }

        if (valid) {
            captureBaseline();
            // TODO: Replace with ViewModel.saveStudent(...)
            showSuccessBanner(MODE_ADD.equals(currentMode)
                    ? getString(R.string.editor_success_added)
                    : getString(R.string.editor_success_saved));
        }
    }

    // ─── Unsaved-changes handling & Back navigation ───────────────────────
    private String textOf(android.widget.TextView v) {
        return v != null && v.getText() != null ? v.getText().toString().trim() : "";
    }

    private void captureBaseline() {
        initialName = textOf(etEditorName);
        initialNumber = textOf(etEditorStudentNumber);
        initialProgramme = textOf(actvEditorProgramme);
        initialGroup = getGroupText();
    }

    private boolean hasUnsavedChanges() {
        return !initialName.equals(textOf(etEditorName))
                || !initialNumber.equals(textOf(etEditorStudentNumber))
                || !initialProgramme.equals(textOf(actvEditorProgramme))
                || !initialGroup.equals(getGroupText());
    }

    /** Toolbar arrow, Cancel button and system Back all come through here. */
    private void handleExit() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.editor_discard_title)
                    .setMessage(R.string.editor_discard_message)
                    .setNegativeButton(R.string.editor_discard_keep, null)
                    .setPositiveButton(R.string.editor_discard_confirm, (dialog, which) -> finish())
                    .show();
        } else {
            finish();
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });
    }

    /** Remove an error message as soon as the lecturer starts correcting that field. */
    private void setupErrorClearing() {
        etEditorName.addTextChangedListener(new SimpleWatcher(tilEditorName));
        etEditorStudentNumber.addTextChangedListener(new SimpleWatcher(tilEditorStudentNumber));
        actvEditorProgramme.setOnItemClickListener((parent, v, position, id) -> tilEditorProgramme.setError(null));
    }

    private static class SimpleWatcher implements TextWatcher {
        private final TextInputLayout layout;
        SimpleWatcher(TextInputLayout layout) { this.layout = layout; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) { layout.setError(null); }
        @Override public void afterTextChanged(Editable s) {}
    }

    private void showSuccessBanner(String message) {
        bannerEditorError.setVisibility(View.GONE);
        bannerEditorSuccess.setVisibility(View.VISIBLE);
        MaterialTextView tv = bannerEditorSuccess.findViewById(R.id.tvEditorSuccessMsg);
        if (tv != null) tv.setText(message);
    }

    // ─── Delete confirmation dialog (§14.8) ───────────────────────────────
    private void showDeleteConfirmation() {
        String studentName = etEditorName.getText() != null
                ? etEditorName.getText().toString().trim() : "";

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message))
                .setNegativeButton(R.string.delete_btn_cancel, null)
                .setPositiveButton(R.string.delete_btn_confirm, (dialog, which) -> {
                    // TODO: Call ViewModel.deleteStudent(studentId)
                    finish(); // Return to roster after delete
                })
                .show();
    }

    // ─── Mock data ────────────────────────────────────────────────────────
    /**
     * Pre-fills fields with mock data in EDIT mode.
     * TODO: Replace with ViewModel.getStudent(studentId).observe(...)
     */
    private void prefillMockData() {
        etEditorName.setText("Peculiar Banda");
        etEditorStudentNumber.setText("202400123");
        actvEditorProgramme.setText("CS (Computer Science)", false);
        if (tvCurrentGroup != null) tvCurrentGroup.setText("G01");
    }

    private String getGroupText() {
        return tvCurrentGroup != null
                ? tvCurrentGroup.getText().toString() : "Unassigned";
    }
}
