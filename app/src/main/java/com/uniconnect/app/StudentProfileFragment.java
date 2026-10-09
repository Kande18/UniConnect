package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/**
 * Fragment displaying the Student's Profile and Overview (§13.4).
 * Dynamically binds the authenticated student's real profile data.
 */
public class StudentProfileFragment extends Fragment {

    private TextView tvProfileStudentNameHeader;
    private TextView tvProfileStudentNumberHeader;
    private TextView tvDisplayFullName;
    private TextView tvDisplayProgramme;
    private TextView tvDisplayStudentNumber;
    private TextView btnRequestCorrection;
    private MaterialButton btnQuickViewGroup;
    private MaterialButton btnQuickEditProfile;
    private MaterialButton btnQuickRequestTransfer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_student_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        populateProfileData();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateProfileData();
    }

    private void initViews(View view) {
        tvProfileStudentNameHeader = view.findViewById(R.id.tvProfileStudentNameHeader);
        tvProfileStudentNumberHeader = view.findViewById(R.id.tvProfileStudentNumberHeader);
        tvDisplayFullName = view.findViewById(R.id.tvDisplayFullName);
        tvDisplayProgramme = view.findViewById(R.id.tvDisplayProgramme);
        tvDisplayStudentNumber = view.findViewById(R.id.tvDisplayStudentNumber);
        btnRequestCorrection = view.findViewById(R.id.btnRequestCorrection);
        btnQuickViewGroup = view.findViewById(R.id.btnQuickViewGroup);
        btnQuickEditProfile = view.findViewById(R.id.btnQuickEditProfile);
        btnQuickRequestTransfer = view.findViewById(R.id.btnQuickRequestTransfer);
    }

    private void populateProfileData() {
        if (!isAdded()) return;
        UserManager userManager = UserManager.getInstance(requireContext());
        String name = userManager.getFullName();
        String studentNumber = userManager.getStudentNumber();
        String progCode = userManager.getProgramme();

        String progFullName = "Computer Science (CS)";
        if ("IT".equalsIgnoreCase(progCode)) progFullName = "Information Technology (IT)";
        else if ("DS".equalsIgnoreCase(progCode)) progFullName = "Data Science (DS)";

        if (tvProfileStudentNameHeader != null) {
            tvProfileStudentNameHeader.setText(name);
        }
        if (tvProfileStudentNumberHeader != null) {
            tvProfileStudentNumberHeader.setText("Student ID: " + studentNumber);
        }
        if (tvDisplayFullName != null) {
            tvDisplayFullName.setText(name);
        }
        if (tvDisplayStudentNumber != null) {
            tvDisplayStudentNumber.setText(studentNumber);
        }
        if (tvDisplayProgramme != null) {
            tvDisplayProgramme.setText(progFullName);
        }
    }

    private void setupListeners() {
        if (btnRequestCorrection != null) {
            btnRequestCorrection.setOnClickListener(v -> {
                Toast.makeText(requireContext(), "Correction request forwarded to coordinator.", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnQuickViewGroup != null) {
            btnQuickViewGroup.setOnClickListener(v -> {
                if (getActivity() instanceof StudentProfileActivity) {
                    ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_group);
                }
            });
        }

        if (btnQuickEditProfile != null) {
            btnQuickEditProfile.setOnClickListener(v -> {
                if (getActivity() instanceof StudentProfileActivity) {
                    ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_edit);
                }
            });
        }

        if (btnQuickRequestTransfer != null) {
            btnQuickRequestTransfer.setOnClickListener(v -> {
                if (getActivity() instanceof StudentProfileActivity) {
                    ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_transfer);
                }
            });
        }
    }
}
