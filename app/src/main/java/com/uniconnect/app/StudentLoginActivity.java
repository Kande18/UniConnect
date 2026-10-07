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
 * Student Login Activity (§13.3).
 *
 * Implements accessible login form, input preservation, validation presentation,
 * and navigation into the Student Profile / Portal.
 */
public class StudentLoginActivity extends AppCompatActivity {

    private MaterialToolbar toolbarStudentLogin;
    private TextInputLayout tilStudentNumber;
    private TextInputEditText etStudentNumber;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private MaterialButton btnSignIn;
    private TextView tvLinkRegister;
    private TextView tvLinkLecturerSignIn;
    private MaterialCardView cardLoginError;
    private TextView tvLoginErrorMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        initViews();
        handleIncomingData();
        setupListeners();
    }

    private void initViews() {
        toolbarStudentLogin = findViewById(R.id.toolbarStudentLogin);
        tilStudentNumber = findViewById(R.id.tilStudentNumber);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvLinkRegister = findViewById(R.id.tvLinkRegister);
        tvLinkLecturerSignIn = findViewById(R.id.tvLinkLecturerSignIn);
        cardLoginError = findViewById(R.id.cardLoginError);
        tvLoginErrorMessage = findViewById(R.id.tvLoginErrorMessage);
    }

    private void handleIncomingData() {
        if (getIntent() != null && getIntent().hasExtra("REGISTERED_STUDENT_NUMBER")) {
            String studentNum = getIntent().getStringExtra("REGISTERED_STUDENT_NUMBER");
            if (!TextUtils.isEmpty(studentNum) && etStudentNumber != null) {
                etStudentNumber.setText(studentNum);
                if (etPassword != null) {
                    etPassword.requestFocus();
                }
            }
        }
    }

    private void setupListeners() {
        if (toolbarStudentLogin != null) {
            toolbarStudentLogin.setNavigationOnClickListener(v -> finish());
        }

        if (tvLinkRegister != null) {
            tvLinkRegister.setOnClickListener(v -> {
                Intent intent = new Intent(StudentLoginActivity.this, StudentRegisterActivity.class);
                startActivity(intent);
            });
        }

        if (tvLinkLecturerSignIn != null) {
            tvLinkLecturerSignIn.setOnClickListener(v -> {
                Intent intent = new Intent(StudentLoginActivity.this, LecturerLoginActivity.class);
                startActivity(intent);
            });
        }

        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> attemptSignIn());
        }
    }

    /**
     * Validates student credentials on the client-side while preserving inputs (§12).
     */
    private void attemptSignIn() {
        tilStudentNumber.setError(null);
        tilPassword.setError(null);
        cardLoginError.setVisibility(View.GONE);

        String studentNumber = etStudentNumber.getText() != null ? etStudentNumber.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean isValid = true;

        if (studentNumber.length() != 9 || !studentNumber.matches("\\d{9}")) {
            tilStudentNumber.setError(getString(R.string.error_student_number_invalid));
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.hint_password));
            isValid = false;
        }

        if (isValid) {
            // Smoothly navigate to Student Profile / Home (Phase 3 landing destination)
            Intent intent = new Intent(StudentLoginActivity.this, StudentProfileActivity.class);
            intent.putExtra("LOGGED_IN_STUDENT_NUMBER", studentNumber);
            startActivity(intent);
        }
    }
}
