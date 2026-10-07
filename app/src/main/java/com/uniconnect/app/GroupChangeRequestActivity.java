package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity alias for Group Change Request Screen (§13.7).
 * Directly opens the Transfer tab in the unified Student Portal.
 */
public class GroupChangeRequestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, StudentProfileActivity.class);
        intent.putExtra("TARGET_TAB", R.id.nav_student_transfer);
        startActivity(intent);
        finish();
    }
}
