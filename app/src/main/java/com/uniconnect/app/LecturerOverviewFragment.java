package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

/**
 * LecturerOverviewFragment  (Lecturer Portal Tab 1)
 * ==================================================
 * Provides the executive dashboard for the lecturer:
 *   - Overview stats: Total students, unassigned, capacity, pending requests
 *   - Live group occupancy indicators
 *   - Quick navigation shortcuts to Roster, Groups, and Requests tabs
 *   - Recent system activity feed
 */
public class LecturerOverviewFragment extends Fragment {

    private MaterialCardView cardOverviewG01;
    private MaterialCardView cardOverviewG02;
    private MaterialCardView cardOverviewG03;
    private MaterialCardView cardOverviewG04;

    private View btnQuickRoster;
    private View btnQuickGroups;
    private View btnQuickRequests;
    private View tvOverviewViewAllGroups;
    private View cardMetricRequests;
    private View cardMetricUnassigned;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_lecturer_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        populateGroupGauges(); // TODO: Connect with ViewModel
        setupNavigationShortcuts();
    }

    private void initViews(View root) {
        cardOverviewG01        = root.findViewById(R.id.cardOverviewG01);
        cardOverviewG02        = root.findViewById(R.id.cardOverviewG02);
        cardOverviewG03        = root.findViewById(R.id.cardOverviewG03);
        cardOverviewG04        = root.findViewById(R.id.cardOverviewG04);
        btnQuickRoster         = root.findViewById(R.id.btnQuickRoster);
        btnQuickGroups         = root.findViewById(R.id.btnQuickGroups);
        btnQuickRequests       = root.findViewById(R.id.btnQuickRequests);
        tvOverviewViewAllGroups= root.findViewById(R.id.tvOverviewViewAllGroups);
        cardMetricRequests     = root.findViewById(R.id.cardMetricRequests);
        cardMetricUnassigned   = root.findViewById(R.id.cardMetricUnassigned);
    }

    private void populateGroupGauges() {
        bindGroupCard(cardOverviewG01, "G01", 14, 15);
        bindGroupCard(cardOverviewG02, "G02", 10, 15);
        bindGroupCard(cardOverviewG03, "G03",  9, 15);
        bindGroupCard(cardOverviewG04, "G04", 15, 15);
    }

    private void bindGroupCard(MaterialCardView card, String label, int count, int capacity) {
        if (card == null) return;
        TextView tvLabel    = card.findViewById(R.id.tvGroupLabel);
        TextView tvCount    = card.findViewById(R.id.tvGroupCount);
        TextView tvCapacity = card.findViewById(R.id.tvGroupCapacity);
        ProgressBar progress = card.findViewById(R.id.progressGroup);

        if (tvLabel != null)    tvLabel.setText(label);
        if (tvCount != null)    tvCount.setText(String.valueOf(count));
        if (tvCapacity != null) tvCapacity.setText(getString(R.string.label_occupancy_of, capacity));
        if (progress != null && isAdded()) {
            int percent = capacity > 0 ? (count * 100) / capacity : 0;
            progress.setProgress(percent);
            if (count >= capacity) {
                progress.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_error)
                        )
                );
            }
        }
    }

    private void setupNavigationShortcuts() {
        if (btnQuickRoster != null) {
            btnQuickRoster.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_roster));
        }
        if (btnQuickGroups != null) {
            btnQuickGroups.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_groups));
        }
        if (tvOverviewViewAllGroups != null) {
            tvOverviewViewAllGroups.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_groups));
        }
        if (btnQuickRequests != null) {
            btnQuickRequests.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_requests));
        }
        if (cardMetricRequests != null) {
            cardMetricRequests.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_requests));
        }
        if (cardMetricUnassigned != null) {
            cardMetricUnassigned.setOnClickListener(v -> navigateToTab(R.id.nav_lecturer_roster));
        }
    }

    private void navigateToTab(int tabId) {
        if (getActivity() instanceof LecturerDashboardActivity) {
            ((LecturerDashboardActivity) getActivity()).selectTab(tabId);
        }
    }
}
