package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

/**
 * LecturerRosterFragment  (Lecturer Portal Tab 2)
 * ================================================
 * Searchable, filterable student directory:
 *   - Search by name or 9-digit student number
 *   - Quick filter chips (All, Unassigned, CS, IT)
 *   - Tap student -> opens LecturerStudentEditorActivity in EDIT mode
 *   - Tap FAB -> opens LecturerStudentEditorActivity in ADD mode
 */
public class LecturerRosterFragment extends Fragment {

    private TextInputEditText etFragSearch;
    private MaterialButton btnFragOpenFilter;
    private Chip chipFilterAll, chipFilterUnassigned, chipFilterCS, chipFilterIT;
    private LinearLayout fragViewLoading;
    private LinearLayout fragViewEmpty;
    private TextView tvFragEmptyMessage;
    private RecyclerView fragRecyclerStudents;
    private ExtendedFloatingActionButton fragFabAddStudent;

    private StudentRosterAdapter adapter;
    private final List<LecturerRosterActivity.StudentItem> allStudents = new ArrayList<>();
    private String currentProgrammeFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_lecturer_roster, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        setupFab();
        loadMockStudents(); // TODO: Connect with ViewModel
    }

    private void initViews(View root) {
        etFragSearch         = root.findViewById(R.id.etFragSearch);
        btnFragOpenFilter    = root.findViewById(R.id.btnFragOpenFilter);
        chipFilterAll        = root.findViewById(R.id.chipFilterAll);
        chipFilterUnassigned = root.findViewById(R.id.chipFilterUnassigned);
        chipFilterCS         = root.findViewById(R.id.chipFilterCS);
        chipFilterIT         = root.findViewById(R.id.chipFilterIT);
        fragViewLoading      = root.findViewById(R.id.fragViewLoading);
        fragViewEmpty        = root.findViewById(R.id.fragViewEmpty);
        tvFragEmptyMessage   = root.findViewById(R.id.tvFragEmptyMessage);
        fragRecyclerStudents = root.findViewById(R.id.fragRecyclerStudents);
        fragFabAddStudent    = root.findViewById(R.id.fragFabAddStudent);
    }

    private void setupRecyclerView() {
        adapter = new StudentRosterAdapter(student -> {
            Intent intent = new Intent(requireContext(), LecturerStudentEditorActivity.class);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_MODE,
                            LecturerStudentEditorActivity.MODE_EDIT);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_STUDENT_ID,
                            student.studentNumber);
            startActivity(intent);
        });
        fragRecyclerStudents.setLayoutManager(new LinearLayoutManager(requireContext()));
        fragRecyclerStudents.setAdapter(adapter);
    }

    private void setupSearch() {
        etFragSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterChips() {
        chipFilterAll.setOnClickListener(v -> {
            selectChip(chipFilterAll);
            currentProgrammeFilter = "All";
            applyFilters();
        });
        chipFilterUnassigned.setOnClickListener(v -> {
            selectChip(chipFilterUnassigned);
            currentProgrammeFilter = "Unassigned";
            applyFilters();
        });
        chipFilterCS.setOnClickListener(v -> {
            selectChip(chipFilterCS);
            currentProgrammeFilter = "CS";
            applyFilters();
        });
        chipFilterIT.setOnClickListener(v -> {
            selectChip(chipFilterIT);
            currentProgrammeFilter = "IT";
            applyFilters();
        });

        btnFragOpenFilter.setOnClickListener(v -> {
            FilterDialogFragment dialog = new FilterDialogFragment();
            dialog.show(getParentFragmentManager(), "FilterDialog");
        });
    }

    private void selectChip(Chip selected) {
        Chip[] chips = {chipFilterAll, chipFilterUnassigned, chipFilterCS, chipFilterIT};
        for (Chip c : chips) {
            boolean isTarget = (c == selected);
            c.setChecked(isTarget);
            c.setTextColor(isTarget ? 0xFFFFFFFF : 0xFF1E293B);
            c.setChipBackgroundColorResource(isTarget ? R.color.action_teal : R.color.white);
        }
    }

    private void setupFab() {
        fragFabAddStudent.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LecturerStudentEditorActivity.class);
            intent.putExtra(LecturerStudentEditorActivity.EXTRA_MODE,
                            LecturerStudentEditorActivity.MODE_ADD);
            startActivity(intent);
        });
    }

    private void loadMockStudents() {
        allStudents.clear();
        allStudents.add(new LecturerRosterActivity.StudentItem("Peculiar Banda",   "202400123", "CS", "G01"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Amara Dlamini",    "202400456", "IT", "G01"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Tendai Moyo",      "202400789", "DS", "G02"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Nkosi Zulu",       "202401011", "CS", "G02"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Fatima Patel",     "202401234", "IT", "G03"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Sipho Ndlovu",     "202401567", "DS", "G03"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Grace Mwangi",     "202401890", "CS", "G04"));
        allStudents.add(new LecturerRosterActivity.StudentItem("Chidi Okafor",     "202402123", "IT", "Unassigned"));

        applyFilters();
    }

    private void applyFilters() {
        String query = etFragSearch.getText() != null
                ? etFragSearch.getText().toString().trim().toLowerCase() : "";

        List<LecturerRosterActivity.StudentItem> filtered = new ArrayList<>();
        for (LecturerRosterActivity.StudentItem s : allStudents) {
            // Check programme/group filter
            boolean matchesFilter = true;
            if ("Unassigned".equals(currentProgrammeFilter)) {
                matchesFilter = "Unassigned".equalsIgnoreCase(s.group);
            } else if (!"All".equals(currentProgrammeFilter)) {
                matchesFilter = s.programme.equalsIgnoreCase(currentProgrammeFilter);
            }

            // Check search query
            boolean matchesSearch = query.isEmpty()
                    || s.name.toLowerCase().contains(query)
                    || s.studentNumber.contains(query);

            if (matchesFilter && matchesSearch) {
                filtered.add(s);
            }
        }

        adapter.setStudents(filtered);
        if (filtered.isEmpty()) {
            fragViewEmpty.setVisibility(View.VISIBLE);
            fragRecyclerStudents.setVisibility(View.GONE);
            tvFragEmptyMessage.setText(query.isEmpty()
                    ? "No students match selected filter."
                    : "No students match \"" + query + "\"");
        } else {
            fragViewEmpty.setVisibility(View.GONE);
            fragRecyclerStudents.setVisibility(View.VISIBLE);
        }
    }
}
