package com.universal.deviceinfo;

/**
 * Pure string helpers for reasoning about Android storage paths, kept separate
 * from the android.* file I/O so the tricky derivation logic is unit-testable.
 */
public final class StoragePaths {

    /** Marker that separates a volume root from the per-app sandbox dir. */
    public static final String APP_DATA_MARKER = "/Android/data/";

    private StoragePaths() {
    }

    /**
     * Given an app-specific external dir such as
     * {@code /storage/1A2B-3C4D/Android/data/com.pkg/files}, returns the volume
     * root {@code /storage/1A2B-3C4D}. If the marker is absent the input is
     * returned unchanged.
     */
    public static String volumeRootFromAppFilesDir(String appFilesPath) {
        if (appFilesPath == null) {
            return null;
        }
        int idx = appFilesPath.indexOf(APP_DATA_MARKER);
        if (idx > 0) {
            return appFilesPath.substring(0, idx);
        }
        return appFilesPath;
    }

    /**
     * Heuristic: is this the PRIMARY (emulated / internal) shared storage rather
     * than a removable SD card or USB drive? The user explicitly wants the file
     * written to secondary/removable volumes, not the OS volume.
     */
    public static boolean isPrimaryEmulated(String path) {
        if (path == null) {
            return false;
        }
        return path.contains("/emulated/") || path.endsWith("/self");
    }

    /** Strips characters that are unsafe in a FAT/exFAT file name. */
    public static String sanitizeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "file";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
