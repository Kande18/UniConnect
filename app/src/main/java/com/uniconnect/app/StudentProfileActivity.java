package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Student Portal & Profile Activity (§13.4, §13.5, §13.6, §13.7).
 *
 * Hosts a 4-button BottomNavigationView for instant switching between:
 * 1. Profile / Dashboard
 * 2. My Laboratory Group
 * 3. Edit Profile Information
 * 4. Group Transfer Request
 */
public class StudentProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavStudent;
    private View btnToolbarSignOut;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        initViews();
        setupBottomNavigation();
        setupSignOutListener();

        // Default to Profile fragment on launch or target specified by Intent
        if (savedInstanceState == null) {
            int targetTab = getIntent().getIntExtra("TARGET_TAB", R.id.nav_student_profile);
            selectTab(targetTab);
        }
    }

    private void initViews() {
        bottomNavStudent = findViewById(R.id.bottomNavStudent);
        btnToolbarSignOut = findViewById(R.id.btnToolbarSignOut);
    }

    private void setupBottomNavigation() {
        if (bottomNavStudent != null) {
            bottomNavStudent.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_student_profile) {
                    loadFragment(new StudentProfileFragment());
                    return true;
                } else if (itemId == R.id.nav_student_group) {
                    loadFragment(new StudentGroupFragment());
                    return true;
                } else if (itemId == R.id.nav_student_edit) {
                    loadFragment(new EditStudentProfileFragment());
                    return true;
                } else if (itemId == R.id.nav_student_transfer) {
                    loadFragment(new GroupChangeRequestFragment());
                    return true;
                }
                return false;
            });
        }
    }

    private void setupSignOutListener() {
        if (btnToolbarSignOut != null) {
            btnToolbarSignOut.setOnClickListener(v -> {
                // Clear active student session
                UserManager.getInstance(StudentProfileActivity.this).logout();

                // Return cleanly to WelcomeActivity (§21 Account Switching / Sign Out)
                Intent intent = new Intent(StudentProfileActivity.this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    /**
     * Programmatically switches the active tab in the BottomNavigationView.
     * Can be invoked from child fragments or external shortcuts.
     */
    public void selectTab(int menuItemId) {
        if (bottomNavStudent != null) {
            bottomNavStudent.setSelectedItemId(menuItemId);
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.fragmentContainerStudent, fragment);
        transaction.commit();
    }
}
