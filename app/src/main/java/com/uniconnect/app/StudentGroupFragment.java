package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/**
 * Fragment displaying the Student's confirmed laboratory group details (§13.5).
 * Dynamically binds group cohort from the authenticated user's session.
 */
public class StudentGroupFragment extends Fragment {

    private TextView tvStudentGroupName;
    private MaterialButton btnNavToTransferTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_student_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStudentGroupName = view.findViewById(R.id.tvStudentGroupName);
        btnNavToTransferTab = view.findViewById(R.id.btnNavToTransferTab);

        updateGroupDisplay();

        if (btnNavToTransferTab != null) {
            btnNavToTransferTab.setOnClickListener(v -> {
                if (getActivity() instanceof StudentProfileActivity) {
                    ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_transfer);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGroupDisplay();
    }

    private void updateGroupDisplay() {
        if (!isAdded()) return;
        UserManager userManager = UserManager.getInstance(requireContext());
        String group = userManager.getLabGroup();
        if (tvStudentGroupName != null) {
            tvStudentGroupName.setText("Laboratory Group " + group);
        }
    }
}
