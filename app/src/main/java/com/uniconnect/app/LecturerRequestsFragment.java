package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

/**
 * LecturerRequestsFragment  (Lecturer Portal Tab 4)
 * ==================================================
 * Interactive review panel for Student Group Change Requests (§13.7 & §14.7).
 * Lecturers can approve or decline student transfers directly from the UI.
 */
public class LecturerRequestsFragment extends Fragment {

    private Chip chipPendingCount;
    private MaterialCardView bannerRequestFeedback;
    private TextView tvRequestFeedbackMsg;

    private View layoutActions1;
    private TextView tvStatusResult1;
    private MaterialButton btnApprove1;
    private MaterialButton btnDecline1;

    private View layoutActions2;
    private TextView tvStatusResult2;
    private MaterialButton btnApprove2;
    private MaterialButton btnDecline2;

    private LinearLayout layoutRequestsEmpty;

    private int pendingCount = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_lecturer_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupActions();
    }

    private void initViews(View root) {
        chipPendingCount       = root.findViewById(R.id.chipPendingCount);
        bannerRequestFeedback  = root.findViewById(R.id.bannerRequestFeedback);
        tvRequestFeedbackMsg   = root.findViewById(R.id.tvRequestFeedbackMsg);

        layoutActions1         = root.findViewById(R.id.layoutActions1);
        tvStatusResult1        = root.findViewById(R.id.tvStatusResult1);
        btnApprove1            = root.findViewById(R.id.btnApprove1);
        btnDecline1            = root.findViewById(R.id.btnDecline1);

        layoutActions2         = root.findViewById(R.id.layoutActions2);
        tvStatusResult2        = root.findViewById(R.id.tvStatusResult2);
        btnApprove2            = root.findViewById(R.id.btnApprove2);
        btnDecline2            = root.findViewById(R.id.btnDecline2);

        layoutRequestsEmpty    = root.findViewById(R.id.layoutRequestsEmpty);
    }

    private void setupActions() {
        btnApprove1.setOnClickListener(v -> {
            handleAction(layoutActions1, tvStatusResult1, true,
                    "Request approved! Peculiar Banda transferred to G02.");
        });

        btnDecline1.setOnClickListener(v -> {
            handleAction(layoutActions1, tvStatusResult1, false,
                    "Request declined for Peculiar Banda.");
        });

        btnApprove2.setOnClickListener(v -> {
            handleAction(layoutActions2, tvStatusResult2, true,
                    "Request approved! Tendai Moyo transferred to G03.");
        });

        btnDecline2.setOnClickListener(v -> {
            handleAction(layoutActions2, tvStatusResult2, false,
                    "Request declined for Tendai Moyo.");
        });
    }

    private void handleAction(View actionLayout, TextView resultView, boolean approved, String message) {
        actionLayout.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        if (approved) {
            resultView.setText("✓ Approved");
            resultView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_success));
        } else {
            resultView.setText("✕ Declined");
            resultView.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.status_error));
        }

        bannerRequestFeedback.setVisibility(View.VISIBLE);
        tvRequestFeedbackMsg.setText(message);

        pendingCount = Math.max(0, pendingCount - 1);
        chipPendingCount.setText(pendingCount + " Pending");

        if (pendingCount == 0) {
            layoutRequestsEmpty.setVisibility(View.VISIBLE);
        }
    }
}
