package com.universal.deviceinfo;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enumerates every mounted storage volume using several strategies so it works
 * from Android 4.1 up to the newest releases:
 *
 * <ol>
 *   <li>{@code getExternalFilesDirs()} (API 19+) — always returns a writable
 *       app-specific dir on EVERY volume, primary and removable alike. This is
 *       the backbone and never requires a permission.</li>
 *   <li>{@link StorageManager#getStorageVolumes()} (API 24+) — enriches each
 *       entry with primary/removable/emulated flags, state and a label.</li>
 *   <li>The legacy {@code SECONDARY_STORAGE} environment variable — catches old
 *       devices (API 16-18) where {@code getExternalFilesDirs} is unavailable.</li>
 * </ol>
 */
public final class VolumeUtil {

    private VolumeUtil() {
    }

    public static List<VolumeInfo> list(Context ctx) {
        Map<String, VolumeInfo> byRoot = new LinkedHashMap<String, VolumeInfo>();

        // ---- 1. app-specific external dirs -> volume roots ----
        File[] appDirs;
        if (Build.VERSION.SDK_INT >= 19) {
            appDirs = ctx.getExternalFilesDirs(null);
        } else {
            appDirs = new File[]{ctx.getExternalFilesDir(null)};
        }
        if (appDirs != null) {
            for (int i = 0; i < appDirs.length; i++) {
                File d = appDirs[i];
                if (d == null) {
                    continue;
                }
                String appPath = d.getAbsolutePath();
                String root = StoragePaths.volumeRootFromAppFilesDir(appPath);
                VolumeInfo vi = new VolumeInfo();
                vi.root = root;
                vi.appDir = appPath;
                vi.primary = (i == 0) || StoragePaths.isPrimaryEmulated(root);
                vi.emulated = StoragePaths.isPrimaryEmulated(root);
                vi.removable = !vi.primary;
                vi.state = d.exists() ? "mounted" : "unknown";
                byRoot.put(root, vi);
            }
        }

        // ---- 2. enrich via StorageManager (API 24+) ----
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                StorageManager sm = (StorageManager) ctx.getSystemService(Context.STORAGE_SERVICE);
                if (sm != null) {
                    List<StorageVolume> vols = sm.getStorageVolumes();
                    for (StorageVolume sv : vols) {
                        String path = volumePath(sv);
                        VolumeInfo vi = (path != null) ? byRoot.get(path) : null;
                        if (vi == null) {
                            vi = new VolumeInfo();
                            vi.root = path;
                            if (path != null) {
                                byRoot.put(path, vi);
                            }
                        }
                        try { vi.primary = sv.isPrimary(); } catch (Throwable ignore) { }
                        try { vi.removable = sv.isRemovable(); } catch (Throwable ignore) { }
                        try { vi.emulated = sv.isEmulated(); } catch (Throwable ignore) { }
                        try { vi.state = sv.getState(); } catch (Throwable ignore) { }
                        try { vi.label = sv.getDescription(ctx); } catch (Throwable ignore) { }
                        try { vi.uuid = sv.getUuid(); } catch (Throwable ignore) { }
                    }
                }
            } catch (Throwable ignore) {
                // StorageManager quirks on some ROMs -> keep the app-dir data.
            }
        }

        // ---- 3. legacy SECONDARY_STORAGE (old devices) ----
        try {
            String secondary = System.getenv("SECONDARY_STORAGE");
            if (secondary != null && !secondary.isEmpty()) {
                for (String p : secondary.split(":")) {
                    if (p == null || p.trim().isEmpty()) {
                        continue;
                    }
                    String root = p.trim();
                    if (!byRoot.containsKey(root)) {
                        VolumeInfo vi = new VolumeInfo();
                        vi.root = root;
                        vi.primary = false;
                        vi.removable = true;
                        vi.emulated = false;
                        vi.state = new File(root).exists() ? "mounted" : "unknown";
                        byRoot.put(root, vi);
                    }
                }
            }
        } catch (Throwable ignore) {
        }

        return new ArrayList<VolumeInfo>(byRoot.values());
    }

    /** Volume mount path: getDirectory() on API 30+, reflection below that. */
    private static String volumePath(StorageVolume sv) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                File dir = sv.getDirectory();
                return dir != null ? dir.getAbsolutePath() : null;
            } catch (Throwable ignore) {
            }
        }
        // Hidden API on older versions: StorageVolume.getPath() / getPathFile().
        try {
            Method m = sv.getClass().getMethod("getPath");
            Object r = m.invoke(sv);
            if (r instanceof String) {
                return (String) r;
            }
        } catch (Throwable ignore) {
        }
        try {
            Method m = sv.getClass().getMethod("getPathFile");
            Object r = m.invoke(sv);
            if (r instanceof File) {
                return ((File) r).getAbsolutePath();
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** True if the primary shared storage is currently mounted & writable. */
    public static boolean primaryMounted() {
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Throwable ignore) {
            return false;
        }
    }
}
