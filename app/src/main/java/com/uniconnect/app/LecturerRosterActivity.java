package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * LecturerRosterActivity  (§14.3 / §14.4 / §14.5)
 * ==================================================
 * Displays the student roster with search and filter support.
 *
 * What this screen shows:
 *   - Search bar (by name or student number)
 *   - Filter button opening FilterDialogFragment
 *   - Active filter chips displayed inline
 *   - RecyclerView of student cards
 *   - Loading / Empty / Error states
 *   - Offline banner when running on cached data
 *   - FAB to add a new student
 *
 * Integration note for the Android Architecture team:
 *   Replace showMockStudents() with a ViewModel LiveData observer.
 *   Call adapter.submitList(students) on data change.
 *   Call showState(UiState.LOADING/EMPTY/ERROR/LIST) to toggle views.
 *   The search EditText text change listener is already wired — connect it
 *   to ViewModel.setSearchQuery(query).
 */
public class LecturerRosterActivity extends AppCompatActivity {

    // ─── Views ────────────────────────────────────────────────────────────
    private MaterialToolbar toolbarRoster;
    private TextInputEditText etSearch;
    private MaterialButton btnOpenFilter;
    private LinearLayout viewLoading;
    private LinearLayout viewEmpty;
    private LinearLayout viewError;
    private RecyclerView recyclerStudents;
    private ExtendedFloatingActionButton fabAddStudent;
    private MaterialButton btnRetry;
    private TextView tvEmptyMessage;
    private TextView tvEmptyHint;

    // ─── Adapter ──────────────────────────────────────────────────────────
    private StudentRosterAdapter adapter;

    // ─── Mock data (TODO: Replace with ViewModel) ─────────────────────────
    private List<StudentItem> allStudents = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_roster);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupButtons();
        showMockStudents(); // TODO: Replace with ViewModel observer
        // Back button: the toolbar arrow and the system Back both simply leave the roster.
    }

    // ─── Initialise views ─────────────────────────────────────────────────
    private void initViews() {
        toolbarRoster   = findViewById(R.id.toolbarRoster);
        etSearch        = findViewById(R.id.etSearch);
        btnOpenFilter   = findViewById(R.id.btnOpenFilter);
        viewLoading     = findViewById(R.id.viewLoading);
        viewEmpty       = findViewById(R.id.viewEmpty);
        viewError       = findViewById(R.id.viewError);
        recyclerStudents = findViewById(R.id.recyclerStudents);
        fabAddStudent   = findViewById(R.id.fabAddStudent);
        btnRetry        = findViewById(R.id.btnRetry);
        tvEmptyMessage  = findViewById(R.id.tvEmptyMessage);
        tvEmptyHint     = findViewById(R.id.tvEmptyHint);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbarRoster);
        toolbarRoster.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new StudentRosterAdapter(student -> {
            // On student card tap → open editor in EDIT mode
            Intent intent = new Intent(this, LecturerStudentEditorActivity.class);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_MODE,
                            LecturerStudentEditorActivity.MODE_EDIT);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_STUDENT_ID,
                            student.studentNumber);
            startActivity(intent);
        });
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        recyclerStudents.setAdapter(adapter);
    }

    // ─── Search wiring ────────────────────────────────────────────────────
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterStudents(s.toString().trim());
                // TODO: Replace with ViewModel.setSearchQuery(query)
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─── Buttons ──────────────────────────────────────────────────────────
    private void setupButtons() {
        fabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(this, LecturerStudentEditorActivity.class);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_MODE,
                            LecturerStudentEditorActivity.MODE_ADD);
            startActivity(intent);
        });

        btnOpenFilter.setOnClickListener(v -> {
            FilterDialogFragment dialog = new FilterDialogFragment();
            dialog.show(getSupportFragmentManager(), "FilterDialog");
        });

        btnRetry.setOnClickListener(v -> showMockStudents());
        // TODO: Replace retry with ViewModel.reload()
    }

    // ─── UI State toggling ────────────────────────────────────────────────
    private void showState(String state) {
        viewLoading.setVisibility(View.GONE);
        viewEmpty.setVisibility(View.GONE);
        viewError.setVisibility(View.GONE);
        recyclerStudents.setVisibility(View.GONE);

        switch (state) {
            case "loading": viewLoading.setVisibility(View.VISIBLE);      break;
            case "empty":   viewEmpty.setVisibility(View.VISIBLE);        break;
            case "error":   viewError.setVisibility(View.VISIBLE);        break;
            case "list":    recyclerStudents.setVisibility(View.VISIBLE); break;
        }
    }

    // ─── Mock data population ─────────────────────────────────────────────
    /**
     * Loads temporary mock students for UI demonstration.
     * TODO: Replace with ViewModel.getStudents().observe(this, students -> {...})
     */
    private void showMockStudents() {
        allStudents.clear();
        allStudents.add(new StudentItem("Peculiar Banda",   "202400123", "CS", "G01"));
        allStudents.add(new StudentItem("Amara Dlamini",    "202400456", "IT", "G01"));
        allStudents.add(new StudentItem("Tendai Moyo",      "202400789", "DS", "G02"));
        allStudents.add(new StudentItem("Nkosi Zulu",       "202401011", "CS", "G02"));
        allStudents.add(new StudentItem("Fatima Patel",     "202401234", "IT", "G03"));
        allStudents.add(new StudentItem("Sipho Ndlovu",     "202401567", "DS", "G03"));
        allStudents.add(new StudentItem("Grace Mwangi",     "202401890", "CS", "G04"));
        allStudents.add(new StudentItem("Chidi Okafor",     "202402123", "IT", "Unassigned"));

        // Re-apply whatever is typed in the search box so a reload/retry never ignores it
        filterStudents(currentQuery());
    }

    private String currentQuery() {
        return etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
    }

    // ─── Search filter ────────────────────────────────────────────────────
    private void filterStudents(String query) {
        if (query.isEmpty()) {
            adapter.setStudents(allStudents);
            if (tvEmptyHint != null) tvEmptyHint.setVisibility(View.GONE);
            // BUG FIX: clearing the search after "no results" used to leave the empty view on screen
            tvEmptyMessage.setText(R.string.roster_empty);
            showState(allStudents.isEmpty() ? "empty" : "list");
        } else {
            List<StudentItem> filtered = new ArrayList<>();
            String lower = query.toLowerCase();
            for (StudentItem s : allStudents) {
                if (s.name.toLowerCase().contains(lower)
                        || s.studentNumber.contains(query)) {
                    filtered.add(s);
                }
            }
            adapter.setStudents(filtered);
            if (filtered.isEmpty()) {
                tvEmptyMessage.setText(R.string.roster_empty_search);
                if (tvEmptyHint != null) tvEmptyHint.setVisibility(View.VISIBLE);
                showState("empty");
            } else {
                showState("list");
            }
        }
    }

    // ─── Simple data model (TODO: replace with real Student entity) ───────
    /** Temporary data holder for UI mock data. Replace with Room entity. */
    static class StudentItem {
        final String name;
        final String studentNumber;
        final String programme;
        final String group;

        StudentItem(String name, String number, String programme, String group) {
            this.name          = name;
            this.studentNumber = number;
            this.programme     = programme;
            this.group         = group;
        }
    }
}
