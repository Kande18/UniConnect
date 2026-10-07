package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Welcome screen Activity for UNI-CONNECT.
 *
 * Provides the main gateway to the application with portal selection
 * for students and lecturers.
 */
public class WelcomeActivity extends AppCompatActivity {

    private View btnStudentPortal;
    private View btnLecturerPortal;
    private View btnRegisterDirect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initViews();
        setupClickListeners();
    }

    /**
     * Initializes UI views by binding their IDs from activity_welcome.xml.
     */
    private void initViews() {
        btnStudentPortal = findViewById(R.id.btnStudentPortal);
        btnLecturerPortal = findViewById(R.id.btnLecturerPortal);
        btnRegisterDirect = findViewById(R.id.btnRegisterDirect);
    }

    /**
     * Sets click listeners on portal buttons for user navigation.
     */
    private void setupClickListeners() {
        if (btnStudentPortal != null) {
            btnStudentPortal.setOnClickListener(v -> navigateToStudentLogin());
        }

        if (btnLecturerPortal != null) {
            btnLecturerPortal.setOnClickListener(v -> navigateToLecturerLogin());
        }

        if (btnRegisterDirect != null) {
            btnRegisterDirect.setOnClickListener(v -> navigateToStudentRegister());
        }
    }

    /**
     * Navigates to StudentLoginActivity when Student Portal is selected.
     */
    private void navigateToStudentLogin() {
        Intent intent = new Intent(WelcomeActivity.this, StudentLoginActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to LecturerLoginActivity when Lecturer Portal is selected.
     */
    private void navigateToLecturerLogin() {
        Intent intent = new Intent(WelcomeActivity.this, LecturerLoginActivity.class);
        startActivity(intent);
    }

    /**
     * Navigates to StudentRegisterActivity when Direct Register is selected.
     */
    private void navigateToStudentRegister() {
        Intent intent = new Intent(WelcomeActivity.this, StudentRegisterActivity.class);
        startActivity(intent);
    }
}
