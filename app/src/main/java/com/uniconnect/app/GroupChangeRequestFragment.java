package com.uniconnect.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Fragment allowing students to request laboratory group changes (§13.7).
 * All text and colors now come from resources; the status views are live regions
 * (see layout) so TalkBack announces the "Pending" state automatically.
 */
public class GroupChangeRequestFragment extends Fragment {

    private MaterialCardView cardTransferStatus;
    private TextView tvLiveRequestStatus;
    private TextView tvLiveRequestDetail;
    private RadioGroup rgTargetGroup;
    private MaterialButton btnSubmitTransferRequest;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_fragment_group_change, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        cardTransferStatus = view.findViewById(R.id.cardTransferStatus);
        tvLiveRequestStatus = view.findViewById(R.id.tvLiveRequestStatus);
        tvLiveRequestDetail = view.findViewById(R.id.tvLiveRequestDetail);
        rgTargetGroup = view.findViewById(R.id.rgTargetGroup);
        btnSubmitTransferRequest = view.findViewById(R.id.btnSubmitTransferRequest);
    }

    private void setupListeners() {
        if (btnSubmitTransferRequest != null) {
            btnSubmitTransferRequest.setOnClickListener(v -> submitRequest());
        }
    }

    private void submitRequest() {
        int selectedId = rgTargetGroup.getCheckedRadioButtonId();
        String selectedGroup = getString(R.string.transfer_default_target);
        if (selectedId != -1) {
            RadioButton selectedRb = getView() != null ? getView().findViewById(selectedId) : null;
            if (selectedRb != null && selectedRb.getText() != null) {
                String label = selectedRb.getText().toString();
                // Keep only "Group G02" from "Group G02 — 10 / 15 Students (5 Available)"
                int cut = label.indexOf(" — ");
                selectedGroup = cut > 0 ? label.substring(0, cut) : label;
            }
        }

        // Update to Pending state (§13.7). Reason text typed by the student is left untouched.
        cardTransferStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.transfer_pending_bg));
        cardTransferStatus.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.transfer_pending_stroke));
        tvLiveRequestStatus.setText(R.string.transfer_status_pending);
        tvLiveRequestStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.transfer_pending_text));
        tvLiveRequestDetail.setText(getString(R.string.transfer_pending_detail, selectedGroup));

        Toast.makeText(requireContext(), R.string.transfer_submitted_toast, Toast.LENGTH_SHORT).show();
    }
}
