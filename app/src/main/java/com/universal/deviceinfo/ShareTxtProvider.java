package com.universal.deviceinfo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Minimal FileProvider-equivalent (no AndroidX) that exposes the generated
 * report from the app cache as a {@code content://} URI, so it can be attached
 * to an email, uploaded to Drive, sent over WhatsApp, etc. via ACTION_SEND.
 */
public class ShareTxtProvider extends ContentProvider {

    public static final String AUTHORITY = "com.universal.deviceinfo.files";
    private static final String DIR = "share";

    public static File shareDir(Context c) {
        File d = new File(c.getCacheDir(), DIR);
        if (!d.exists()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
        return d;
    }

    public static Uri uriFor(String name) {
        return Uri.parse("content://" + AUTHORITY + "/" + name);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File fileFor(Uri uri) {
        String seg = uri.getLastPathSegment();
        if (seg == null) {
            return null;
        }
        // Strip any path component to keep access inside the share dir.
        String name = new File(seg).getName();
        return new File(shareDir(getContext()), name);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File f = fileFor(uri);
        if (f == null || !f.exists()) {
            return null;
        }
        String[] cols = (projection != null) ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) {
                row[i] = f.getName();
            } else if (OpenableColumns.SIZE.equals(cols[i])) {
                row[i] = f.length();
            } else {
                row[i] = null;
            }
        }
        MatrixCursor cur = new MatrixCursor(cols, 1);
        cur.addRow(row);
        return cur;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileFor(uri);
        if (f == null || !f.exists()) {
            throw new FileNotFoundException("No existe: " + uri);
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
