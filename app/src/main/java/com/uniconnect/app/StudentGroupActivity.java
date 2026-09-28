package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity alias for Student Group Screen (§13.5).
 * Directly opens the Student Group tab in the unified Student Portal.
 */
public class StudentGroupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, StudentProfileActivity.class);
        intent.putExtra("TARGET_TAB", R.id.nav_student_group);
        startActivity(intent);
        finish();
    }
}
