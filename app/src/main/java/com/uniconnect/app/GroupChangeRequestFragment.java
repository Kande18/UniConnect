package com.uniconnect.app;

import android.graphics.Color;
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
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Fragment allowing students to request laboratory group changes (§13.7).
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
        String selectedGroup = "Group G02";
        if (selectedId != -1) {
            RadioButton selectedRb = getView() != null ? getView().findViewById(selectedId) : null;
            if (selectedRb != null && selectedRb.getText() != null) {
                selectedGroup = selectedRb.getText().toString();
            }
        }

        // Update to Pending state (§13.7)
        cardTransferStatus.setCardBackgroundColor(Color.parseColor("#FEF3C7")); // soft amber
        cardTransferStatus.setStrokeColor(Color.parseColor("#F59E0B"));
        tvLiveRequestStatus.setText("Status: Pending Lecturer Review");
        tvLiveRequestStatus.setTextColor(Color.parseColor("#92400E"));
        tvLiveRequestDetail.setText("Transfer request for " + selectedGroup + " has been submitted. Awaiting lecturer confirmation.");

        Toast.makeText(requireContext(), "Transfer request submitted.", Toast.LENGTH_SHORT).show();
    }
}
