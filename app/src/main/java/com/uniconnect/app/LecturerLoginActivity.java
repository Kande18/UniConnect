package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Lecturer Login Activity (§14.1).
 *
 * Implements accessible login form for academic staff, input validation presentation,
 * and navigation into the Lecturer Dashboard.
 */
public class LecturerLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbarLecturerLogin;
    private TextInputLayout tilLecturerId;
    private TextInputEditText etLecturerId;
    private TextInputLayout tilLecturerPassword;
    private TextInputEditText etLecturerPassword;
    private MaterialButton btnLecturerSignIn;
    private TextView tvSwitchToStudentPortal;
    private MaterialCardView cardLecturerError;
    private TextView tvLecturerErrorMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        toolbarLecturerLogin = findViewById(R.id.toolbarLecturerLogin);
        tilLecturerId = findViewById(R.id.tilLecturerId);
        etLecturerId = findViewById(R.id.etLecturerId);
        tilLecturerPassword = findViewById(R.id.tilLecturerPassword);
        etLecturerPassword = findViewById(R.id.etLecturerPassword);
        btnLecturerSignIn = findViewById(R.id.btnLecturerSignIn);
        tvSwitchToStudentPortal = findViewById(R.id.tvSwitchToStudentPortal);
        cardLecturerError = findViewById(R.id.cardLecturerError);
        tvLecturerErrorMessage = findViewById(R.id.tvLecturerErrorMessage);
    }

    private void setupListeners() {
        if (toolbarLecturerLogin != null) {
            toolbarLecturerLogin.setNavigationOnClickListener(v -> finish());
        }

        if (tvSwitchToStudentPortal != null) {
            tvSwitchToStudentPortal.setOnClickListener(v -> {
                Intent intent = new Intent(LecturerLoginActivity.this, StudentLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (btnLecturerSignIn != null) {
            btnLecturerSignIn.setOnClickListener(v -> attemptLecturerSignIn());
        }
    }

    /**
     * Validates lecturer credentials on the client-side while preserving inputs (§12).
     */
    private void attemptLecturerSignIn() {
        tilLecturerId.setError(null);
        tilLecturerPassword.setError(null);
        cardLecturerError.setVisibility(View.GONE);

        String lecturerId = etLecturerId.getText() != null ? etLecturerId.getText().toString().trim() : "";
        String password = etLecturerPassword.getText() != null ? etLecturerPassword.getText().toString() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(lecturerId)) {
            tilLecturerId.setError(getString(R.string.error_lecturer_id_required));
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilLecturerPassword.setError(getString(R.string.hint_password));
            isValid = false;
        }

        if (isValid) {
            // Smoothly navigate to Lecturer Dashboard (Phase 4 landing destination)
            Intent intent = new Intent(LecturerLoginActivity.this, LecturerDashboardActivity.class);
            intent.putExtra("LOGGED_IN_STAFF_ID", lecturerId);
            startActivity(intent);
        }
    }
}
