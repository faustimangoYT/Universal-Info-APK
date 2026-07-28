package com.universal.deviceinfo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the device-info report to storage, favouring the ROOT of every
 * secondary/removable volume (SD card, USB drive) exactly as requested, and
 * degrading gracefully across all Android versions:
 *
 * <ol>
 *   <li><b>Direct write</b> to the volume root — works on Android &le; 10 (legacy
 *       storage), on rooted / permissive ROMs (very common on TV boxes), and
 *       when All-Files access is granted on Android 11+.</li>
 *   <li><b>Root (su) write</b> to the true root — for rooted devices where the
 *       direct attempt was blocked by SELinux/scoped storage.</li>
 *   <li><b>App-specific dir</b> on the same secondary volume — ALWAYS writable,
 *       needs no permission; the file still lands on the secondary device.</li>
 * </ol>
 *
 * A separate SAF path ({@link #writeToDocumentTree}) covers the case where the
 * user picks the USB/SD folder by hand on a locked-down modern device.
 */
public final class StorageWriter {

    /** Fixed name so each run REPLACES the previous file, as required. */
    public static final String FILE_NAME = "InformacionDispositivo.txt";

    private final Context ctx;

    public StorageWriter(Context context) {
        this.ctx = context.getApplicationContext();
    }

    /** Result of an automatic write pass, ready to show in the UI. */
    public static final class Report {
        public final List<String> lines = new ArrayList<String>();
        public final List<String> successPaths = new ArrayList<String>();
        public boolean anySecondaryVolume;    // a USB/SD (secondary disk) exists
        public boolean secondaryDiskWritten;  // wrote at least one copy on it
        public boolean wroteToSecondaryRoot;  // wrote to its actual root
        public boolean onlyInternalFallback;  // no secondary -> emergency copy
        public boolean anySuccess;
        public boolean rootAvailable;
    }

    public Report writeEverywhere(String content) {
        Report r = new Report();
        r.rootAvailable = isRootAvailable();
        File tmp = writeTemp(content); // used for su copies

        List<VolumeInfo> vols = VolumeUtil.list(ctx);
        List<VolumeInfo> secondary = new ArrayList<VolumeInfo>();
        for (VolumeInfo v : vols) {
            // The target is any disk that is NOT the primary/OS storage:
            // a removable or otherwise secondary volume (USB / SD).
            if (!v.primary || v.removable) {
                secondary.add(v);
            }
        }
        r.anySecondaryVolume = !secondary.isEmpty();

        if (!secondary.isEmpty()) {
            // Requirement: the .txt must live on the disk that does NOT hold the
            // operating system. Write it to EVERY secondary/removable disk found.
            for (VolumeInfo v : secondary) {
                writeToSecondary(r, v, content, tmp);
            }
        } else {
            // No USB/SD present. Deliberately do NOT dump the file on the internal
            // shared-storage root (that is the OS disk). Keep only an emergency
            // app-scoped copy and tell the user to connect a secondary disk.
            r.onlyInternalFallback = true;
            File dir = ctx.getExternalFilesDir(null);
            if (dir == null) {
                dir = ctx.getFilesDir();
            }
            File dest = new File(dir, FILE_NAME);
            if (tryWrite(dest, content)) {
                r.anySuccess = true;
                r.successPaths.add(dest.getAbsolutePath());
                r.lines.add("⚠ No hay disco secundario (USB/SD) conectado.");
                r.lines.add("Copia de emergencia en el interno: " + dest.getAbsolutePath());
            }
            r.lines.add("➡ Conectá un pendrive o tarjeta SD y tocá 'Actualizar' "
                    + "para guardar en el disco secundario.");
        }

        if (tmp != null) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
        return r;
    }

    /**
     * Writes to ONE secondary disk, trying in order: direct root write -> su
     * (root) -> the app-specific dir on that same secondary disk (guaranteed).
     * Every success here lands on the secondary disk, never on the OS disk.
     */
    private void writeToSecondary(Report r, VolumeInfo v, String content, File tmp) {
        if (v == null) {
            return;
        }
        String tag = "DISCO SECUNDARIO"
                + (v.label != null ? " " + v.label : "")
                + " (" + v.root + ")";

        // 1) direct write to the volume ROOT
        if (v.root != null) {
            File dest = new File(v.root, FILE_NAME);
            if (tryWrite(dest, content)) {
                r.anySuccess = true;
                r.secondaryDiskWritten = true;
                r.wroteToSecondaryRoot = true;
                r.successPaths.add(dest.getAbsolutePath());
                r.lines.add("✓ RAÍZ  " + tag + "  →  " + dest.getAbsolutePath());
                writeAppDirCopy(r, v, content, false);
                return;
            }
        }

        // 2) root (su) write to the true ROOT
        if (r.rootAvailable && v.root != null && tmp != null) {
            String dest = v.root + "/" + FILE_NAME;
            if (writeWithSu(tmp, dest)) {
                r.anySuccess = true;
                r.secondaryDiskWritten = true;
                r.wroteToSecondaryRoot = true;
                r.successPaths.add(dest);
                r.lines.add("✓ RAÍZ (root) " + tag + "  →  " + dest);
                writeAppDirCopy(r, v, content, false);
                return;
            }
        }

        // 3) guaranteed app-specific dir ON THE SAME secondary disk
        if (writeAppDirCopy(r, v, content, true)) {
            r.secondaryDiskWritten = true; // still physically on the secondary disk
            r.lines.add("△ La RAÍZ no es escribible en este Android, pero se guardó "
                    + "en la carpeta de la app DEL MISMO disco secundario (ruta arriba). "
                    + "Para la raíz exacta usá el botón 'Carpeta USB/SD'.");
        } else {
            r.lines.add("✗ No se pudo escribir en " + tag);
        }
    }

    /** Writes into the app-specific dir on this volume; returns success. */
    private boolean writeAppDirCopy(Report r, VolumeInfo v, String content, boolean report) {
        if (v.appDir == null) {
            return false;
        }
        File dest = new File(v.appDir, FILE_NAME);
        boolean ok = tryWrite(dest, content);
        if (ok) {
            r.anySuccess = true;
            if (!r.successPaths.contains(dest.getAbsolutePath())) {
                r.successPaths.add(dest.getAbsolutePath());
            }
            if (report) {
                r.lines.add("✓ APP-DIR  →  " + dest.getAbsolutePath());
            }
        }
        return ok;
    }

    private boolean tryWrite(File dest, String content) {
        Writer w = null;
        try {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            w = new OutputStreamWriter(new FileOutputStream(dest, false), "UTF-8");
            w.write(content);
            w.flush();
            return dest.exists() && dest.length() > 0;
        } catch (Throwable t) {
            return false;
        } finally {
            closeSilently(w);
        }
    }

    private File writeTemp(String content) {
        Writer w = null;
        try {
            File tmp = new File(ctx.getCacheDir(), "info_tmp.txt");
            w = new OutputStreamWriter(new FileOutputStream(tmp, false), "UTF-8");
            w.write(content);
            w.flush();
            return tmp;
        } catch (Throwable t) {
            return null;
        } finally {
            closeSilently(w);
        }
    }

    private boolean isRootAvailable() {
        java.lang.Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            p.waitFor();
            return sb.toString().contains("uid=0");
        } catch (Throwable t) {
            return false;
        } finally {
            if (p != null) {
                try { p.destroy(); } catch (Throwable ignore) { }
            }
        }
    }

    private boolean writeWithSu(File tmp, String destPath) {
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{
                    "su", "-c", "cp -f '" + tmp.getAbsolutePath() + "' '" + destPath + "'"});
            int code = p.waitFor();
            if (code == 0) {
                // best effort make it world-readable on FAT this is a no-op
                try {
                    Runtime.getRuntime().exec(new String[]{
                            "su", "-c", "chmod 666 '" + destPath + "'"}).waitFor();
                } catch (Throwable ignore) {
                }
                return new File(destPath).exists();
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    // ==================== SAF (user-picked folder) ====================

    /**
     * Writes {@code content} as {@code FILE_NAME} into the SAF tree the user
     * granted via {@code ACTION_OPEN_DOCUMENT_TREE}. If a file with that name
     * already exists it is overwritten (truncated), so it is "replaced each
     * time" as required. Returns a human-readable destination or {@code null}.
     * API 21+.
     */
    public static String writeToDocumentTree(Context ctx, Uri treeUri, String content) {
        if (Build.VERSION.SDK_INT < 21 || treeUri == null) {
            return null;
        }
        try {
            ContentResolver cr = ctx.getContentResolver();
            String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri parentDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId);

            Uri existing = findChild(cr, treeUri, treeDocId, FILE_NAME);
            Uri target = existing;
            if (target == null) {
                target = DocumentsContract.createDocument(cr, parentDocUri, "text/plain", FILE_NAME);
            }
            if (target == null) {
                return null;
            }
            OutputStream os = cr.openOutputStream(target, "wt"); // truncate + write
            if (os == null) {
                return null;
            }
            try {
                os.write(content.getBytes("UTF-8"));
                os.flush();
            } finally {
                closeSilently(os);
            }
            return target.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static Uri findChild(ContentResolver cr, Uri treeUri, String parentDocId, String name) {
        Cursor c = null;
        try {
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId);
            c = cr.query(childrenUri, new String[]{
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String docId = c.getString(0);
                    String display = c.getString(1);
                    if (name.equals(display)) {
                        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
                    }
                }
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null) {
                try { c.close(); } catch (Throwable ignore) { }
            }
        }
        return null;
    }

    private static void closeSilently(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Throwable ignore) { }
        }
    }
}
