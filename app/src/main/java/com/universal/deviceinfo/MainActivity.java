package com.universal.deviceinfo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
 * Universal Device Info — light-themed, searchable, categorised device report.
 * Shows everything the collector can read, saves the full .txt into the app's
 * Android/data/.../faustimango_YT/UniversalDeviceInfo folder (and any USB/SD),
 * and can share the .txt to any app. Pure framework, no AndroidX.
 */
public class MainActivity extends Activity {

    private static final int REQ_PERM = 101;
    private static final String REPORT_TITLE = "UNIVERSAL DEVICE INFO";

    // Category name (chip label) followed by the section titles it includes.
    // "Básico" and "Todo" are handled specially.
    private static final String[][] CATS = {
        {"Básico"},
        {"Todo"},
        {"CPU", "Procesador (CPU)"},
        {"RAM", "Memoria RAM"},
        {"GPU", "GPU / OpenGL"},
        {"Red/WiFi", "Conectividad", "Interfaces de red", "Bluetooth", "Telefonía / SIM"},
        {"Pantalla", "Pantalla"},
        {"Batería", "Batería", "Térmico / Temperaturas"},
        {"Almac.", "Almacenamiento"},
        {"Sensores", "Sensores", "Cámaras", "Dispositivos de entrada", "Audio"},
        {"Sistema", "Identidad del dispositivo", "Sistema Android", "Características del sistema",
            "Propiedades del sistema", "Entorno de ejecución", "Idioma / Región / Hora",
            "Códecs multimedia", "Aplicaciones instaladas"},
    };

    private LinearLayout sectionsContainer;
    private LinearLayout chipsRow;
    private final List<TextView> chips = new ArrayList<TextView>();
    private EditText searchField;
    private TextView statusView;

    private DeviceInfoCollector collector;
    private StorageWriter writer;

    private List<InfoSection> allSections = new ArrayList<InfoSection>();
    private InfoSection saveSection;
    private volatile String lastReport;
    private int selectedCat = 0;
    private String query = "";
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiBuilder.BG);
        int h = UiBuilder.dp(this, 18);
        root.setPadding(h, UiBuilder.dp(this, 16), h, 0);

        // Title + subtitle
        root.addView(UiBuilder.title(this, "Universal Device Info"));
        root.addView(UiBuilder.subtitle(this, Build.MANUFACTURER + " " + Build.MODEL
                + "  ·  Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"));

        // Action buttons
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, UiBuilder.dp(this, 12), 0, UiBuilder.dp(this, 12));
        Button btnReload = UiBuilder.button(this, "↻ Actualizar", false);
        Button btnShare = UiBuilder.button(this, "🔗 Compartir TXT", true);
        LinearLayout.LayoutParams m = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        m.rightMargin = UiBuilder.dp(this, 10);
        actions.addView(btnReload, m);
        actions.addView(btnShare);
        root.addView(actions);
        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { reload(); }
        });
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shareTxt(); }
        });

        // Search field
        searchField = UiBuilder.searchField(this);
        root.addView(searchField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                render();
            }
        });

        // Category chips (horizontal scroll)
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.topMargin = UiBuilder.dp(this, 12);
        hsvLp.bottomMargin = UiBuilder.dp(this, 8);
        root.addView(hsv, hsvLp);
        chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        hsv.addView(chipsRow);
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            TextView chip = UiBuilder.chip(this, CATS[i][0]);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.rightMargin = UiBuilder.dp(this, 8);
            chip.setLayoutParams(cLp);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectCategory(idx); }
            });
            chips.add(chip);
            chipsRow.addView(chip);
        }
        UiBuilder.styleChip(chips.get(0), true);

        // Status line
        statusView = UiBuilder.status(this);
        statusView.setText("Cargando…");
        root.addView(statusView);

        // Scrollable content
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(scroll, scLp);
        sectionsContainer = new LinearLayout(this);
        sectionsContainer.setOrientation(LinearLayout.VERTICAL);
        sectionsContainer.setPadding(0, UiBuilder.dp(this, 6), 0, UiBuilder.dp(this, 16));
        scroll.addView(sectionsContainer);

        return root;
    }

    private void selectCategory(int idx) {
        selectedCat = idx;
        for (int i = 0; i < chips.size(); i++) {
            UiBuilder.styleChip(chips.get(i), i == idx);
        }
        if (searchField.getText().length() > 0) {
            searchField.setText(""); // watcher re-renders with the new category
        } else {
            render();
        }
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
        reload();
    }

    private void reload() {
        if (busy) {
            return;
        }
        busy = true;
        statusView.setText("Recopilando información del dispositivo…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<InfoSection> sections;
                try {
                    sections = collector.collectAll();
                } catch (Throwable t) {
                    sections = new ArrayList<InfoSection>();
                    InfoSection e = new InfoSection("Error");
                    e.add("Fallo", String.valueOf(t));
                    sections.add(e);
                }
                String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
                final String report = ReportBuilder.build(REPORT_TITLE, stamp, sections);
                lastReport = report;
                StorageWriter.Report wr;
                try {
                    wr = writer.writeEverywhere(report);
                } catch (Throwable t) {
                    wr = new StorageWriter.Report();
                    wr.lines.add("Error al guardar: " + t);
                }
                final List<InfoSection> fSections = sections;
                final StorageWriter.Report fWr = wr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        allSections = fSections;
                        saveSection = buildSaveSection(fWr);
                        render();
                        String p = fWr.primarySavedPath;
                        statusView.setText(p != null ? "✓ TXT guardado en: " + p
                                : "✗ No se pudo guardar el TXT.");
                        busy = false;
                    }
                });
            }
        }, "collector").start();
    }

    // -------------------------------------------------------------- Render

    private void render() {
        if (sectionsContainer == null) {
            return;
        }
        sectionsContainer.removeAllViews();

        List<InfoSection> base = new ArrayList<InfoSection>();
        if (saveSection != null) {
            base.add(saveSection);
        }
        base.addAll(allSections);

        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<InfoSection> visible;
        if (!q.isEmpty()) {
            visible = Filters.search(base, q);
        } else {
            String name = CATS[selectedCat][0];
            if ("Básico".equals(name)) {
                visible = buildBasico(allSections);
            } else if ("Todo".equals(name)) {
                visible = base;
            } else {
                visible = Filters.byTitles(base, CATS[selectedCat]);
            }
        }

        if (visible.isEmpty()) {
            TextView t = new TextView(this);
            t.setText(q.isEmpty() ? "Todavía cargando…" : "Sin resultados para \"" + query + "\".");
            t.setTextColor(UiBuilder.MUTED);
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
            t.setPadding(0, UiBuilder.dp(this, 20), 0, 0);
            sectionsContainer.addView(t);
        } else {
            for (InfoSection s : visible) {
                sectionsContainer.addView(UiBuilder.sectionCard(this, s), UiBuilder.cardLp(this));
            }
        }
    }

    private List<InfoSection> buildBasico(List<InfoSection> all) {
        List<InfoSection> out = new ArrayList<InfoSection>();
        InfoSection s = new InfoSection("Resumen básico");
        s.addIfPresent("Fabricante", Filters.value(all, "Identidad del dispositivo", "Fabricante"));
        s.addIfPresent("Modelo", Filters.value(all, "Identidad del dispositivo", "Modelo"));
        s.addIfPresent("Android", Filters.value(all, "Sistema Android", "Versión Android"));
        s.addIfPresent("CPU", Filters.value(all, "Procesador (CPU)", "Hardware (cpuinfo)"));
        s.addIfPresent("Núcleos", Filters.value(all, "Procesador (CPU)", "Núcleos (runtime)"));
        s.addIfPresent("Arquitectura", Filters.value(all, "Procesador (CPU)", "ABI principal"));
        s.addIfPresent("RAM total", Filters.value(all, "Memoria RAM", "RAM total"));
        s.addIfPresent("RAM disponible", Filters.value(all, "Memoria RAM", "RAM disponible"));
        String gpu = Filters.value(all, "GPU / OpenGL", "GL_RENDERER");
        if (gpu == null) {
            gpu = Filters.value(all, "GPU / OpenGL", "Versión OpenGL");
        }
        s.addIfPresent("GPU", gpu);
        String screen = Filters.value(all, "Pantalla", "Resolución (real)");
        if (screen == null) {
            screen = Filters.value(all, "Pantalla", "Resolución (usable)");
        }
        s.addIfPresent("Pantalla", screen);
        s.addIfPresent("Almacenamiento", Filters.value(all, "Almacenamiento", "Volúmenes detectados"));
        s.addIfPresent("Batería", Filters.value(all, "Batería", "Nivel"));
        String wifi = Filters.value(all, "Conectividad", "SSID");
        if (wifi == null) {
            wifi = Filters.value(all, "Conectividad", "WiFi habilitado");
        }
        s.addIfPresent("WiFi", wifi);
        String bt = Filters.value(all, "Bluetooth", "Nombre");
        if (bt == null) {
            bt = Filters.value(all, "Bluetooth", "Soportado");
        }
        s.addIfPresent("Bluetooth", bt);
        if (s.isEmpty()) {
            s.add("Info", "Todavía cargando…");
        }
        out.add(s);
        return out;
    }

    private InfoSection buildSaveSection(StorageWriter.Report wr) {
        InfoSection s = new InfoSection("Guardado del TXT");
        s.add("Archivo", StorageWriter.FILE_NAME);
        if (wr.primarySavedPath != null) {
            s.add("Guardado en", wr.primarySavedPath);
        }
        s.add("En Android/data", wr.appDataWritten);
        s.add("En disco secundario (USB/SD)", wr.secondaryDiskWritten);
        if (!wr.successPaths.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String p : wr.successPaths) {
                b.append(p).append('\n');
            }
            s.add("Todas las rutas", b.toString().trim());
        }
        s.add("Compartir", "usá el botón 'Compartir TXT' para enviarlo por correo, Drive, WhatsApp, etc.");
        return s;
    }

    // ----------------------------------------------------------------- Share

    private void shareTxt() {
        if (lastReport == null) {
            toast("Todavía se está cargando, esperá un momento…");
            return;
        }
        Uri uri = writer.writeShareFile(lastReport);
        if (uri == null) {
            toast("No se pudo preparar el TXT para compartir.");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, "Información del dispositivo - " + Build.MODEL);
            i.putExtra(Intent.EXTRA_TEXT, "Informe generado por Universal Device Info (faustimango_YT).");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Compartir TXT"));
        } catch (Throwable t) {
            toast("No hay apps para compartir: " + t);
        }
    }

    private void toast(String msg) {
        try {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } catch (Throwable ignore) {
        }
    }
}
