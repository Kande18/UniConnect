package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

/**
 * Fragment displaying the Student's confirmed laboratory group details (§13.5).
 */
public class StudentGroupFragment extends Fragment {

    private MaterialButton btnNavToTransferTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_student_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnNavToTransferTab = view.findViewById(R.id.btnNavToTransferTab);
        if (btnNavToTransferTab != null) {
            btnNavToTransferTab.setOnClickListener(v -> {
                if (getActivity() instanceof StudentProfileActivity) {
                    ((StudentProfileActivity) getActivity()).selectTab(R.id.nav_student_transfer);
                }
            });
        }
    }
}
