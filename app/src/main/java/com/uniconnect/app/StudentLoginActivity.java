package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Student Login Screen Activity.
 */
public class StudentLoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        View linkRegister = findViewById(R.id.tvLinkRegister);
        if (linkRegister != null) {
            linkRegister.setOnClickListener(v -> {
                Intent intent = new Intent(StudentLoginActivity.this, StudentRegisterActivity.class);
                startActivity(intent);
            });
        }

        View linkLecturerSignIn = findViewById(R.id.tvLinkLecturerSignIn);
        if (linkLecturerSignIn != null) {
            linkLecturerSignIn.setOnClickListener(v -> {
                Intent intent = new Intent(StudentLoginActivity.this, LecturerLoginActivity.class);
                startActivity(intent);
            });
        }
    }
}
