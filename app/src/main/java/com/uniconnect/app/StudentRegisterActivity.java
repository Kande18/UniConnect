package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Student Registration Activity (§13.2).
 *
 * Implements accessible form inputs, programme selection, error presentation,
 * input preservation on validation failure (§12), and smooth navigation.
 */
public class StudentRegisterActivity extends AppCompatActivity {

    private MaterialToolbar toolbarRegister;
    private MaterialCardView cardRegisterStatus;
    private TextView tvRegisterStatusMessage;

    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;

    private TextInputLayout tilStudentNumber;
    private TextInputEditText etStudentNumber;

    private TextInputLayout tilProgramme;
    private AutoCompleteTextView actvProgramme;

    private TextInputLayout tilClaimCode;
    private TextInputEditText etClaimCode;

    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;

    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;

    private MaterialButton btnSubmitRegister;
    private TextView btnNavToSignIn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        initViews();
        setupProgrammeDropdown();
        setupListeners();
    }

    private void initViews() {
        toolbarRegister = findViewById(R.id.toolbarRegister);
        cardRegisterStatus = findViewById(R.id.cardRegisterStatus);
        tvRegisterStatusMessage = findViewById(R.id.tvRegisterStatusMessage);

        tilFullName = findViewById(R.id.tilFullName);
        etFullName = findViewById(R.id.etFullName);

        tilStudentNumber = findViewById(R.id.tilStudentNumber);
        etStudentNumber = findViewById(R.id.etStudentNumber);

        tilProgramme = findViewById(R.id.tilProgramme);
        actvProgramme = findViewById(R.id.actvProgramme);

        tilClaimCode = findViewById(R.id.tilClaimCode);
        etClaimCode = findViewById(R.id.etClaimCode);

        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);

        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        btnNavToSignIn = findViewById(R.id.btnNavToSignIn);
    }

    private void setupProgrammeDropdown() {
        String[] programmes = getResources().getStringArray(R.array.programme_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                programmes
        );
        actvProgramme.setAdapter(adapter);
    }

    private void setupListeners() {
        if (toolbarRegister != null) {
            toolbarRegister.setNavigationOnClickListener(v -> finish());
        }

        if (btnNavToSignIn != null) {
            btnNavToSignIn.setOnClickListener(v -> {
                Intent intent = new Intent(StudentRegisterActivity.this, StudentLoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (btnSubmitRegister != null) {
            btnSubmitRegister.setOnClickListener(v -> validateAndSubmit());
        }
    }

    /**
     * Validates input fields without discarding previously typed data (§12).
     */
    private void validateAndSubmit() {
        // Clear previous error messages
        tilFullName.setError(null);
        tilStudentNumber.setError(null);
        tilProgramme.setError(null);
        tilClaimCode.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String studentNumber = etStudentNumber.getText() != null ? etStudentNumber.getText().toString().trim() : "";
        String programme = actvProgramme.getText() != null ? actvProgramme.getText().toString().trim() : "";
        String claimCode = etClaimCode.getText() != null ? etClaimCode.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        // 1. Name validation (2-100 characters)
        if (name.length() < 2 || name.length() > 100) {
            tilFullName.setError(getString(R.string.error_name_required));
            isValid = false;
        }

        // 2. Student number validation (exactly 9 digits)
        if (studentNumber.length() != 9 || !studentNumber.matches("\\d{9}")) {
            tilStudentNumber.setError(getString(R.string.error_student_number_invalid));
            isValid = false;
        }

        // 3. Programme validation
        if (TextUtils.isEmpty(programme)) {
            tilProgramme.setError(getString(R.string.error_programme_required));
            isValid = false;
        }

        // 4. Claim code validation
        if (TextUtils.isEmpty(claimCode)) {
            tilClaimCode.setError(getString(R.string.error_claim_code_required));
            isValid = false;
        }

        // 5. Password validation
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_length));
            isValid = false;
        }

        // 6. Confirm password validation
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        }

        // Note: Even if validation fails, typed data remains intact in EditTexts (§12)
        if (isValid) {
            // Show success confirmation
            cardRegisterStatus.setVisibility(View.VISIBLE);
            tvRegisterStatusMessage.setText(getString(R.string.registration_success_message));

            // Navigate to Student Login
            btnSubmitRegister.postDelayed(() -> {
                Intent intent = new Intent(StudentRegisterActivity.this, StudentLoginActivity.class);
                intent.putExtra("REGISTERED_STUDENT_NUMBER", studentNumber);
                startActivity(intent);
                finish();
            }, 800);
        }
    }
}
