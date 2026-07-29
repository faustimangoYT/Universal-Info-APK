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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes the device-info report onto secondary (USB/SD) disks — the disk that
 * does NOT hold the operating system.
 *
 * <p>Instead of insisting on the exact filesystem root (which modern Android
 * blocks), it saves into the writable folder <b>closest to the root</b>: it
 * walks candidate directories from the root inward and picks the first one it
 * can actually write to, creating a {@code UniversalDeviceInfo} folder there.
 * On Android &le; 10 / rooted / permissive ROMs that ends up right next to the
 * root; on locked-down Android 11+ it lands in the app folder on that same
 * secondary disk. Either way the app reports the exact path on screen.
 */
public final class StorageWriter {

    /** Fixed name so each run REPLACES the previous file, as required. */
    public static final String FILE_NAME = "InformacionDispositivo.txt";
    /** Folder created on the secondary disk to hold the report. */
    public static final String APP_FOLDER = "UniversalDeviceInfo";

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
        public boolean onlyInternalFallback;  // no secondary -> emergency copy
        public boolean anySuccess;
        public String primarySavedPath;       // main path to show prominently
    }

    public Report writeEverywhere(String content) {
        Report r = new Report();

        List<VolumeInfo> vols = VolumeUtil.list(ctx);
        List<VolumeInfo> secondary = new ArrayList<VolumeInfo>();
        for (VolumeInfo v : vols) {
            // Target = any disk that is NOT the primary/OS storage.
            if (!v.primary || v.removable) {
                secondary.add(v);
            }
        }
        r.anySecondaryVolume = !secondary.isEmpty();

        if (!secondary.isEmpty()) {
            for (VolumeInfo v : secondary) {
                writeToSecondary(r, v, content);
            }
        } else {
            // No USB/SD present. Do NOT dump on the internal shared-storage root
            // (the OS disk); keep only an emergency app-scoped copy and prompt
            // the user to connect a secondary disk.
            r.onlyInternalFallback = true;
            File base = ctx.getExternalFilesDir(null);
            if (base == null) {
                base = ctx.getFilesDir();
            }
            File dest = new File(new File(base, APP_FOLDER), FILE_NAME);
            if (tryWrite(dest, content)) {
                r.anySuccess = true;
                r.successPaths.add(dest.getAbsolutePath());
                r.lines.add("⚠ No hay disco secundario (USB/SD) conectado.");
                r.lines.add("Copia de emergencia en el interno: " + dest.getAbsolutePath());
            }
            r.lines.add("➡ Conectá un pendrive o tarjeta SD y tocá 'Actualizar' "
                    + "para guardar en el disco secundario.");
        }

        if (!r.successPaths.isEmpty()) {
            r.primarySavedPath = r.successPaths.get(0);
        }
        return r;
    }

    /**
     * Writes to ONE secondary disk, choosing the writable folder CLOSEST to its
     * root. Never targets the bare root file; always uses a folder.
     */
    private void writeToSecondary(Report r, VolumeInfo v, String content) {
        String label = v.label != null ? v.label : "";
        for (File parent : candidateDirs(v)) {
            File folder = new File(parent, APP_FOLDER);
            File dest = new File(folder, FILE_NAME);
            if (tryWrite(dest, content)) {
                r.anySuccess = true;
                r.secondaryDiskWritten = true;
                r.successPaths.add(dest.getAbsolutePath());
                int level = depthBelowRoot(v.root, folder);
                r.lines.add("✓ DISCO SECUNDARIO " + label + " → " + dest.getAbsolutePath()
                        + "  (nivel " + level + " desde la raíz)");
                return; // shallowest writable folder found for this disk
            }
        }
        r.lines.add("✗ No se pudo escribir en el disco secundario " + label
                + " (" + v.root + ")");
    }

    /**
     * Candidate parent directories on a volume, ordered from CLOSEST-to-root to
     * deepest, so the first writable one wins.
     */
    private List<File> candidateDirs(VolumeInfo v) {
        List<File> out = new ArrayList<File>();
        Set<String> seen = new LinkedHashSet<String>();

        if (v.root != null) {
            addCand(out, seen, new File(v.root));               // root-level folder
            addCand(out, seen, new File(v.root, "Download"));   // common near-root dirs
            addCand(out, seen, new File(v.root, "Documents"));
        }
        if (v.appDir != null) {
            // Ancestor chain from the app dir up to (and including) the root.
            // These are always writable at the deepest level (getExternalFilesDir),
            // and progressively shallower toward the root.
            String rootPath = v.root != null ? new File(v.root).getAbsolutePath() : null;
            File a = new File(v.appDir);
            List<File> chain = new ArrayList<File>();
            int guard = 0;
            while (a != null && guard++ < 20) {
                chain.add(a);
                if (rootPath != null && a.getAbsolutePath().equals(rootPath)) {
                    break;
                }
                a = a.getParentFile();
            }
            for (int i = chain.size() - 1; i >= 0; i--) {
                addCand(out, seen, chain.get(i));
            }
        }

        // Order shallow (closest to root) first; stable within equal depth.
        Collections.sort(out, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return depthOf(a) - depthOf(b);
            }
        });
        return out;
    }

    private static void addCand(List<File> out, Set<String> seen, File f) {
        if (f == null) {
            return;
        }
        String p = f.getAbsolutePath();
        if (seen.add(p)) {
            out.add(f);
        }
    }

    private static int depthOf(File f) {
        int n = 0;
        String p = f.getAbsolutePath();
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) == '/') {
                n++;
            }
        }
        return n;
    }

    private static int depthBelowRoot(String root, File folder) {
        if (root == null) {
            return depthOf(folder);
        }
        int d = depthOf(folder) - depthOf(new File(root));
        return d < 0 ? 0 : d;
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

    // ==================== SAF (user-picked folder) ====================

    /**
     * Writes {@code content} as {@code FILE_NAME} into the SAF tree the user
     * granted via {@code ACTION_OPEN_DOCUMENT_TREE}, overwriting any existing
     * file of that name. Returns a human-readable destination or {@code null}.
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
