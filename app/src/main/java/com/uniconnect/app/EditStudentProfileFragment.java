package com.uniconnect.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Fragment allowing students to edit permitted profile information (§13.6).
 */
public class EditStudentProfileFragment extends Fragment {

    private MaterialCardView cardEditProfileStatus;
    private TextInputLayout tilEditFullName;
    private TextInputEditText etEditFullName;
    private TextInputLayout tilEditProgramme;
    private AutoCompleteTextView actvEditProgramme;
    private MaterialButton btnSaveProfileChanges;
    private MaterialButton btnCancelEditProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupProgrammeDropdown();
        setupListeners();
    }

    private void initViews(View view) {
        cardEditProfileStatus = view.findViewById(R.id.cardEditProfileStatus);
        tilEditFullName = view.findViewById(R.id.tilEditFullName);
        etEditFullName = view.findViewById(R.id.etEditFullName);
        tilEditProgramme = view.findViewById(R.id.tilEditProgramme);
        actvEditProgramme = view.findViewById(R.id.actvEditProgramme);
        btnSaveProfileChanges = view.findViewById(R.id.btnSaveProfileChanges);
        btnCancelEditProfile = view.findViewById(R.id.btnCancelEditProfile);
    }

    private void setupProgrammeDropdown() {
        String[] programmes = getResources().getStringArray(R.array.programme_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                programmes
        );
        actvEditProgramme.setAdapter(adapter);
    }

    private void setupListeners() {
        if (btnCancelEditProfile != null) {
            btnCancelEditProfile.setOnClickListener(v -> returnToProfileTab());
        }

        if (btnSaveProfileChanges != null) {
            btnSaveProfileChanges.setOnClickListener(v -> saveChanges());
        }
    }

    private void saveChanges() {
        tilEditFullName.setError(null);
        String name = etEditFullName.getText() != null ? etEditFullName.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name) || name.length() < 2 || name.length() > 100) {
            tilEditFullName.setError(getString(R.string.error_name_required));
            return;
        }

        cardEditProfileStatus.setVisibility(View.VISIBLE);
        btnSaveProfileChanges.postDelayed(this::returnToProfileTab, 900);
    }

    private void returnToProfileTab() {
        if (getActivity() instanceof StudentProfileActivity) {
            ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_profile);
        }
    }
}
