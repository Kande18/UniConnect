package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Student Registration Activity (§13.2).
 *
 * Implements accessible form inputs, Google Sign-Up pre-population (grayed-out read-only fields),
 * programme selection, error presentation, and account registration.
 */
public class StudentRegisterActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_UP = 9002;

    private MaterialToolbar toolbarRegister;
    private MaterialCardView cardRegisterStatus;
    private TextView tvRegisterStatusMessage;

    private MaterialButton btnGoogleRegister;

    private TextInputLayout tilFullName;
    private TextInputEditText etFullName;

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;

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

    private GoogleSignInClient googleSignInClient;
    private UserManager userManager;

    private boolean isGoogleLinked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_register);

        userManager = UserManager.getInstance(this);

        initViews();
        setupProgrammeDropdown();
        setupGoogleSignIn();
        handleIncomingGoogleData();
        setupListeners();
    }

    private void initViews() {
        toolbarRegister = findViewById(R.id.toolbarRegister);
        cardRegisterStatus = findViewById(R.id.cardRegisterStatus);
        tvRegisterStatusMessage = findViewById(R.id.tvRegisterStatusMessage);

        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);

        tilFullName = findViewById(R.id.tilFullName);
        etFullName = findViewById(R.id.etFullName);

        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);

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

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void handleIncomingGoogleData() {
        if (getIntent() != null) {
            String gName = getIntent().getStringExtra("GOOGLE_NAME");
            String gEmail = getIntent().getStringExtra("GOOGLE_EMAIL");
            if (!TextUtils.isEmpty(gName) || !TextUtils.isEmpty(gEmail)) {
                applyGoogleAccountData(gName, gEmail);
            }
        }
    }

    /**
     * Pre-populates Google name and email, then disables and grays them out as read-only.
     */
    private void applyGoogleAccountData(String name, String email) {
        isGoogleLinked = true;

        if (!TextUtils.isEmpty(name)) {
            etFullName.setText(name);
            etFullName.setEnabled(false);
            etFullName.setFocusable(false);
            tilFullName.setHelperText(getString(R.string.hint_google_linked));
            tilFullName.setAlpha(0.7f);
        }

        if (!TextUtils.isEmpty(email)) {
            etEmail.setText(email);
            etEmail.setEnabled(false);
            etEmail.setFocusable(false);
            tilEmail.setHelperText(getString(R.string.hint_google_linked));
            tilEmail.setAlpha(0.7f);
        }

        // Focus immediately on the required student number
        etStudentNumber.requestFocus();
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

        if (btnGoogleRegister != null) {
            btnGoogleRegister.setOnClickListener(v -> {
                if (googleSignInClient != null) {
                    googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                        Intent signInIntent = googleSignInClient.getSignInIntent();
                        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_UP);
                    });
                }
            });
        }

        if (btnSubmitRegister != null) {
            btnSubmitRegister.setOnClickListener(v -> validateAndSubmit());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_UP) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    applyGoogleAccountData(account.getDisplayName(), account.getEmail());
                    Toast.makeText(this, "Google account linked. Now enter your Student Number & Claim Code.", Toast.LENGTH_LONG).show();
                }
            } catch (ApiException e) {
                if (e.getStatusCode() != 12501) {
                    Toast.makeText(this, "Google Sign-Up failed (code " + e.getStatusCode() + "). You can register manually.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Validates input fields and saves student account without discarding user data.
     */
    private void validateAndSubmit() {
        // Clear previous error messages
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilStudentNumber.setError(null);
        tilProgramme.setError(null);
        tilClaimCode.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
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

        // Extract programme code if full name selected (e.g. "Computer Science (CS)" -> "CS")
        String progCode = "CS";
        if (programme.contains("IT") || programme.contains("Information Technology")) progCode = "IT";
        else if (programme.contains("DS") || programme.contains("Data Science")) progCode = "DS";

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

        if (isValid) {
            boolean registered = userManager.registerAccount(studentNumber, name, progCode, claimCode, password, email);
            if (!registered) {
                tilStudentNumber.setError(getString(R.string.error_student_number_duplicate));
                return;
            }

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
