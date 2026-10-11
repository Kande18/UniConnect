package com.uniconnect.app;

public enum SyncState {
    SAVED_LOCALLY("Saved locally"),
    PENDING("Pending"),
    SYNCING("Syncing"),
    SYNCED("Synced"),
    ACTION_REQUIRED("Action required");

    private final String displayLabel;

    SyncState(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }
}