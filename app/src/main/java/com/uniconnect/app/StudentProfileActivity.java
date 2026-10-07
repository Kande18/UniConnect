package com.uniconnect.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
 *
 * Changes (usability / accessibility):
 *  - Tab fragments are created once and then hidden/shown, so typed input
 *    (e.g. the Edit Profile form or the transfer reason) is NOT lost when the
 *    student switches tabs.
 *  - Re-tapping the current tab does nothing (it no longer rebuilds the screen).
 *  - Predictable Back: any tab -> Profile tab -> exit.
 */
public class StudentProfileActivity extends AppCompatActivity {

    private static final String STATE_CURRENT_TAB = "state_current_tab";

    private static final int[] TAB_IDS = {
            R.id.nav_student_profile,
            R.id.nav_student_group,
            R.id.nav_student_edit,
            R.id.nav_student_transfer
    };

    private BottomNavigationView bottomNavStudent;
    private View btnToolbarSignOut;
    private int currentTabId = R.id.nav_student_profile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        initViews();
        setupBottomNavigation();
        setupSignOutListener();
        setupBackNavigation();

        if (savedInstanceState == null) {
            // Default to Profile fragment on launch or target specified by Intent
            int targetTab = getIntent().getIntExtra("TARGET_TAB", R.id.nav_student_profile);
            selectTab(targetTab);
        } else {
            // After rotation the FragmentManager restores the fragments (and which one is hidden).
            currentTabId = savedInstanceState.getInt(STATE_CURRENT_TAB, R.id.nav_student_profile);
            if (bottomNavStudent != null && bottomNavStudent.getMenu().findItem(currentTabId) != null) {
                bottomNavStudent.getMenu().findItem(currentTabId).setChecked(true);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_TAB, currentTabId);
    }

    private void initViews() {
        bottomNavStudent = findViewById(R.id.bottomNavStudent);
        btnToolbarSignOut = findViewById(R.id.btnToolbarSignOut);
    }

    private void setupBottomNavigation() {
        if (bottomNavStudent == null) return;

        bottomNavStudent.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (!isKnownTab(itemId)) return false;
            showTab(itemId);
            return true;
        });

        // Tapping the already-selected tab must not rebuild the screen (would clear typed input).
        bottomNavStudent.setOnItemReselectedListener(item -> { /* no-op */ });
    }

    /** Back: Edit / Group / Transfer -> Profile tab; Profile tab -> leave the portal. */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTabId != R.id.nav_student_profile) {
                    selectTab(R.id.nav_student_profile);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupSignOutListener() {
        if (btnToolbarSignOut != null) {
            btnToolbarSignOut.setOnClickListener(v -> {
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

    // ─── Fragment handling: create once, then hide/show ───────────────────
    private boolean isKnownTab(int id) {
        for (int tabId : TAB_IDS) {
            if (tabId == id) return true;
        }
        return false;
    }

    private String tagFor(int tabId) {
        return "student_tab_" + tabId;
    }

    private Fragment createFragment(int tabId) {
        if (tabId == R.id.nav_student_group) return new StudentGroupFragment();
        if (tabId == R.id.nav_student_edit) return new EditStudentProfileFragment();
        if (tabId == R.id.nav_student_transfer) return new GroupChangeRequestFragment();
        return new StudentProfileFragment();
    }

    private void showTab(int tabId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.setReorderingAllowed(true);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        for (int id : TAB_IDS) {
            Fragment existing = fm.findFragmentByTag(tagFor(id));
            if (existing != null && id != tabId) {
                transaction.hide(existing);
            }
        }

        Fragment target = fm.findFragmentByTag(tagFor(tabId));
        if (target == null) {
            transaction.add(R.id.fragmentContainerStudent, createFragment(tabId), tagFor(tabId));
        } else {
            transaction.show(target);
        }
        transaction.commit();
        currentTabId = tabId;
    }
}
