package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * LecturerGroupsFragment  (Lecturer Portal Tab 3)
 * ================================================
 * Displays comprehensive laboratory group management:
 *   - Total capacity meter across all groups
 *   - Unassigned student pool banner with shortcut to roster
 *   - Detailed group cards (G01–G04) with schedules, venues, and occupancy
 */
public class LecturerGroupsFragment extends Fragment {

    private MaterialButton btnAssignUnassigned;
    private MaterialCardView cardGroupDetailG01;
    private MaterialCardView cardGroupDetailG02;
    private MaterialCardView cardGroupDetailG03;
    private MaterialCardView cardGroupDetailG04;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_lecturer_groups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupActions();
    }

    private void initViews(View root) {
        btnAssignUnassigned = root.findViewById(R.id.btnAssignUnassigned);
        cardGroupDetailG01  = root.findViewById(R.id.cardGroupDetailG01);
        cardGroupDetailG02  = root.findViewById(R.id.cardGroupDetailG02);
        cardGroupDetailG03  = root.findViewById(R.id.cardGroupDetailG03);
        cardGroupDetailG04  = root.findViewById(R.id.cardGroupDetailG04);
    }

    private void setupActions() {
        if (btnAssignUnassigned != null) {
            btnAssignUnassigned.setOnClickListener(v -> navigateToRoster());
        }

        View.OnClickListener rosterJump = v -> navigateToRoster();
        if (cardGroupDetailG01 != null) cardGroupDetailG01.setOnClickListener(rosterJump);
        if (cardGroupDetailG02 != null) cardGroupDetailG02.setOnClickListener(rosterJump);
        if (cardGroupDetailG03 != null) cardGroupDetailG03.setOnClickListener(rosterJump);
        if (cardGroupDetailG04 != null) cardGroupDetailG04.setOnClickListener(rosterJump);
    }

    private void navigateToRoster() {
        if (getActivity() instanceof LecturerDashboardActivity) {
            ((LecturerDashboardActivity) getActivity()).selectTab(R.id.nav_lecturer_roster);
        }
    }
}
