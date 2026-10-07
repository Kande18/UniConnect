package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * LecturerDashboardActivity  (Lecturer Portal Hub Activity)
 * ==========================================================
 * Hosts a 4-button BottomNavigationView for rapid switching between:
 *   1. Overview   (Analytics, Gauges, Quick Actions, Feed)
 *   2. Roster     (Searchable Student Directory, Filter Chips, Add FAB)
 *   3. Groups     (Lab Group Capacities, Schedules, Venues)
 *   4. Requests   (Student Transfer Approvals & Rejections)
 */
public class LecturerDashboardActivity extends AppCompatActivity {

    private MaterialToolbar toolbarLecturerPortal;
    private BottomNavigationView bottomNavLecturer;
    private View btnToolbarSignOut;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        initViews();
        setupToolbar();
        setupBottomNavigation();
        setupSignOut();

        if (savedInstanceState == null) {
            int targetTab = getIntent().getIntExtra("TARGET_TAB", R.id.nav_lecturer_overview);
            if (targetTab == R.id.nav_lecturer_overview) {
                loadFragment(new LecturerOverviewFragment());
                updateToolbarTitle("Lecturer Overview");
                if (bottomNavLecturer != null) {
                    bottomNavLecturer.setSelectedItemId(R.id.nav_lecturer_overview);
                }
            } else {
                selectTab(targetTab);
            }
        }
    }

    private void initViews() {
        toolbarLecturerPortal = findViewById(R.id.toolbarLecturerPortal);
        bottomNavLecturer     = findViewById(R.id.bottomNavLecturer);
        btnToolbarSignOut     = findViewById(R.id.btnToolbarSignOut);
    }

    private void setupToolbar() {
        if (toolbarLecturerPortal != null) {
            setSupportActionBar(toolbarLecturerPortal);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavLecturer != null) {
            bottomNavLecturer.setOnItemSelectedListener(item -> switchTabFragment(item.getItemId()));
        }
    }

    private boolean switchTabFragment(int itemId) {
        if (itemId == R.id.nav_lecturer_overview) {
            loadFragment(new LecturerOverviewFragment());
            updateToolbarTitle("Lecturer Overview");
            return true;
        } else if (itemId == R.id.nav_lecturer_roster) {
            loadFragment(new LecturerRosterFragment());
            updateToolbarTitle("Student Roster");
            return true;
        } else if (itemId == R.id.nav_lecturer_groups) {
            loadFragment(new LecturerGroupsFragment());
            updateToolbarTitle("Lab Groups");
            return true;
        } else if (itemId == R.id.nav_lecturer_requests) {
            loadFragment(new LecturerRequestsFragment());
            updateToolbarTitle("Transfer Requests");
            return true;
        }
        return false;
    }

    private void updateToolbarTitle(String title) {
        if (toolbarLecturerPortal != null) {
            toolbarLecturerPortal.setTitle(title);
        }
    }

    private void setupSignOut() {
        if (btnToolbarSignOut != null) {
            btnToolbarSignOut.setOnClickListener(v -> {
                Intent intent = new Intent(this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
        if (bottomNavLecturer != null) {
            if (bottomNavLecturer.getSelectedItemId() == menuItemId) {
                switchTabFragment(menuItemId);
            } else {
                bottomNavLecturer.setSelectedItemId(menuItemId);
            }
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.replace(R.id.fragmentContainerLecturer, fragment);
        transaction.commit();
    }
}
