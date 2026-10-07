package com.uniconnect.app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * FilterDialogFragment  (§14.5)
 * ================================
 * Bottom-sheet dialog for filtering the student roster by:
 *   - Programme (All / CS / IT / DS)
 *   - Lab Group  (All / G01 / G02 / G03 / G04 / Unassigned)
 *
 * Integration note for the Backend team:
 *   When "Apply Filters" is clicked, call the parent activity's
 *   applyFilters(programme, group) method or use a shared ViewModel.
 *   Currently wired to a basic interface callback.
 */
public class FilterDialogFragment extends BottomSheetDialogFragment {

    /** Callback for when the user applies or resets filters. */
    public interface FilterListener {
        void onFiltersApplied(String programme, String group);
        void onFiltersReset();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ChipGroup chipGroupProgramme = view.findViewById(R.id.chipGroupProgramme);
        ChipGroup chipGroupLabGroup  = view.findViewById(R.id.chipGroupLabGroup);
        MaterialButton btnApply      = view.findViewById(R.id.btnApplyFilters);
        MaterialButton btnReset      = view.findViewById(R.id.btnResetFilters);

        btnApply.setOnClickListener(v -> {
            // Collect selected programme
            String programme = getSelectedChipText(chipGroupProgramme, view);
            // Collect selected group
            String group = getSelectedChipText(chipGroupLabGroup, view);

            // TODO: Send filters to ViewModel or parent activity
            dismiss();
        });

        btnReset.setOnClickListener(v -> {
            // Clear all chip selections
            chipGroupProgramme.clearCheck();
            chipGroupLabGroup.clearCheck();
            // TODO: Notify ViewModel to reset filters
            dismiss();
        });
    }

    private String getSelectedChipText(ChipGroup group, View root) {
        int selectedId = group.getCheckedChipId();
        if (selectedId == View.NO_ID) return "";
        Chip chip = root.findViewById(selectedId);
        return chip != null ? chip.getText().toString() : "";
    }
}
