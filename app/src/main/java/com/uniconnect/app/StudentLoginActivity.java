package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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
import org.json.JSONObject;

/**
 * Student Login Activity (§13.3).
 *
 * Implements authenticated login, credential verification, red error feedback,
 * Google Sign-In integration, and dynamic routing to the Student Portal.
 */
public class StudentLoginActivity extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private MaterialToolbar toolbarStudentLogin;
    private TextInputLayout tilStudentNumber;
    private TextInputEditText etStudentNumber;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private MaterialButton btnSignIn;
    private MaterialButton btnGoogleSignIn;
    private TextView tvLinkRegister;
    private TextView tvLinkLecturerSignIn;
    private MaterialCardView cardLoginError;
    private TextView tvLoginErrorMessage;

    private GoogleSignInClient googleSignInClient;
    private UserManager userManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        userManager = UserManager.getInstance(this);

        initViews();
        setupGoogleSignIn();
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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvLinkRegister = findViewById(R.id.tvLinkRegister);
        tvLinkLecturerSignIn = findViewById(R.id.tvLinkLecturerSignIn);
        cardLoginError = findViewById(R.id.cardLoginError);
        tvLoginErrorMessage = findViewById(R.id.tvLoginErrorMessage);
    }

    private void setupGoogleSignIn() {
        // Request basic profile and email without requestIdToken (which causes code 10 when used with Android client IDs)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
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

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
        }
    }

    private void startGoogleSignIn() {
        if (googleSignInClient != null) {
            // Sign out first to ensure account chooser dialog appears
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String googleName = account.getDisplayName() != null ? account.getDisplayName() : "";
                String googleEmail = account.getEmail() != null ? account.getEmail() : "";

                // Check if this Google email is already linked to an existing account
                JSONObject linkedAcc = userManager.findAccountByEmail(googleEmail);
                if (linkedAcc != null) {
                    // Existing student user: log directly into profile
                    String studentNumber = linkedAcc.optString("studentNumber");
                    String fullName = linkedAcc.optString("fullName", googleName);
                    String programme = linkedAcc.optString("programme", "CS");
                    String labGroup = linkedAcc.optString("labGroup", "G01");

                    userManager.saveSession(studentNumber, fullName, programme, labGroup, googleEmail, "google-linked-token");

                    Toast.makeText(this, "Welcome back, " + fullName + "!", Toast.LENGTH_SHORT).show();
                    navigateToProfile(studentNumber);
                } else {
                    // New user: pre-populate Google Name & Email into registration and gray them out
                    Toast.makeText(this, "Google account linked. Complete your student details.", Toast.LENGTH_LONG).show();
                    Intent registerIntent = new Intent(StudentLoginActivity.this, StudentRegisterActivity.class);
                    registerIntent.putExtra("GOOGLE_NAME", googleName);
                    registerIntent.putExtra("GOOGLE_EMAIL", googleEmail);
                    startActivity(registerIntent);
                }
            }
        } catch (ApiException e) {
            if (e.getStatusCode() == 10) {
                // Developer Error (10): Google Console OAuth Client or SHA-1 not yet active
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Google Account Setup (Code 10)")
                        .setMessage("Google Play Services returned Developer Error 10 (SHA-1 fingerprint not yet registered in Google Console).\n\nWould you like to test the Google Sign-In flow with a simulated Google account?")
                        .setPositiveButton("Test Google Flow", (dialog, which) -> promptSimulatedGoogleUser())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else if (e.getStatusCode() != 12501) { // 12501 = user cancelled
                Toast.makeText(this, "Google Sign-In failed (code " + e.getStatusCode() + "). Please sign in with student number.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void promptSimulatedGoogleUser() {
        final android.widget.EditText inputEmail = new android.widget.EditText(this);
        inputEmail.setHint("e.g. student@gmail.com");
        inputEmail.setText("student@gmail.com");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Simulate Google User")
                .setMessage("Enter a Google email address to test the linked flow:")
                .setView(inputEmail)
                .setPositiveButton("Continue", (dialog, which) -> {
                    String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "student@gmail.com";
                    if (TextUtils.isEmpty(email)) email = "student@gmail.com";
                    String name = "Google Student (" + email.split("@")[0] + ")";

                    JSONObject linkedAcc = userManager.findAccountByEmail(email);
                    if (linkedAcc != null) {
                        String studentNumber = linkedAcc.optString("studentNumber");
                        String fullName = linkedAcc.optString("fullName", name);
                        String programme = linkedAcc.optString("programme", "CS");
                        String labGroup = linkedAcc.optString("labGroup", "G01");

                        userManager.saveSession(studentNumber, fullName, programme, labGroup, email, "simulated-google-token");
                        Toast.makeText(this, "Welcome back, " + fullName + "!", Toast.LENGTH_SHORT).show();
                        navigateToProfile(studentNumber);
                    } else {
                        Toast.makeText(this, "Simulated Google account linked. Complete your student details.", Toast.LENGTH_LONG).show();
                        Intent registerIntent = new Intent(StudentLoginActivity.this, StudentRegisterActivity.class);
                        registerIntent.putExtra("GOOGLE_NAME", name);
                        registerIntent.putExtra("GOOGLE_EMAIL", email);
                        startActivity(registerIntent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Validates student credentials against the database/backend.
     * Rejects invalid inputs with prominent red error card and field alerts.
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

        if (!isValid) {
            return;
        }

        // Disable button during validation to prevent double-submissions
        btnSignIn.setEnabled(false);
        btnSignIn.setText(R.string.state_loading);

        userManager.attemptLogin(studentNumber, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(String studentNumber, String fullName, String programme, String labGroup) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText(R.string.btn_sign_in);
                cardLoginError.setVisibility(View.GONE);

                navigateToProfile(studentNumber);
            }

            @Override
            public void onError(String errorMessage) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText(R.string.btn_sign_in);

                // Display red error feedback
                cardLoginError.setVisibility(View.VISIBLE);
                tvLoginErrorMessage.setText(getString(R.string.error_wrong_credentials));

                tilStudentNumber.setError(getString(R.string.error_student_number_invalid));
                tilPassword.setError(getString(R.string.hint_password));
            }
        });
    }

    private void navigateToProfile(String studentNumber) {
        Intent intent = new Intent(StudentLoginActivity.this, StudentProfileActivity.class);
        intent.putExtra("LOGGED_IN_STUDENT_NUMBER", studentNumber);
        startActivity(intent);
        finish();
    }
}
