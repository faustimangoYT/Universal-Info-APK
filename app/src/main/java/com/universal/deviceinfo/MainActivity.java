package com.universal.deviceinfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Single-screen TV app: gathers every piece of device info it can, shows it in a
 * focusable card layout, and writes a full .txt report to the root of any
 * secondary (USB/SD) volume it can reach. Pure framework, no AndroidX, so it
 * runs from Android 4.1 to the latest release.
 */
public class MainActivity extends Activity {

    private static final int REQ_PERM = 101;
    private static final int REQ_TREE = 202;
    private static final String PREFS = "udi_prefs";
    private static final String KEY_TREE = "saf_tree_uri";
    private static final String REPORT_TITLE = "UNIVERSAL DEVICE INFO";

    private LinearLayout sectionsContainer;
    private TextView statusView;
    private Button btnReload;

    private DeviceInfoCollector collector;
    private StorageWriter writer;

    private volatile String lastReport;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        collector = new DeviceInfoCollector(this);
        writer = new StorageWriter(this);
        setContentView(buildUi());
        ensurePermissionThenLoad();
    }

    // ------------------------------------------------------------------ UI

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiBuilder.BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int h = UiBuilder.dp(this, 28); // overscan-safe horizontal padding for TV
        int v = UiBuilder.dp(this, 24);
        root.setPadding(h, v, h, v);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(buildHeader());

        sectionsContainer = new LinearLayout(this);
        sectionsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(sectionsContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return scroll;
    }

    private View buildHeader() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = UiBuilder.dp(this, 18);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams cardLp = UiBuilder.cardLp(this);
        card.setLayoutParams(cardLp);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF101A31);
        bg.setCornerRadius(UiBuilder.dp(this, 16));
        bg.setStroke(Math.max(1, UiBuilder.dp(this, 1)), UiBuilder.STROKE);
        if (Build.VERSION.SDK_INT >= 16) {
            card.setBackground(bg);
        } else {
            //noinspection deprecation
            card.setBackgroundDrawable(bg);
        }

        card.addView(UiBuilder.title(this, "Universal Device Info"));
        card.addView(UiBuilder.subtitle(this,
                "Información completa del dispositivo · interfaz para TV"));

        TextView summary = new TextView(this);
        summary.setText(Build.MANUFACTURER + " " + Build.MODEL + "   •   Android "
                + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        summary.setTextColor(UiBuilder.ACCENT_2);
        summary.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
        summary.setPadding(0, UiBuilder.dp(this, 8), 0, 0);
        card.addView(summary);

        // Button row (equal weights so it fits any screen width).
        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, UiBuilder.dp(this, 14), 0, 0);

        btnReload = UiBuilder.button(this, "↻ Actualizar");
        Button btnSave = UiBuilder.button(this, "💾 Guardar TXT");
        Button btnSaf = UiBuilder.button(this, "📂 Carpeta USB/SD");

        btns.addView(btnReload, weightLp());
        btns.addView(btnSave, weightLp());
        btns.addView(btnSaf, weightLp());
        card.addView(btns);

        statusView = UiBuilder.status(this);
        statusView.setText("Iniciando…");
        card.addView(statusView);

        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { reload(); }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { resaveOnly(); }
        });
        btnSaf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { pickFolder(); }
        });

        btnReload.requestFocus();
        return card;
    }

    private LinearLayout.LayoutParams weightLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        int m = UiBuilder.dp(this, 5);
        lp.leftMargin = m;
        lp.rightMargin = m;
        return lp;
    }

    // -------------------------------------------------------------- Loading

    private void ensurePermissionThenLoad() {
        if (Build.VERSION.SDK_INT >= 23) {
            String w = android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (checkSelfPermission(w) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        w, android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM);
                return;
            }
        }
        reload();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Load regardless of the outcome: the writer adapts to whatever access
        // exists, and all the read-only info needs no permission at all.
        reload();
    }

    private void reload() {
        if (busy) {
            return;
        }
        busy = true;
        setStatus("Recopilando información del dispositivo…");
        sectionsContainer.removeAllViews();
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<InfoSection> sections;
                try {
                    sections = collector.collectAll();
                } catch (Throwable t) {
                    sections = new ArrayList<InfoSection>();
                    InfoSection err = new InfoSection("Error");
                    err.add("Fallo", String.valueOf(t));
                    sections.add(err);
                }
                String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                final String report = ReportBuilder.build(REPORT_TITLE, stamp, sections);
                lastReport = report;

                StorageWriter.Report wr;
                try {
                    wr = writer.writeEverywhere(report);
                } catch (Throwable t) {
                    wr = new StorageWriter.Report();
                    wr.lines.add("Error al escribir: " + t);
                }
                final String safResult = trySavedSaf(report);

                final List<InfoSection> finalSections = sections;
                final StorageWriter.Report finalWr = wr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        render(finalSections, finalWr, safResult);
                        toastSaved(finalWr);
                        busy = false;
                    }
                });
            }
        }, "collector").start();
    }

    private void resaveOnly() {
        if (lastReport == null) {
            toast("Todavía se está cargando, esperá un momento…");
            return;
        }
        setStatus("Guardando TXT de nuevo…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StorageWriter.Report wr = writer.writeEverywhere(lastReport);
                final String saf = trySavedSaf(lastReport);
                runOnUiThread(new Runnable() {
                    @Override public void run() { showWriteStatus(wr, saf); toastSaved(wr); }
                });
            }
        }, "resave").start();
    }

    // -------------------------------------------------------------- Render

    private void render(List<InfoSection> sections, StorageWriter.Report wr, String safResult) {
        sectionsContainer.removeAllViews();
        // First card: where the TXT went.
        sectionsContainer.addView(
                UiBuilder.sectionCard(this, buildSaveResultSection(wr, safResult)),
                UiBuilder.cardLp(this));
        for (InfoSection s : sections) {
            sectionsContainer.addView(UiBuilder.sectionCard(this, s), UiBuilder.cardLp(this));
        }
        showWriteStatus(wr, safResult);
    }

    private InfoSection buildSaveResultSection(StorageWriter.Report wr, String safResult) {
        InfoSection s = new InfoSection("¿Dónde se guardó el TXT?");
        if (wr.primarySavedPath != null) {
            s.add("Archivo guardado en", wr.primarySavedPath);
            int slash = wr.primarySavedPath.lastIndexOf('/');
            if (slash > 0) {
                s.add("Carpeta", wr.primarySavedPath.substring(0, slash));
            }
        } else {
            s.add("Archivo guardado en", "no se pudo guardar");
        }
        s.add("Nombre de archivo", StorageWriter.FILE_NAME);
        s.add("Disco secundario (USB/SD) detectado", wr.anySecondaryVolume);
        s.add("Guardado en el disco secundario", wr.secondaryDiskWritten);
        if (wr.onlyInternalFallback) {
            s.add("Solo copia interna (sin USB/SD)", true);
        }
        if (wr.successPaths.size() > 1) {
            StringBuilder b = new StringBuilder();
            for (String p : wr.successPaths) {
                b.append(p).append('\n');
            }
            s.add("Todas las rutas escritas", b.toString().trim());
        }
        if (!wr.lines.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String l : wr.lines) {
                b.append(l).append('\n');
            }
            s.add("Detalle", b.toString().trim());
        }
        if (safResult != null) {
            s.add("Carpeta elegida (SAF)", safResult);
        }
        return s;
    }

    private void showWriteStatus(StorageWriter.Report wr, String safResult) {
        StringBuilder sb = new StringBuilder();
        if (wr.secondaryDiskWritten && wr.primarySavedPath != null) {
            sb.append("✓ Guardado en el disco secundario:\n").append(wr.primarySavedPath);
        } else if (wr.onlyInternalFallback) {
            sb.append("⚠ No hay USB/SD conectado. Copia interna de emergencia");
            if (wr.primarySavedPath != null) {
                sb.append(":\n").append(wr.primarySavedPath);
            }
            sb.append("\nConectá el disco secundario y tocá Actualizar.");
        } else {
            sb.append("✗ No se pudo guardar en el disco secundario.");
        }
        if (safResult != null) {
            sb.append("\nSAF ✓: ").append(safResult);
        }
        setStatus(sb.toString());
    }

    /** Toast announcing exactly where the file landed, shown on open/refresh. */
    private void toastSaved(StorageWriter.Report wr) {
        if (wr.primarySavedPath != null) {
            toast("TXT guardado en:\n" + wr.primarySavedPath);
        } else {
            toast("No se pudo guardar el TXT. Conectá un USB/SD y tocá Actualizar.");
        }
    }

    private void setStatus(final String text) {
        if (statusView != null) {
            statusView.setText(text);
        }
    }

    // ----------------------------------------------------------------- SAF

    private void pickFolder() {
        if (Build.VERSION.SDK_INT < 21) {
            toast("Elegir carpeta (SAF) requiere Android 5.0+. En tu versión, la app "
                    + "ya intenta escribir directamente en la raíz.");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(i, REQ_TREE);
        } catch (Throwable t) {
            toast("No se pudo abrir el selector de carpetas: " + t);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_TREE && resultCode == RESULT_OK && data != null) {
            final Uri tree = data.getData();
            if (tree == null) {
                return;
            }
            try {
                final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                if (Build.VERSION.SDK_INT >= 19) {
                    getContentResolver().takePersistableUriPermission(tree, flags);
                }
            } catch (Throwable ignore) {
            }
            getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_TREE, tree.toString()).apply();
            setStatus("Guardando en la carpeta elegida…");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String report = lastReport != null ? lastReport
                            : ReportBuilder.build(REPORT_TITLE, new Date().toString(),
                            collector.collectAll());
                    final String res = StorageWriter.writeToDocumentTree(
                            MainActivity.this, tree, report);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (res != null) {
                                setStatus("✓ Guardado en la carpeta elegida (SAF).");
                                toast("TXT guardado en la carpeta USB/SD elegida.");
                            } else {
                                setStatus("✗ No se pudo escribir en la carpeta elegida.");
                            }
                        }
                    });
                }
            }, "saf-write").start();
        }
    }

    private String trySavedSaf(String report) {
        try {
            SharedPreferences p = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String s = p.getString(KEY_TREE, null);
            if (s == null) {
                return null;
            }
            return StorageWriter.writeToDocumentTree(this, Uri.parse(s), report);
        } catch (Throwable t) {
            return null;
        }
    }

    private void toast(String msg) {
        try {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } catch (Throwable ignore) {
        }
    }
}
