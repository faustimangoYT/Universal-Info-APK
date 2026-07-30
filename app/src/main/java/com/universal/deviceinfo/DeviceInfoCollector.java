package com.universal.deviceinfo;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.telephony.CellInfo;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.app.KeyguardManager;
import android.hardware.display.DisplayManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.media.MediaDrm;
import android.net.TrafficStats;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Display;
import android.view.InputDevice;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Collects as much device information as the platform will expose, from
 * Android 4.1 to the newest release. Every version-specific API is called
 * behind a {@link Build.VERSION#SDK_INT} guard, and every section is isolated
 * so a failure in one never blocks the others ("MÁS = MEJOR": partial data
 * beats no data).
 */
public final class DeviceInfoCollector {

    private final Context ctx;

    public DeviceInfoCollector(Context context) {
        this.ctx = context.getApplicationContext();
    }

    public List<InfoSection> collectAll() {
        List<InfoSection> out = new ArrayList<InfoSection>();
        add(out, "Identidad del dispositivo", 1);
        add(out, "Sistema Android", 2);
        add(out, "Procesador (CPU)", 3);
        add(out, "Memoria RAM", 4);
        add(out, "Almacenamiento", 5);
        add(out, "Pantalla", 6);
        add(out, "GPU / OpenGL", 7);
        add(out, "Batería", 8);
        add(out, "Cámaras", 9);
        add(out, "Sensores", 10);
        add(out, "Conectividad", 11);
        add(out, "Interfaces de red", 12);
        add(out, "Telefonía / SIM", 13);
        add(out, "Bluetooth", 14);
        add(out, "Audio", 15);
        add(out, "Dispositivos de entrada", 16);
        add(out, "Códecs multimedia", 17);
        add(out, "Características del sistema", 18);
        add(out, "Aplicaciones instaladas", 19);
        add(out, "Idioma / Región / Hora", 20);
        add(out, "Propiedades del sistema", 21);
        add(out, "Entorno de ejecución", 22);
        add(out, "Térmico / Temperaturas", 23);
        add(out, "Seguridad / Root", 24);
        add(out, "Energía (PowerManager)", 25);
        add(out, "DRM protegido (Widevine)", 26);
        add(out, "WiFi (capacidades)", 27);
        add(out, "Uso de datos (desde el arranque)", 28);
        add(out, "USB conectado", 29);
        add(out, "Ubicación (proveedores)", 30);
        add(out, "Ajustes del sistema", 31);
        add(out, "Particiones del sistema", 32);
        add(out, "Pantallas (DisplayManager)", 33);
        add(out, "Software del sistema", 34);
        add(out, "Kernel / procesos", 35);
        add(out, "Telefonía detallada", 36);
        add(out, "SIMs (SubscriptionManager)", 37);
        add(out, "Celdas (CellInfo)", 38);
        add(out, "Ubicación GPS", 39);
        add(out, "WiFi conectado (detalle)", 40);
        add(out, "Redes WiFi cercanas", 41);
        add(out, "Bluetooth emparejados", 42);
        add(out, "Cuentas (tipos disponibles)", 43);
        add(out, "Permisos", 44);
        return out;
    }

    /** Runs one section builder, catching everything so the app never dies. */
    private void add(List<InfoSection> out, String title, int which) {
        InfoSection s;
        try {
            s = build(which);
        } catch (Throwable t) {
            s = new InfoSection(title);
            s.add("Error", String.valueOf(t));
        }
        if (s != null && !s.isEmpty()) {
            out.add(s);
        }
    }

    private InfoSection build(int which) {
        switch (which) {
            case 1: return identity();
            case 2: return android();
            case 3: return cpu();
            case 4: return memory();
            case 5: return storage();
            case 6: return display();
            case 7: return gpu();
            case 8: return battery();
            case 9: return cameras();
            case 10: return sensors();
            case 11: return connectivity();
            case 12: return netInterfaces();
            case 13: return telephony();
            case 14: return bluetooth();
            case 15: return audio();
            case 16: return inputDevices();
            case 17: return codecs();
            case 18: return features();
            case 19: return apps();
            case 20: return localeTime();
            case 21: return sysProps();
            case 22: return runtimeEnv();
            case 23: return thermal();
            case 24: return security();
            case 25: return power();
            case 26: return drm();
            case 27: return wifiCaps();
            case 28: return traffic();
            case 29: return usb();
            case 30: return location();
            case 31: return systemSettings();
            case 32: return partitions();
            case 33: return displays();
            case 34: return software();
            case 35: return kernelProc();
            case 36: return telephonyDetail();
            case 37: return subscriptions();
            case 38: return cells();
            case 39: return gpsLocation();
            case 40: return wifiDetail();
            case 41: return wifiScan();
            case 42: return bluetoothPaired();
            case 43: return accountTypes();
            case 44: return permissions();
            default: return null;
        }
    }

    // ============================ 1. IDENTITY ============================
    private InfoSection identity() {
        InfoSection s = new InfoSection("Identidad del dispositivo");
        s.add("Fabricante", Formats.nn(Build.MANUFACTURER));
        s.add("Marca", Formats.nn(Build.BRAND));
        s.add("Modelo", Formats.nn(Build.MODEL));
        s.add("Nombre en Ajustes", Formats.nn(globalSetting("device_name")));
        s.add("Dispositivo (device)", Formats.nn(Build.DEVICE));
        s.add("Producto", Formats.nn(Build.PRODUCT));
        s.add("Placa (board)", Formats.nn(Build.BOARD));
        s.add("Hardware", Formats.nn(Build.HARDWARE));
        if (Build.VERSION.SDK_INT >= 31) {
            s.add("SoC fabricante", Formats.nn(Build.SOC_MANUFACTURER));
            s.add("SoC modelo", Formats.nn(Build.SOC_MODEL));
        }
        s.add("Industrial design (ID)", Formats.nn(Build.ID));
        s.add("Serial (Build.SERIAL)", Formats.nn(safeSerial()));
        s.add("Bootloader", Formats.nn(Build.BOOTLOADER));
        s.add("Banda base (radio)", Formats.nn(safeRadio()));
        s.add("Tags", Formats.nn(Build.TAGS));
        s.add("Tipo de build", Formats.nn(Build.TYPE));
        s.add("Host de compilación", Formats.nn(Build.HOST));
        s.add("Usuario de compilación", Formats.nn(Build.USER));
        s.add("Display build", Formats.nn(Build.DISPLAY));
        s.add("Fingerprint", Formats.nn(Build.FINGERPRINT));
        s.add("¿Parece emulador?", isProbablyEmulator());
        return s;
    }

    // ============================ 2. ANDROID ============================
    private InfoSection android() {
        InfoSection s = new InfoSection("Sistema Android");
        int sdk = Build.VERSION.SDK_INT;
        s.add("Versión Android", Formats.nn(Build.VERSION.RELEASE) + "  (" + versionName(sdk) + ")");
        s.add("Nivel de API (SDK_INT)", sdk);
        s.add("Nombre en clave", Formats.nn(Build.VERSION.CODENAME));
        s.add("Incremental", Formats.nn(Build.VERSION.INCREMENTAL));
        if (sdk >= 23) {
            s.add("Parche de seguridad", Formats.nn(Build.VERSION.SECURITY_PATCH));
            s.add("SO base", Formats.nn(Build.VERSION.BASE_OS));
            s.add("Preview SDK", Build.VERSION.PREVIEW_SDK_INT);
        }
        s.add("Kernel", Formats.nn(System.getProperty("os.version")));
        s.add("Fecha de build", new Date(Build.TIME).toString());
        s.add("VM", Formats.nn(System.getProperty("java.vm.name")) + " "
                + Formats.nn(System.getProperty("java.vm.version")));
        if (sdk >= 23) {
            s.add("Proceso de 64 bits", Process.is64Bit());
        }
        s.add("ABIs soportadas", abiList());
        s.add("Bootclasspath def.", Formats.nn(System.getenv("BOOTCLASSPATH") != null ? "definido" : null));
        return s;
    }

    // ============================ 3. CPU ============================
    private InfoSection cpu() {
        InfoSection s = new InfoSection("Procesador (CPU)");
        s.add("Núcleos (runtime)", Runtime.getRuntime().availableProcessors());
        s.add("Núcleos (sysfs)", countCpuCores());
        s.add("Arquitectura (os.arch)", Formats.nn(System.getProperty("os.arch")));
        s.add("ABI principal", Formats.nn(Build.VERSION.SDK_INT >= 21
                ? (Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : null)
                : legacyAbi()));
        s.add("ABIs 32-bit", Build.VERSION.SDK_INT >= 21 ? join(Build.SUPPORTED_32_BIT_ABIS) : "-");
        s.add("ABIs 64-bit", Build.VERSION.SDK_INT >= 21 ? join(Build.SUPPORTED_64_BIT_ABIS) : "-");

        String cpuinfo = readFile("/proc/cpuinfo");
        if (cpuinfo != null) {
            s.add("Hardware (cpuinfo)", Formats.nn(ProcParser.firstValueForKeys(
                    cpuinfo, "Hardware", "model name", "Processor")));
            List<String> models = ProcParser.distinctValuesForKey(cpuinfo, "model name");
            if (models.isEmpty()) {
                models = ProcParser.distinctValuesForKey(cpuinfo, "Processor");
            }
            if (!models.isEmpty()) {
                s.add("Modelo(s) de núcleo", join(models));
            }
            s.addIfPresent("Implementador CPU", ProcParser.firstValueForKeys(cpuinfo, "CPU implementer"));
            s.addIfPresent("Arquitectura CPU", ProcParser.firstValueForKeys(cpuinfo, "CPU architecture"));
            s.addIfPresent("Variante/Parte", ProcParser.firstValueForKeys(cpuinfo, "CPU variant")
                    + " / " + ProcParser.firstValueForKeys(cpuinfo, "CPU part"));
            s.addIfPresent("Revisión", ProcParser.firstValueForKeys(cpuinfo, "CPU revision", "Revision"));
            s.addIfPresent("Features/Flags", ProcParser.firstValueForKeys(cpuinfo, "Features", "flags"));
            s.addIfPresent("BogoMIPS", ProcParser.firstValueForKeys(cpuinfo, "BogoMIPS"));
        }
        s.add("Frecuencia mín/máx", cpuFreqRange());
        s.add("Governor actual", Formats.nn(trim(readFile(
                "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"))));
        s.addIfPresent("Frecuencias por núcleo", perCoreFreqs());
        return s;
    }

    // ============================ 23. THERMAL ============================
    private InfoSection thermal() {
        InfoSection s = new InfoSection("Térmico / Temperaturas");
        int n = 0;
        try {
            File[] zones = new File("/sys/class/thermal/").listFiles();
            if (zones != null) {
                for (File z : zones) {
                    if (!z.getName().startsWith("thermal_zone")) {
                        continue;
                    }
                    String temp = trim(readFile(z.getAbsolutePath() + "/temp"));
                    if (temp == null) {
                        continue;
                    }
                    long t = parseLong(temp);
                    if (t < 0) {
                        continue;
                    }
                    String type = trim(readFile(z.getAbsolutePath() + "/type"));
                    // Values are usually in milli-Celsius.
                    String tc = (t > 1000)
                            ? String.format(Locale.US, "%.1f °C", t / 1000.0)
                            : (t + " (crudo)");
                    s.add(z.getName() + (type != null ? " (" + type + ")" : ""), tc);
                    n++;
                }
            }
        } catch (Throwable t) {
            s.add("Térmico", "no disponible (" + t + ")");
        }
        if (n == 0 && s.isEmpty()) {
            s.add("Sensores térmicos", "no accesibles en este dispositivo");
        }
        return s;
    }

    private String perCoreFreqs() {
        int cores = countCpuCores();
        if (cores <= 0) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < cores; i++) {
            String base = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/";
            String mx = trim(readFile(base + "cpuinfo_max_freq"));
            String cur = trim(readFile(base + "scaling_cur_freq"));
            if (mx == null && cur == null) {
                continue;
            }
            b.append("cpu").append(i).append(": ");
            if (cur != null) {
                b.append(Formats.khzToHuman(parseLong(cur))).append(" / ");
            }
            b.append("máx ").append(mx != null ? Formats.khzToHuman(parseLong(mx)) : "?").append('\n');
        }
        return b.length() == 0 ? null : b.toString().trim();
    }

    // ============================ 4. MEMORY ============================
    private InfoSection memory() {
        InfoSection s = new InfoSection("Memoria RAM");
        // Read meminfo first so it can back-fill total RAM on API < 16, where
        // MemoryInfo.totalMem does not exist yet.
        String meminfo = readFile("/proc/meminfo");
        long memTotalKb = meminfo != null ? ProcParser.meminfoKb(meminfo, "MemTotal") : -1;
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long total = (Build.VERSION.SDK_INT >= 16) ? mi.totalMem
                    : (memTotalKb > 0 ? memTotalKb * 1024L : -1);
            s.add("RAM total", Formats.humanBytesDetailed(total));
            s.add("RAM disponible", Formats.humanBytesDetailed(mi.availMem)
                    + "  (" + (total > 0 ? Formats.percent(mi.availMem, total) : "?") + ")");
            s.add("Umbral memoria baja", Formats.humanBytes(mi.threshold));
            s.add("¿Memoria baja ahora?", mi.lowMemory);
            s.add("Clase de memoria app", am.getMemoryClass() + " MB");
            s.add("Clase memoria grande", am.getLargeMemoryClass() + " MB");
            if (Build.VERSION.SDK_INT >= 19) {
                s.add("Dispositivo low-RAM", am.isLowRamDevice());
            }
        } catch (Throwable t) {
            s.add("ActivityManager", "no disponible (" + t + ")");
        }
        if (meminfo != null) {
            s.add("MemTotal", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "MemTotal")));
            s.add("MemFree", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "MemFree")));
            s.add("MemAvailable", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "MemAvailable")));
            s.add("Buffers", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "Buffers")));
            s.add("Cached", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "Cached")));
            s.add("SwapTotal", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "SwapTotal")));
            s.add("SwapFree", Formats.kbToHuman(ProcParser.meminfoKb(meminfo, "SwapFree")));
        }
        Runtime r = Runtime.getRuntime();
        s.add("Heap máx (app)", Formats.humanBytes(r.maxMemory()));
        s.add("Heap total (app)", Formats.humanBytes(r.totalMemory()));
        s.add("Heap libre (app)", Formats.humanBytes(r.freeMemory()));
        s.add("Native heap total", Formats.humanBytes(Debug.getNativeHeapSize()));
        s.add("Native heap usado", Formats.humanBytes(Debug.getNativeHeapAllocatedSize()));
        return s;
    }

    // ============================ 5. STORAGE ============================
    private InfoSection storage() {
        InfoSection s = new InfoSection("Almacenamiento");
        List<VolumeInfo> vols = VolumeUtil.list(ctx);
        s.add("Volúmenes detectados", vols.size());
        int idx = 0;
        for (VolumeInfo v : vols) {
            idx++;
            String head = "Volumen " + idx + (v.primary ? " (PRINCIPAL)" : " (SECUNDARIO)");
            StringBuilder b = new StringBuilder();
            b.append("Ruta: ").append(Formats.nn(v.root)).append('\n');
            if (v.label != null) b.append("Etiqueta: ").append(v.label).append('\n');
            b.append("Tipo: ")
                    .append(v.primary ? "principal" : "secundario")
                    .append(v.removable ? ", extraíble" : "")
                    .append(v.emulated ? ", emulado" : "").append('\n');
            b.append("Estado: ").append(Formats.nn(v.state)).append('\n');
            long[] sz = statFs(v.root != null ? v.root : v.appDir);
            if (sz != null) {
                b.append("Total: ").append(Formats.humanBytesDetailed(sz[0])).append('\n');
                b.append("Libre: ").append(Formats.humanBytesDetailed(sz[1]))
                        .append(" (").append(Formats.percent(sz[1], sz[0])).append(")\n");
                b.append("Usado: ").append(Formats.humanBytesDetailed(sz[0] - sz[1]));
            } else {
                b.append("Tamaño: no accesible directamente");
            }
            s.add(head, b.toString());
        }
        s.add("Data dir", Formats.nn(safePath(Environment.getDataDirectory())));
        s.add("Root dir", Formats.nn(safePath(Environment.getRootDirectory())));
        s.add("Cache interna", Formats.nn(safePath(ctx.getCacheDir())));
        return s;
    }

    // ============================ 6. DISPLAY ============================
    private InfoSection display() {
        InfoSection s = new InfoSection("Pantalla");
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        s.add("Resolución (usable)", dm.widthPixels + " x " + dm.heightPixels + " px");
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            Display d = wm.getDefaultDisplay();
            if (Build.VERSION.SDK_INT >= 17) {
                DisplayMetrics real = new DisplayMetrics();
                d.getRealMetrics(real);
                s.add("Resolución (real)", real.widthPixels + " x " + real.heightPixels + " px");
                s.add("Nombre del display", Formats.nn(d.getName()));
            }
            s.add("Tasa de refresco", String.format(Locale.US, "%.1f Hz", d.getRefreshRate()));
            s.add("Rotación", d.getRotation() * 90 + "°");
            if (Build.VERSION.SDK_INT >= 23) {
                Display.Mode[] modes = d.getSupportedModes();
                StringBuilder mb = new StringBuilder();
                for (Display.Mode m : modes) {
                    mb.append(m.getPhysicalWidth()).append("x").append(m.getPhysicalHeight())
                            .append("@").append(String.format(Locale.US, "%.0f", m.getRefreshRate()))
                            .append("Hz\n");
                }
                if (mb.length() > 0) s.add("Modos soportados", mb.toString().trim());
            }
            if (Build.VERSION.SDK_INT >= 24) {
                Display.HdrCapabilities hdr = d.getHdrCapabilities();
                if (hdr != null) {
                    s.add("Tipos HDR", join(hdrTypes(hdr.getSupportedHdrTypes())));
                }
            }
        } catch (Throwable ignore) {
        }
        s.add("Densidad (dpi)", dm.densityDpi + " dpi");
        s.add("Densidad (factor)", String.format(Locale.US, "%.2f", dm.density));
        s.add("Bucket de densidad", densityBucket(dm.densityDpi));
        s.add("xdpi / ydpi", String.format(Locale.US, "%.1f / %.1f", dm.xdpi, dm.ydpi));
        s.add("Escala de fuente", String.format(Locale.US, "%.2f", dm.scaledDensity));
        double inches = Math.sqrt(Math.pow(dm.widthPixels / dm.xdpi, 2)
                + Math.pow(dm.heightPixels / dm.ydpi, 2));
        s.add("Tamaño físico aprox.", String.format(Locale.US, "%.2f pulgadas", inches));
        Configuration cfg = ctx.getResources().getConfiguration();
        s.add("Orientación", cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
                ? "horizontal" : "vertical");
        if (Build.VERSION.SDK_INT >= 13) {
            s.add("Ancho x Alto (dp)", cfg.screenWidthDp + " x " + cfg.screenHeightDp + " dp");
            s.add("Ancho mínimo (dp)", cfg.smallestScreenWidthDp + " dp");
        }
        s.add("Categoría de pantalla", screenSizeName(cfg));
        s.add("¿Modo TV (UI)?", (cfg.uiMode & Configuration.UI_MODE_TYPE_MASK)
                == Configuration.UI_MODE_TYPE_TELEVISION);
        return s;
    }

    // ============================ 7. GPU ============================
    private InfoSection gpu() {
        InfoSection s = new InfoSection("GPU / OpenGL");
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo ci = am.getDeviceConfigurationInfo();
            s.add("Versión OpenGL ES (req.)", Formats.nn(ci.getGlEsVersion()));
        } catch (Throwable ignore) {
        }
        if (Build.VERSION.SDK_INT >= 17) {
            String[] gl = queryGlStrings();
            if (gl != null) {
                s.add("GL_RENDERER", Formats.nn(gl[0]));
                s.add("GL_VENDOR", Formats.nn(gl[1]));
                s.add("GL_VERSION", Formats.nn(gl[2]));
                s.add("GLSL", Formats.nn(gl[3]));
                if (gl[4] != null) {
                    String[] ext = gl[4].trim().split("\\s+");
                    s.add("Nº extensiones GL", ext.length);
                    s.add("Extensiones GL", join(ext));
                }
            } else {
                s.add("Detalle GPU", "no se pudo crear contexto EGL");
            }
        }
        return s;
    }

    // ============================ 8. BATTERY ============================
    private InfoSection battery() {
        InfoSection s = new InfoSection("Batería");
        Intent i = ctx.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i != null) {
            boolean present = i.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
            s.add("Presente", present);
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                s.add("Nivel", Formats.percent(level, scale) + "  (" + level + "/" + scale + ")");
            }
            s.add("Estado", batteryStatus(i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)));
            s.add("Salud", batteryHealth(i.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)));
            s.add("Conectado a", batteryPlugged(i.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)));
            s.add("Tecnología", Formats.nn(i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)));
            int volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (volt > 0) s.add("Voltaje", Formats.milliVolts(volt));
            int temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1000);
            if (temp > -1000) s.add("Temperatura", Formats.deciCelsius(temp));
        }
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
                int cap = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (cap >= 0) s.add("Capacidad (BM)", cap + " %");
                long chg = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                if (chg > 0) s.add("Carga", chg / 1000 + " mAh");
                int cur = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                s.add("Corriente ahora", cur + " µA");
                int avg = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                s.add("Corriente media", avg + " µA");
                if (Build.VERSION.SDK_INT >= 23) {
                    s.add("¿Cargando?", bm.isCharging());
                }
            } catch (Throwable ignore) {
            }
        }
        return s;
    }

    // ============================ 9. CAMERAS ============================
    private InfoSection cameras() {
        InfoSection s = new InfoSection("Cámaras");
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
                String[] ids = cm.getCameraIdList();
                s.add("Número de cámaras", ids.length);
                for (String id : ids) {
                    CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                    StringBuilder b = new StringBuilder();
                    Integer facing = cc.get(CameraCharacteristics.LENS_FACING);
                    b.append("Orientación: ").append(facingName(facing)).append('\n');
                    Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    b.append("Flash: ").append(flash != null && flash ? "sí" : "no").append('\n');
                    Integer hw = cc.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    b.append("Nivel HW: ").append(hwLevel(hw));
                    android.util.SizeF ps = cc.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    if (ps != null) b.append('\n').append("Sensor: ")
                            .append(String.format(Locale.US, "%.2f x %.2f mm", ps.getWidth(), ps.getHeight()));
                    android.util.Size pa = cc.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    if (pa != null) b.append('\n').append("Resolución máx.: ")
                            .append(pa.getWidth()).append("x").append(pa.getHeight());
                    s.add("Cámara " + id, b.toString());
                }
                return s;
            } catch (Throwable ignore) {
            }
        }
        // Legacy fallback.
        try {
            int n = android.hardware.Camera.getNumberOfCameras();
            s.add("Número de cámaras (legacy)", n);
        } catch (Throwable t) {
            s.add("Cámaras", "no disponible");
        }
        return s;
    }

    // ============================ 10. SENSORS ============================
    private InfoSection sensors() {
        InfoSection s = new InfoSection("Sensores");
        try {
            SensorManager sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> list = sm.getSensorList(Sensor.TYPE_ALL);
            s.add("Número de sensores", list.size());
            for (Sensor sensor : list) {
                StringBuilder b = new StringBuilder();
                b.append("Tipo: ").append(Build.VERSION.SDK_INT >= 20
                        ? Formats.nn(sensor.getStringType()) : String.valueOf(sensor.getType())).append('\n');
                b.append("Fabricante: ").append(Formats.nn(sensor.getVendor())).append('\n');
                b.append("Versión: ").append(sensor.getVersion()).append('\n');
                b.append("Potencia: ").append(sensor.getPower()).append(" mA\n");
                b.append("Resolución: ").append(sensor.getResolution()).append('\n');
                b.append("Rango máx.: ").append(sensor.getMaximumRange());
                if (Build.VERSION.SDK_INT >= 21) {
                    b.append('\n').append("Wake-up: ").append(sensor.isWakeUpSensor() ? "sí" : "no");
                }
                s.add(Formats.nn(sensor.getName()), b.toString());
            }
        } catch (Throwable t) {
            s.add("Sensores", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 11. CONNECTIVITY ============================
    @SuppressWarnings("deprecation")
    private InfoSection connectivity() {
        InfoSection s = new InfoSection("Conectividad");
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null) {
                s.add("Red activa", Formats.nn(ni.getTypeName()) + " / " + Formats.nn(ni.getSubtypeName()));
                s.add("Conectada", ni.isConnected());
            } else {
                s.add("Red activa", "ninguna");
            }
            if (Build.VERSION.SDK_INT >= 23) {
                Network n = cm.getActiveNetwork();
                if (n != null) {
                    NetworkCapabilities nc = cm.getNetworkCapabilities(n);
                    if (nc != null) {
                        s.add("Transporte", transports(nc));
                        s.add("Ancho banda bajada", nc.getLinkDownstreamBandwidthKbps() + " kbps");
                        s.add("Ancho banda subida", nc.getLinkUpstreamBandwidthKbps() + " kbps");
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            s.add("WiFi habilitado", wm.isWifiEnabled());
            WifiInfo wi = wm.getConnectionInfo();
            if (wi != null) {
                s.addIfPresent("SSID", stripQuotes(wi.getSSID()));
                s.addIfPresent("BSSID", wi.getBSSID());
                if (wi.getLinkSpeed() > 0) s.add("Velocidad enlace", wi.getLinkSpeed() + " Mbps");
                s.add("RSSI", wi.getRssi() + " dBm");
                if (Build.VERSION.SDK_INT >= 21 && wi.getFrequency() > 0) {
                    s.add("Frecuencia", wi.getFrequency() + " MHz");
                }
                int ip = wi.getIpAddress();
                if (ip != 0) s.add("IP WiFi", intToIp(ip));
            }
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null) {
                s.addIfPresent("Gateway", dhcp.gateway != 0 ? intToIp(dhcp.gateway) : null);
                s.addIfPresent("DNS1", dhcp.dns1 != 0 ? intToIp(dhcp.dns1) : null);
            }
        } catch (Throwable ignore) {
        }
        return s;
    }

    // ============================ 12. NET INTERFACES ============================
    private InfoSection netInterfaces() {
        InfoSection s = new InfoSection("Interfaces de red");
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            if (ifaces == null) {
                s.add("Interfaces", "no disponible");
                return s;
            }
            List<NetworkInterface> all = Collections.list(ifaces);
            for (NetworkInterface ni : all) {
                StringBuilder b = new StringBuilder();
                try { b.append("Activa: ").append(ni.isUp() ? "sí" : "no").append('\n'); } catch (Throwable ig) { }
                try {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null && mac.length > 0) b.append("MAC: ").append(macToStr(mac)).append('\n');
                } catch (Throwable ig) { }
                try { if (ni.getMTU() > 0) b.append("MTU: ").append(ni.getMTU()).append('\n'); } catch (Throwable ig) { }
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (addr != null) {
                        b.append("IP: ").append(addr.getHostAddress())
                                .append("/").append(ia.getNetworkPrefixLength()).append('\n');
                    }
                }
                String body = b.toString().trim();
                if (!body.isEmpty()) {
                    s.add(ni.getName() + (ni.getDisplayName() != null
                            && !ni.getDisplayName().equals(ni.getName())
                            ? " (" + ni.getDisplayName() + ")" : ""), body);
                }
            }
        } catch (Throwable t) {
            s.add("Interfaces", "error: " + t);
        }
        return s;
    }

    // ============================ 13. TELEPHONY ============================
    private InfoSection telephony() {
        InfoSection s = new InfoSection("Telefonía / SIM");
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                s.add("Telefonía", "no disponible");
                return s;
            }
            s.add("Tipo de teléfono", phoneType(tm.getPhoneType()));
            s.addIfPresent("Operador de red", tm.getNetworkOperatorName());
            s.addIfPresent("Operador SIM", tm.getSimOperatorName());
            s.addIfPresent("País de red", up(tm.getNetworkCountryIso()));
            s.addIfPresent("País SIM", up(tm.getSimCountryIso()));
            s.add("Estado SIM", simState(tm.getSimState()));
            try { s.add("En roaming", tm.isNetworkRoaming()); } catch (Throwable ig) { }
        } catch (Throwable t) {
            s.add("Telefonía", "no disponible (" + t + ")");
        }
        if (s.size() <= 1) {
            s.add("Nota", "Este dispositivo no parece tener módulo de telefonía.");
        }
        return s;
    }

    // ============================ 14. BLUETOOTH ============================
    private InfoSection bluetooth() {
        InfoSection s = new InfoSection("Bluetooth");
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            if (a == null) {
                s.add("Bluetooth", "no soportado");
                return s;
            }
            s.add("Soportado", true);
            try { s.addIfPresent("Nombre", a.getName()); } catch (Throwable ig) { }
            try { s.addIfPresent("Dirección", a.getAddress()); } catch (Throwable ig) { }
            try { s.add("Habilitado", a.isEnabled()); } catch (Throwable ig) { }
        } catch (Throwable t) {
            s.add("Bluetooth", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 15. AUDIO ============================
    private InfoSection audio() {
        InfoSection s = new InfoSection("Audio");
        try {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            s.add("Volumen música", am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    + "/" + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            s.add("Volumen alarma", am.getStreamVolume(AudioManager.STREAM_ALARM)
                    + "/" + am.getStreamMaxVolume(AudioManager.STREAM_ALARM));
            if (Build.VERSION.SDK_INT >= 17) {
                s.addIfPresent("Sample rate salida", am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
                s.addIfPresent("Frames por buffer", am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
            }
            if (Build.VERSION.SDK_INT >= 23) {
                android.media.AudioDeviceInfo[] outs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                s.add("Salidas de audio", outs.length);
            }
        } catch (Throwable t) {
            s.add("Audio", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 16. INPUT DEVICES ============================
    private InfoSection inputDevices() {
        InfoSection s = new InfoSection("Dispositivos de entrada");
        try {
            int[] ids = InputDevice.getDeviceIds();
            s.add("Número de dispositivos", ids.length);
            for (int id : ids) {
                InputDevice d = InputDevice.getDevice(id);
                if (d == null) continue;
                StringBuilder b = new StringBuilder();
                b.append("Fuentes: ").append(sourcesOf(d));
                if (Build.VERSION.SDK_INT >= 19) {
                    b.append('\n').append("Vendor/Product: ")
                            .append(Integer.toHexString(d.getVendorId())).append(" / ")
                            .append(Integer.toHexString(d.getProductId()));
                }
                s.add(Formats.nn(d.getName()), b.toString());
            }
        } catch (Throwable t) {
            s.add("Entrada", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 17. CODECS ============================
    private InfoSection codecs() {
        InfoSection s = new InfoSection("Códecs multimedia");
        try {
            int count = MediaCodecList.getCodecCount();
            int enc = 0, dec = 0;
            StringBuilder all = new StringBuilder();
            for (int i = 0; i < count; i++) {
                MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                boolean isEnc = info.isEncoder();
                if (isEnc) enc++; else dec++;
                all.append(isEnc ? "[ENC] " : "[DEC] ").append(info.getName())
                        .append("  -> ").append(join(info.getSupportedTypes())).append('\n');
            }
            s.add("Total códecs", count);
            s.add("Codificadores", enc);
            s.add("Decodificadores", dec);
            s.add("Lista completa", all.toString().trim());
        } catch (Throwable t) {
            s.add("Códecs", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 18. FEATURES ============================
    private InfoSection features() {
        InfoSection s = new InfoSection("Características del sistema");
        PackageManager pm = ctx.getPackageManager();
        s.add("Pantalla táctil", pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN));
        s.add("Leanback (TV)", pm.hasSystemFeature("android.software.leanback"));
        s.add("Cámara", pm.hasSystemFeature(PackageManager.FEATURE_CAMERA));
        s.add("Bluetooth", pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
        s.add("WiFi", pm.hasSystemFeature(PackageManager.FEATURE_WIFI));
        s.add("Telefonía", pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY));
        s.add("GPS", pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS));
        s.add("NFC", pm.hasSystemFeature(PackageManager.FEATURE_NFC));
        if (Build.VERSION.SDK_INT >= 23) {
            s.add("Huella dactilar", pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT));
        }
        try {
            FeatureInfo[] fis = pm.getSystemAvailableFeatures();
            StringBuilder b = new StringBuilder();
            int n = 0;
            for (FeatureInfo fi : fis) {
                if (fi.name != null) {
                    b.append(fi.name).append('\n');
                    n++;
                }
            }
            s.add("Total features", n);
            s.add("Lista de features", b.toString().trim());
        } catch (Throwable ignore) {
        }
        return s;
    }

    // ============================ 19. APPS ============================
    private InfoSection apps() {
        InfoSection s = new InfoSection("Aplicaciones instaladas");
        try {
            PackageManager pm = ctx.getPackageManager();
            List<PackageInfo> pkgs = pm.getInstalledPackages(0);
            int system = 0, user = 0;
            StringBuilder b = new StringBuilder();
            for (PackageInfo pi : pkgs) {
                boolean sys = pi.applicationInfo != null
                        && (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (sys) system++; else user++;
                b.append(sys ? "[sys] " : "[usr] ").append(pi.packageName)
                        .append("  v").append(Formats.nn(pi.versionName)).append('\n');
            }
            s.add("Total instaladas", pkgs.size());
            s.add("Del sistema", system);
            s.add("De usuario", user);
            s.add("Lista completa", b.toString().trim());
        } catch (Throwable t) {
            s.add("Aplicaciones", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 20. LOCALE / TIME ============================
    private InfoSection localeTime() {
        InfoSection s = new InfoSection("Idioma / Región / Hora");
        Locale l = Locale.getDefault();
        s.add("Locale", l.toString());
        s.add("Idioma", l.getDisplayLanguage() + " (" + l.getLanguage() + ")");
        s.add("País", l.getDisplayCountry() + " (" + l.getCountry() + ")");
        TimeZone tz = TimeZone.getDefault();
        s.add("Zona horaria", tz.getID() + " — " + tz.getDisplayName());
        s.add("Offset UTC", String.format(Locale.US, "%+d min", tz.getRawOffset() / 60000));
        s.add("Hora actual", new Date().toString());
        s.add("Uptime (encendido)", humanMillis(SystemClock.elapsedRealtime()));
        s.add("Tiempo activo CPU", humanMillis(SystemClock.uptimeMillis()));
        s.add("Arranque (boot)", new Date(System.currentTimeMillis()
                - SystemClock.elapsedRealtime()).toString());
        s.add("Charset por defecto", java.nio.charset.Charset.defaultCharset().name());
        return s;
    }

    // ============================ 21. SYSTEM PROPERTIES ============================
    private InfoSection sysProps() {
        InfoSection s = new InfoSection("Propiedades del sistema");
        String getprop = exec("getprop");
        if (getprop != null && !getprop.isEmpty()) {
            s.addIfPresent("ro.product.name", propOf(getprop, "ro.product.name"));
            s.addIfPresent("ro.product.board", propOf(getprop, "ro.product.board"));
            s.addIfPresent("ro.board.platform", propOf(getprop, "ro.board.platform"));
            s.addIfPresent("ro.hardware", propOf(getprop, "ro.hardware"));
            s.addIfPresent("ro.build.description", propOf(getprop, "ro.build.description"));
            s.addIfPresent("dalvik.vm.heapsize", propOf(getprop, "dalvik.vm.heapsize"));
            int lines = getprop.split("\n").length;
            s.add("Total de propiedades", lines);
            s.add("Volcado completo (getprop)", getprop.trim());
        } else {
            s.add("getprop", "no accesible en este dispositivo");
        }
        return s;
    }

    // ============================ 22. RUNTIME / ENV ============================
    private InfoSection runtimeEnv() {
        InfoSection s = new InfoSection("Entorno de ejecución");
        s.add("Paquete de la app", ctx.getPackageName());
        try {
            PackageInfo self = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            s.add("Versión de la app", self.versionName + " (code " + self.versionCode + ")");
        } catch (Throwable ignore) {
        }
        s.add("PID / UID", Process.myPid() + " / " + Process.myUid());
        s.add("Java VM", Formats.nn(System.getProperty("java.vm.name")));
        s.add("Java version", Formats.nn(System.getProperty("java.version")));
        s.add("Java vendor", Formats.nn(System.getProperty("java.vendor")));
        s.add("OS name", Formats.nn(System.getProperty("os.name")));
        s.add("OS arch", Formats.nn(System.getProperty("os.arch")));
        s.add("User dir", Formats.nn(System.getProperty("user.dir")));
        s.add("Files dir (app)", Formats.nn(safePath(ctx.getFilesDir())));
        s.add("ANDROID_ROOT", Formats.nn(System.getenv("ANDROID_ROOT")));
        s.add("ANDROID_DATA", Formats.nn(System.getenv("ANDROID_DATA")));
        s.add("EXTERNAL_STORAGE", Formats.nn(System.getenv("EXTERNAL_STORAGE")));
        s.add("SECONDARY_STORAGE", Formats.nn(System.getenv("SECONDARY_STORAGE")));
        return s;
    }

    // ============================ 24. SECURITY / ROOT ============================
    private InfoSection security() {
        InfoSection s = new InfoSection("Seguridad / Root");
        s.add("¿Rooteado (binario su)?", isRooted());
        s.add("Build tags", Formats.nn(Build.TAGS));
        s.add("¿test-keys?", Build.TAGS != null && Build.TAGS.contains("test-keys"));
        s.addIfPresent("Verified boot state", getprop("ro.boot.verifiedbootstate"));
        s.addIfPresent("Bootloader bloqueado", getprop("ro.boot.flash.locked"));
        s.addIfPresent("Device state (vbmeta)", getprop("ro.boot.vbmeta.device_state"));
        s.addIfPresent("ro.debuggable", getprop("ro.debuggable"));
        s.addIfPresent("ro.secure", getprop("ro.secure"));
        s.addIfPresent("ADB habilitado", settingsGlobal("adb_enabled"));
        s.addIfPresent("Opciones de desarrollador", settingsGlobal("development_settings_enabled"));
        s.addIfPresent("OEM unlock permitido", settingsGlobal("oem_unlock_allowed"));
        s.addIfPresent("SELinux", selinuxMode());
        try {
            KeyguardManager km = (KeyguardManager) ctx.getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                s.add("Keyguard seguro", km.isKeyguardSecure());
                if (Build.VERSION.SDK_INT >= 23) {
                    s.add("Bloqueo de dispositivo", km.isDeviceSecure());
                }
            }
        } catch (Throwable ignore) {
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                android.hardware.biometrics.BiometricManager bm =
                        (android.hardware.biometrics.BiometricManager)
                                ctx.getSystemService(Context.BIOMETRIC_SERVICE);
                if (bm != null) {
                    s.add("Biometría", biometricStr(bm.canAuthenticate()));
                }
            } catch (Throwable ignore) {
            }
        }
        return s;
    }

    // ============================ 25. POWER ============================
    private InfoSection power() {
        InfoSection s = new InfoSection("Energía (PowerManager)");
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (pm == null) {
                s.add("PowerManager", "no disponible");
                return s;
            }
            if (Build.VERSION.SDK_INT >= 20) {
                s.add("Pantalla interactiva", pm.isInteractive());
            }
            if (Build.VERSION.SDK_INT >= 21) {
                s.add("Ahorro de energía", pm.isPowerSaveMode());
            }
            if (Build.VERSION.SDK_INT >= 23) {
                s.add("Modo Doze (idle)", pm.isDeviceIdleMode());
                s.add("Ignora optimización de batería",
                        pm.isIgnoringBatteryOptimizations(ctx.getPackageName()));
            }
            if (Build.VERSION.SDK_INT >= 24) {
                s.add("Sustained performance", pm.isSustainedPerformanceModeSupported());
            }
            if (Build.VERSION.SDK_INT >= 29) {
                s.add("Estado térmico", thermalStatusStr(pm.getCurrentThermalStatus()));
            }
        } catch (Throwable t) {
            s.add("Energía", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 26. DRM ============================
    private InfoSection drm() {
        InfoSection s = new InfoSection("DRM protegido (Widevine)");
        if (Build.VERSION.SDK_INT < 18) {
            s.add("DRM", "requiere Android 4.3+");
            return s;
        }
        UUID widevine = new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
        UUID playready = new UUID(0x9A04F07998404286L, 0xAB92E65BE0885F95L);
        UUID clearkey = new UUID(0x1077EFECC0B24D02L, 0xACE33C1E52E2FB4BL);
        boolean wv = false;
        try {
            wv = MediaDrm.isCryptoSchemeSupported(widevine);
            s.add("Widevine soportado", wv);
            s.add("PlayReady soportado", MediaDrm.isCryptoSchemeSupported(playready));
            s.add("ClearKey soportado", MediaDrm.isCryptoSchemeSupported(clearkey));
        } catch (Throwable ignore) {
        }
        MediaDrm md = null;
        try {
            if (wv) {
                md = new MediaDrm(widevine);
                s.addIfPresent("Widevine version", drmProp(md, "version"));
                s.addIfPresent("Nivel de seguridad", drmProp(md, "securityLevel"));
                s.addIfPresent("Nivel HDCP", drmProp(md, "hdcpLevel"));
                s.addIfPresent("Max HDCP", drmProp(md, "maxHdcpLevel"));
                s.addIfPresent("System ID", drmProp(md, "systemId"));
                s.addIfPresent("OEM Crypto API", drmProp(md, "oemCryptoApiVersion"));
                s.addIfPresent("Descripción", drmProp(md, "description"));
                s.addIfPresent("Algoritmos", drmProp(md, "algorithms"));
            }
        } catch (Throwable ignore) {
        } finally {
            if (md != null) {
                try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        md.close();
                    } else {
                        releaseDrm(md);
                    }
                } catch (Throwable ignore) {
                }
            }
        }
        return s;
    }

    // ============================ 27. WIFI CAPABILITIES ============================
    private InfoSection wifiCaps() {
        InfoSection s = new InfoSection("WiFi (capacidades)");
        try {
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                s.add("WiFi", "no disponible");
                return s;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                s.add("5 GHz soportado", wm.is5GHzBandSupported());
                s.add("Wi-Fi Direct (P2P)", wm.isP2pSupported());
                s.add("RTT (802.11mc)", wm.isDeviceToApRttSupported());
                s.add("Enhanced power reporting", wm.isEnhancedPowerReportingSupported());
                s.add("TDLS", wm.isTdlsSupported());
                s.add("Preferred network offload", wm.isPreferredNetworkOffloadSupported());
            }
            if (Build.VERSION.SDK_INT >= 29) {
                s.add("WPA3-SAE", wm.isWpa3SaeSupported());
                s.add("WPA3-Enterprise (Suite-B)", wm.isWpa3SuiteBSupported());
                s.add("Enhanced Open (OWE)", wm.isEnhancedOpenSupported());
                s.add("Easy Connect (DPP)", wm.isEasyConnectSupported());
            }
            if (Build.VERSION.SDK_INT >= 30) {
                s.add("6 GHz soportado", wm.is6GHzBandSupported());
                s.add("STA+AP concurrencia", wm.isStaApConcurrencySupported());
                s.add("Sugerencias de red máx/app", wm.getMaxNumberOfNetworkSuggestionsPerApp());
                s.add("Nivel de señal máx", wm.getMaxSignalLevel());
            }
        } catch (Throwable t) {
            s.add("WiFi caps", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 28. TRAFFIC ============================
    private InfoSection traffic() {
        InfoSection s = new InfoSection("Uso de datos (desde el arranque)");
        long tr = TrafficStats.getTotalRxBytes();
        long tt = TrafficStats.getTotalTxBytes();
        if (tr == TrafficStats.UNSUPPORTED) {
            s.add("TrafficStats", "no soportado en este dispositivo");
            return s;
        }
        s.add("Total recibido", Formats.humanBytesDetailed(tr));
        s.add("Total enviado", Formats.humanBytesDetailed(tt));
        long mr = TrafficStats.getMobileRxBytes();
        long mt = TrafficStats.getMobileTxBytes();
        s.add("Móvil recibido", Formats.humanBytesDetailed(mr));
        s.add("Móvil enviado", Formats.humanBytesDetailed(mt));
        try {
            long ur = TrafficStats.getUidRxBytes(Process.myUid());
            long ut = TrafficStats.getUidTxBytes(Process.myUid());
            if (ur > 0) {
                s.add("Esta app recibido", Formats.humanBytes(ur));
            }
            if (ut > 0) {
                s.add("Esta app enviado", Formats.humanBytes(ut));
            }
        } catch (Throwable ignore) {
        }
        return s;
    }

    // ============================ 29. USB ============================
    private InfoSection usb() {
        InfoSection s = new InfoSection("USB conectado");
        try {
            UsbManager um = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
            if (um == null) {
                s.add("USB", "no disponible");
                return s;
            }
            java.util.Collection<UsbDevice> devs = um.getDeviceList().values();
            s.add("Dispositivos USB", devs.size());
            for (UsbDevice d : devs) {
                StringBuilder b = new StringBuilder();
                b.append("VID:PID: ").append(String.format(Locale.US, "%04X:%04X",
                        d.getVendorId(), d.getProductId())).append('\n');
                b.append("Clase: ").append(d.getDeviceClass());
                if (Build.VERSION.SDK_INT >= 21) {
                    String mn = d.getManufacturerName();
                    String pn = d.getProductName();
                    if (mn != null) {
                        b.append('\n').append("Fabricante: ").append(mn);
                    }
                    if (pn != null) {
                        b.append('\n').append("Producto: ").append(pn);
                    }
                }
                b.append('\n').append("Interfaces: ").append(d.getInterfaceCount());
                s.add(Formats.nn(d.getDeviceName()), b.toString());
            }
            if (devs.isEmpty()) {
                s.add("Nota", "No hay dispositivos USB conectados ahora.");
            }
        } catch (Throwable t) {
            s.add("USB", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 30. LOCATION ============================
    private InfoSection location() {
        InfoSection s = new InfoSection("Ubicación (proveedores)");
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                s.add("Ubicación", "no disponible");
                return s;
            }
            if (Build.VERSION.SDK_INT >= 28) {
                s.add("Ubicación habilitada", lm.isLocationEnabled());
            }
            try { s.add("GPS habilitado", lm.isProviderEnabled(LocationManager.GPS_PROVIDER)); } catch (Throwable ig) { }
            try { s.add("Red habilitado", lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)); } catch (Throwable ig) { }
            s.add("Todos los proveedores", join(lm.getAllProviders()));
            s.add("Proveedores activos", join(lm.getProviders(true)));
            if (Build.VERSION.SDK_INT >= 28) {
                s.addIfPresent("Modelo GNSS", lm.getGnssHardwareModelName());
                int yr = lm.getGnssYearOfHardware();
                if (yr > 0) {
                    s.add("Año del hardware GNSS", yr);
                }
            }
        } catch (Throwable t) {
            s.add("Ubicación", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 31. SYSTEM SETTINGS ============================
    private InfoSection systemSettings() {
        InfoSection s = new InfoSection("Ajustes del sistema");
        s.addIfPresent("Android ID", settingsSecure("android_id"));
        s.addIfPresent("Nombre del dispositivo", settingsGlobal("device_name"));
        s.addIfPresent("Brillo de pantalla", settingsSystem("screen_brightness"));
        s.addIfPresent("Brillo automático", settingsSystem("screen_brightness_mode"));
        s.addIfPresent("Timeout de pantalla (ms)", settingsSystem("screen_off_timeout"));
        s.addIfPresent("Rotación automática", settingsSystem("accelerometer_rotation"));
        s.addIfPresent("Escala de fuente", settingsSystem("font_scale"));
        s.addIfPresent("Modo avión", settingsGlobal("airplane_mode_on"));
        s.addIfPresent("Auto hora", settingsGlobal("auto_time"));
        s.addIfPresent("Auto zona horaria", settingsGlobal("auto_time_zone"));
        s.addIfPresent("Datos móviles", settingsGlobal("mobile_data"));
        s.addIfPresent("Wi-Fi (estado guardado)", settingsGlobal("wifi_on"));
        s.addIfPresent("Boot count", settingsGlobal("boot_count"));
        s.addIfPresent("Private DNS modo", settingsGlobal("private_dns_mode"));
        s.addIfPresent("Private DNS host", settingsGlobal("private_dns_specifier"));
        s.addIfPresent("Método de entrada", settingsSecure("default_input_method"));
        s.addIfPresent("Servicios de accesibilidad", settingsSecure("enabled_accessibility_services"));
        return s;
    }

    // ============================ 32. PARTITIONS ============================
    private InfoSection partitions() {
        InfoSection s = new InfoSection("Particiones del sistema");
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                for (Build.Partition p : Build.getFingerprintedPartitions()) {
                    s.add(Formats.nn(p.getName()),
                            "fingerprint: " + Formats.nn(p.getFingerprint())
                                    + "\nbuild: " + new Date(p.getBuildTimeMillis()));
                }
            } catch (Throwable t) {
                s.add("Particiones", "no disponible (" + t + ")");
            }
        }
        if (s.isEmpty()) {
            s.addIfPresent("system", getprop("ro.system.build.fingerprint"));
            s.addIfPresent("vendor", getprop("ro.vendor.build.fingerprint"));
            s.addIfPresent("product", getprop("ro.product.build.fingerprint"));
            s.addIfPresent("boot", getprop("ro.bootimage.build.fingerprint"));
            if (s.isEmpty()) {
                s.add("Particiones", "requiere Android 10+ o getprop");
            }
        }
        return s;
    }

    // ============================ 33. DISPLAYS ============================
    private InfoSection displays() {
        InfoSection s = new InfoSection("Pantallas (DisplayManager)");
        if (Build.VERSION.SDK_INT < 17) {
            s.add("Pantallas", "requiere Android 4.2+");
            return s;
        }
        try {
            DisplayManager dm = (DisplayManager) ctx.getSystemService(Context.DISPLAY_SERVICE);
            if (dm == null) {
                s.add("DisplayManager", "no disponible");
                return s;
            }
            Display[] ds = dm.getDisplays();
            s.add("Número de pantallas", ds.length);
            for (Display d : ds) {
                StringBuilder b = new StringBuilder();
                b.append("ID: ").append(d.getDisplayId()).append('\n');
                DisplayMetrics dmx = new DisplayMetrics();
                d.getRealMetrics(dmx);
                b.append("Resolución: ").append(dmx.widthPixels).append("x").append(dmx.heightPixels)
                        .append(" @ ").append(dmx.densityDpi).append("dpi\n");
                b.append("Refresco: ").append(String.format(Locale.US, "%.1f Hz", d.getRefreshRate()));
                if (Build.VERSION.SDK_INT >= 26) {
                    b.append('\n').append("HDR: ").append(d.isHdr() ? "sí" : "no");
                    b.append('\n').append("Wide color gamut: ").append(d.isWideColorGamut() ? "sí" : "no");
                }
                s.add(Formats.nn(d.getName()), b.toString());
            }
        } catch (Throwable t) {
            s.add("Pantallas", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 34. SOFTWARE ============================
    private InfoSection software() {
        InfoSection s = new InfoSection("Software del sistema");
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                android.content.pm.PackageInfo wv = WebView.getCurrentWebViewPackage();
                if (wv != null) {
                    s.add("WebView", wv.packageName + " " + wv.versionName);
                }
            } catch (Throwable ignore) {
            }
        }
        s.addIfPresent("Google Play Services", pkgVersion("com.google.android.gms"));
        s.addIfPresent("Google Play Store", pkgVersion("com.android.vending"));
        try {
            Vibrator vb = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            if (vb != null) {
                s.add("Vibrador", vb.hasVibrator());
                if (Build.VERSION.SDK_INT >= 26) {
                    s.add("Control de amplitud", vb.hasAmplitudeControl());
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            android.nfc.NfcManager nm = (android.nfc.NfcManager) ctx.getSystemService(Context.NFC_SERVICE);
            android.nfc.NfcAdapter na = (nm != null) ? nm.getDefaultAdapter() : null;
            s.add("NFC", na != null ? (na.isEnabled() ? "habilitado" : "presente, apagado") : "no soportado");
        } catch (Throwable ignore) {
        }
        try {
            AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                s.add("Accesibilidad activa", am.isEnabled());
                s.add("Exploración táctil", am.isTouchExplorationEnabled());
            }
        } catch (Throwable ignore) {
        }
        try {
            PackageInfo self = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            s.add("App instalada (primera vez)", new Date(self.firstInstallTime).toString());
            s.add("App actualizada", new Date(self.lastUpdateTime).toString());
        } catch (Throwable ignore) {
        }
        return s;
    }

    // ============================ 35. KERNEL / PROC ============================
    private InfoSection kernelProc() {
        InfoSection s = new InfoSection("Kernel / procesos");
        s.addIfPresent("Kernel (os.version)", System.getProperty("os.version"));
        String ver = readFile("/proc/version");
        if (ver != null) {
            s.add("/proc/version", ver.trim());
        }
        s.addIfPresent("Hostname", trim(readFile("/proc/sys/kernel/hostname")));
        s.addIfPresent("SELinux", selinuxMode());
        String up = trim(readFile("/proc/uptime"));
        if (up != null) {
            try {
                double secs = Double.parseDouble(up.split("\\s+")[0]);
                s.add("Uptime", humanMillis((long) (secs * 1000)));
            } catch (Throwable ig) {
            }
        }
        s.addIfPresent("Load average", trim(readFile("/proc/loadavg")));
        s.addIfPresent("CPU online", trim(readFile("/sys/devices/system/cpu/online")));
        s.addIfPresent("CPU present", trim(readFile("/sys/devices/system/cpu/present")));
        s.addIfPresent("CPU possible", trim(readFile("/sys/devices/system/cpu/possible")));
        s.add("Procesadores (runtime)", Runtime.getRuntime().availableProcessors());
        s.add("Threads activos (app)", Thread.activeCount());
        s.addIfPresent("Boot ID", trim(readFile("/proc/sys/kernel/random/boot_id")));
        return s;
    }

    // ---- helpers for the extended sections ----

    private boolean isRooted() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
                "/system/app/Superuser.apk", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
                "/vendor/bin/su", "/system/xbin/busybox"};
        for (String p : paths) {
            try {
                if (new File(p).exists()) {
                    return true;
                }
            } catch (Throwable ignore) {
            }
        }
        return Build.TAGS != null && Build.TAGS.contains("test-keys");
    }

    private String getprop(String key) {
        String v = exec("getprop", key);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    private String settingsGlobal(String key) {
        try {
            if (Build.VERSION.SDK_INT >= 17) {
                return Settings.Global.getString(ctx.getContentResolver(), key);
            }
        } catch (Throwable ignore) {
        }
        try {
            return Settings.System.getString(ctx.getContentResolver(), key);
        } catch (Throwable t) {
            return null;
        }
    }

    private String settingsSecure(String key) {
        try {
            return Settings.Secure.getString(ctx.getContentResolver(), key);
        } catch (Throwable t) {
            return null;
        }
    }

    private String settingsSystem(String key) {
        try {
            return Settings.System.getString(ctx.getContentResolver(), key);
        } catch (Throwable t) {
            return null;
        }
    }

    private String selinuxMode() {
        String e = trim(readFile("/sys/fs/selinux/enforce"));
        if ("1".equals(e)) {
            return "Enforcing";
        }
        if ("0".equals(e)) {
            return "Permissive";
        }
        return getprop("ro.boot.selinux");
    }

    private String pkgVersion(String pkg) {
        try {
            return ctx.getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String drmProp(MediaDrm md, String key) {
        try {
            return md.getPropertyString(key);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static void releaseDrm(MediaDrm md) {
        md.release();
    }

    private static String biometricStr(int r) {
        switch (r) {
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS:
                return "disponible y configurada";
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return "sin hardware";
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "hardware no disponible";
            case android.hardware.biometrics.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return "hardware presente, sin datos configurados";
            default:
                return "código " + r;
        }
    }

    private static String thermalStatusStr(int st) {
        switch (st) {
            case PowerManager.THERMAL_STATUS_NONE: return "normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "leve";
            case PowerManager.THERMAL_STATUS_MODERATE: return "moderado";
            case PowerManager.THERMAL_STATUS_SEVERE: return "severo";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "crítico";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "emergencia";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "apagado inminente";
            default: return "código " + st;
        }
    }

    // ============================ 36. TELEPHONY DETAIL ============================
    private InfoSection telephonyDetail() {
        InfoSection s = new InfoSection("Telefonía detallada");
        if (!hasPerm(android.Manifest.permission.READ_PHONE_STATE)) {
            s.add("Permiso requerido", "READ_PHONE_STATE — tocá 'Permisos' y concedelo");
            return s;
        }
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm == null) {
            s.add("Telefonía", "no disponible");
            return s;
        }
        try { s.addIfPresent("Versión de software radio", tm.getDeviceSoftwareVersion()); } catch (Throwable ig) { }
        s.add("IMEI / MEID", Formats.nn(restricted(tm, 0)));
        s.add("IMSI (subscriber)", Formats.nn(restricted(tm, 1)));
        s.add("Serial de SIM", Formats.nn(restricted(tm, 2)));
        try { s.addIfPresent("Número de teléfono", tm.getLine1Number()); } catch (Throwable ig) { }
        if (Build.VERSION.SDK_INT >= 30) {
            try { s.addIfPresent("TAC (Type Allocation Code)", tm.getTypeAllocationCode()); } catch (Throwable ig) { }
            try { s.addIfPresent("Código de fabricante", tm.getManufacturerCode()); } catch (Throwable ig) { }
        }
        s.addIfPresent("Operador de red (MCC/MNC)", tm.getNetworkOperator());
        s.addIfPresent("Nombre del operador de red", tm.getNetworkOperatorName());
        s.addIfPresent("Operador de SIM (MCC/MNC)", tm.getSimOperator());
        s.addIfPresent("Nombre del operador SIM", tm.getSimOperatorName());
        s.addIfPresent("País de red", up(tm.getNetworkCountryIso()));
        s.addIfPresent("País de SIM", up(tm.getSimCountryIso()));
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                CharSequence cn = tm.getSimCarrierIdName();
                if (cn != null) s.add("Carrier ID", cn.toString());
            } catch (Throwable ig) { }
        }
        s.add("Tipo de teléfono", phoneType(tm.getPhoneType()));
        s.add("Estado de SIM", simState(tm.getSimState()));
        s.add("Estado de datos", dataState(tm.getDataState()));
        if (Build.VERSION.SDK_INT >= 24) {
            try { s.add("Red de datos", networkTypeName(tm.getDataNetworkType())); } catch (Throwable ig) { }
            try { s.add("Red de voz", networkTypeName(tm.getVoiceNetworkType())); } catch (Throwable ig) { }
        }
        try { s.add("En roaming", tm.isNetworkRoaming()); } catch (Throwable ig) { }
        if (Build.VERSION.SDK_INT >= 23) {
            try { s.add("Nº de teléfonos (SIM slots)", tm.getPhoneCount()); } catch (Throwable ig) { }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            try { s.add("Módems activos", tm.getActiveModemCount()); } catch (Throwable ig) { }
        }
        try { s.add("Capaz de voz / SMS", tm.isVoiceCapable() + " / " + tm.isSmsCapable()); } catch (Throwable ig) { }
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                SignalStrength ss = tm.getSignalStrength();
                if (ss != null) s.add("Nivel de señal", ss.getLevel() + "/4");
            } catch (Throwable ig) { }
        }
        return s;
    }

    // ============================ 37. SUBSCRIPTIONS ============================
    private InfoSection subscriptions() {
        InfoSection s = new InfoSection("SIMs (SubscriptionManager)");
        if (Build.VERSION.SDK_INT < 22) {
            s.add("SubscriptionManager", "requiere Android 5.1+");
            return s;
        }
        if (!hasPerm(android.Manifest.permission.READ_PHONE_STATE)) {
            s.add("Permiso requerido", "READ_PHONE_STATE");
            return s;
        }
        try {
            SubscriptionManager sm = (SubscriptionManager)
                    ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm == null) {
                s.add("SIMs", "no disponible");
                return s;
            }
            List<SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
            if (list == null || list.isEmpty()) {
                s.add("SIMs activas", "0");
                return s;
            }
            s.add("SIMs activas", list.size());
            int i = 0;
            for (SubscriptionInfo si : list) {
                i++;
                StringBuilder b = new StringBuilder();
                b.append("Operador: ").append(Formats.nn(si.getCarrierName())).append('\n');
                b.append("Nombre: ").append(Formats.nn(si.getDisplayName())).append('\n');
                b.append("Slot: ").append(si.getSimSlotIndex())
                        .append("  ID: ").append(si.getSubscriptionId()).append('\n');
                b.append("País: ").append(Formats.nn(si.getCountryIso()));
                if (Build.VERSION.SDK_INT >= 29) {
                    b.append('\n').append("MCC/MNC: ").append(Formats.nn(si.getMccString()))
                            .append("/").append(Formats.nn(si.getMncString()));
                }
                if (Build.VERSION.SDK_INT >= 28) {
                    b.append('\n').append("eSIM: ").append(si.isEmbedded() ? "sí" : "no");
                }
                b.append('\n').append("Data roaming: ").append(si.getDataRoaming() == 1 ? "activado" : "desactivado");
                try {
                    CharSequence num = si.getNumber();
                    if (num != null && num.length() > 0) {
                        b.append('\n').append("Número: ").append(num);
                    }
                } catch (Throwable ig) { }
                s.add("SIM " + i, b.toString());
            }
        } catch (Throwable t) {
            s.add("SIMs", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 38. CELLS ============================
    private InfoSection cells() {
        InfoSection s = new InfoSection("Celdas (CellInfo)");
        if (!hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            s.add("Permiso requerido", "ACCESS_FINE_LOCATION");
            return s;
        }
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                s.add("Celdas", "no disponible");
                return s;
            }
            List<CellInfo> cells = tm.getAllCellInfo();
            if (cells == null || cells.isEmpty()) {
                s.add("Celdas", "sin información (¿sin SIM o sin señal?)");
                return s;
            }
            s.add("Celdas detectadas", cells.size());
            int i = 0;
            for (CellInfo c : cells) {
                i++;
                String type = c.getClass().getSimpleName().replace("CellInfo", "");
                String reg = c.isRegistered() ? " · registrada" : "";
                s.add("Celda " + i + " · " + type + reg, String.valueOf(c));
            }
        } catch (SecurityException se) {
            s.add("Celdas", "permiso de ubicación no concedido");
        } catch (Throwable t) {
            s.add("Celdas", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 39. GPS LOCATION ============================
    private InfoSection gpsLocation() {
        InfoSection s = new InfoSection("Ubicación GPS");
        if (!hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION)
                && !hasPerm(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            s.add("Permiso requerido", "ACCESS_FINE_LOCATION / COARSE");
            return s;
        }
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                s.add("Ubicación", "no disponible");
                return s;
            }
            Location best = null;
            String[] provs = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER,
                    LocationManager.PASSIVE_PROVIDER};
            for (String p : provs) {
                try {
                    Location l = lm.getLastKnownLocation(p);
                    if (l != null && (best == null || l.getTime() > best.getTime())) {
                        best = l;
                    }
                } catch (Throwable ig) { }
            }
            if (best == null) {
                s.add("Última ubicación", "sin fix reciente (abrí un mapa para obtener uno)");
                return s;
            }
            s.add("Latitud", String.valueOf(best.getLatitude()));
            s.add("Longitud", String.valueOf(best.getLongitude()));
            if (best.hasAltitude()) s.add("Altitud", String.format(Locale.US, "%.1f m", best.getAltitude()));
            if (best.hasAccuracy()) s.add("Precisión", String.format(Locale.US, "%.1f m", best.getAccuracy()));
            if (best.hasSpeed()) s.add("Velocidad", String.format(Locale.US, "%.1f m/s", best.getSpeed()));
            if (best.hasBearing()) s.add("Rumbo", String.format(Locale.US, "%.0f°", best.getBearing()));
            s.add("Proveedor", Formats.nn(best.getProvider()));
            s.add("Hora del fix", new Date(best.getTime()).toString());
        } catch (SecurityException se) {
            s.add("Ubicación", "permiso no concedido");
        } catch (Throwable t) {
            s.add("Ubicación", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 40. WIFI DETAIL ============================
    @SuppressWarnings("deprecation")
    private InfoSection wifiDetail() {
        InfoSection s = new InfoSection("WiFi conectado (detalle)");
        try {
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                s.add("WiFi", "no disponible");
                return s;
            }
            s.add("WiFi habilitado", wm.isWifiEnabled());
            WifiInfo wi = wm.getConnectionInfo();
            if (wi == null) {
                s.add("WiFi", "sin conexión");
                return s;
            }
            String ssid = stripQuotes(wi.getSSID());
            if (ssid == null && !hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                s.add("SSID", "requiere permiso de ubicación");
            } else {
                s.addIfPresent("SSID", ssid);
            }
            s.addIfPresent("BSSID", wi.getBSSID());
            if (wi.getLinkSpeed() > 0) s.add("Velocidad de enlace", wi.getLinkSpeed() + " Mbps");
            if (Build.VERSION.SDK_INT >= 30) {
                if (wi.getTxLinkSpeedMbps() > 0) s.add("Tx link speed", wi.getTxLinkSpeedMbps() + " Mbps");
                if (wi.getRxLinkSpeedMbps() > 0) s.add("Rx link speed", wi.getRxLinkSpeedMbps() + " Mbps");
                s.add("Estándar WiFi", wifiStandardStr(wi.getWifiStandard()));
            }
            s.add("RSSI", wi.getRssi() + " dBm");
            if (Build.VERSION.SDK_INT >= 21 && wi.getFrequency() > 0) {
                s.add("Frecuencia", wi.getFrequency() + " MHz");
            }
            int ip = wi.getIpAddress();
            if (ip != 0) s.add("IP", intToIp(ip));
            String mac = wi.getMacAddress();
            if (mac != null && !"02:00:00:00:00:00".equals(mac)) s.add("MAC", mac);
            s.add("SSID oculto", wi.getHiddenSSID());
            DhcpInfo dh = wm.getDhcpInfo();
            if (dh != null) {
                s.addIfPresent("Gateway", dh.gateway != 0 ? intToIp(dh.gateway) : null);
                s.addIfPresent("DNS1", dh.dns1 != 0 ? intToIp(dh.dns1) : null);
                s.addIfPresent("DNS2", dh.dns2 != 0 ? intToIp(dh.dns2) : null);
                s.addIfPresent("Máscara", dh.netmask != 0 ? intToIp(dh.netmask) : null);
                if (dh.leaseDuration > 0) s.add("Lease DHCP", dh.leaseDuration + " s");
            }
        } catch (Throwable t) {
            s.add("WiFi", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 41. WIFI SCAN ============================
    private InfoSection wifiScan() {
        InfoSection s = new InfoSection("Redes WiFi cercanas");
        if (!hasPerm(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            s.add("Permiso requerido", "ACCESS_FINE_LOCATION");
            return s;
        }
        try {
            WifiManager wm = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (wm == null) {
                s.add("WiFi", "no disponible");
                return s;
            }
            List<ScanResult> res = wm.getScanResults();
            if (res == null || res.isEmpty()) {
                s.add("Redes", "sin resultados (activá WiFi y ubicación)");
                return s;
            }
            s.add("Redes detectadas", res.size());
            StringBuilder b = new StringBuilder();
            for (ScanResult r : res) {
                b.append((r.SSID == null || r.SSID.isEmpty()) ? "(oculta)" : r.SSID)
                        .append("  ").append(r.frequency).append("MHz  ")
                        .append(r.level).append("dBm  ").append(secFromCaps(r.capabilities))
                        .append("  ").append(r.BSSID).append('\n');
            }
            s.add("Lista", b.toString().trim());
        } catch (SecurityException se) {
            s.add("Redes", "permiso o ubicación no concedidos");
        } catch (Throwable t) {
            s.add("Redes", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 42. BLUETOOTH PAIRED ============================
    private InfoSection bluetoothPaired() {
        InfoSection s = new InfoSection("Bluetooth emparejados");
        if (Build.VERSION.SDK_INT >= 31
                && !hasPerm(android.Manifest.permission.BLUETOOTH_CONNECT)) {
            s.add("Permiso requerido", "BLUETOOTH_CONNECT");
            return s;
        }
        try {
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            if (a == null) {
                s.add("Bluetooth", "no soportado");
                return s;
            }
            try { s.addIfPresent("Adaptador", a.getName()); } catch (Throwable ig) { }
            try { s.add("Habilitado", a.isEnabled()); } catch (Throwable ig) { }
            java.util.Set<BluetoothDevice> bonded = a.getBondedDevices();
            s.add("Emparejados", bonded != null ? bonded.size() : 0);
            if (bonded != null) {
                for (BluetoothDevice d : bonded) {
                    StringBuilder b = new StringBuilder();
                    b.append("Dirección: ").append(Formats.nn(d.getAddress()));
                    if (Build.VERSION.SDK_INT >= 21) {
                        b.append('\n').append("Tipo: ").append(btTypeStr(d.getType()));
                    }
                    b.append('\n').append("Vínculo: ").append(bondStateStr(d.getBondState()));
                    try {
                        if (d.getBluetoothClass() != null) {
                            b.append('\n').append("Clase: 0x")
                                    .append(Integer.toHexString(d.getBluetoothClass().getDeviceClass()));
                        }
                    } catch (Throwable ig) { }
                    s.add(Formats.nn(d.getName()), b.toString());
                }
            }
        } catch (SecurityException se) {
            s.add("Bluetooth", "permiso no concedido");
        } catch (Throwable t) {
            s.add("Bluetooth", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 43. ACCOUNT TYPES ============================
    private InfoSection accountTypes() {
        InfoSection s = new InfoSection("Cuentas (tipos disponibles)");
        try {
            AccountManager am = AccountManager.get(ctx);
            AuthenticatorDescription[] types = am.getAuthenticatorTypes();
            s.add("Autenticadores", types.length);
            StringBuilder b = new StringBuilder();
            for (AuthenticatorDescription t : types) {
                b.append(t.type).append("  (").append(t.packageName).append(")\n");
            }
            if (b.length() > 0) s.add("Lista", b.toString().trim());
            s.add("Nota", "Solo los TIPOS de cuenta (Google, WhatsApp, etc.). "
                    + "No se leen tus cuentas ni datos personales.");
        } catch (Throwable t) {
            s.add("Cuentas", "no disponible (" + t + ")");
        }
        return s;
    }

    // ============================ 44. PERMISSIONS ============================
    private InfoSection permissions() {
        InfoSection s = new InfoSection("Permisos");
        String[][] p = {
            {"Almacenamiento", android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
            {"Teléfono (READ_PHONE_STATE)", android.Manifest.permission.READ_PHONE_STATE},
            {"Número (READ_PHONE_NUMBERS)", "android.permission.READ_PHONE_NUMBERS"},
            {"Ubicación fina", android.Manifest.permission.ACCESS_FINE_LOCATION},
            {"Ubicación aprox.", android.Manifest.permission.ACCESS_COARSE_LOCATION},
            {"Bluetooth (CONNECT)", "android.permission.BLUETOOTH_CONNECT"},
        };
        for (String[] pr : p) {
            s.add(pr[0], hasPerm(pr[1]) ? "concedido ✓" : "no concedido ✗");
        }
        s.add("Nota", "Sin estos permisos, algunas secciones muestran menos datos. "
                + "Tocá el botón 'Permisos' para concederlos.");
        return s;
    }

    // ---- helpers for permission-gated sections ----

    private boolean hasPerm(String perm) {
        try {
            if (Build.VERSION.SDK_INT >= 23) {
                return ctx.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
            }
            return ctx.getPackageManager().checkPermission(perm, ctx.getPackageName())
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String restricted(TelephonyManager tm, int which) {
        try {
            switch (which) {
                case 0: return (Build.VERSION.SDK_INT >= 26) ? tm.getImei() : tm.getDeviceId();
                case 1: return tm.getSubscriberId();
                case 2: return tm.getSimSerialNumber();
                default: return null;
            }
        } catch (SecurityException se) {
            return "restringido (Android 10+, solo apps del sistema)";
        } catch (Throwable t) {
            return null;
        }
    }

    private static String secFromCaps(String caps) {
        if (caps == null) return "?";
        if (caps.contains("WPA3")) return "WPA3";
        if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA")) return "WPA";
        if (caps.contains("WEP")) return "WEP";
        return "abierta";
    }

    private static String dataState(int st) {
        switch (st) {
            case TelephonyManager.DATA_DISCONNECTED: return "desconectado";
            case TelephonyManager.DATA_CONNECTING: return "conectando";
            case TelephonyManager.DATA_CONNECTED: return "conectado";
            case TelephonyManager.DATA_SUSPENDED: return "suspendido";
            default: return "estado " + st;
        }
    }

    private static String btTypeStr(int t) {
        switch (t) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC: return "clásico";
            case BluetoothDevice.DEVICE_TYPE_LE: return "BLE";
            case BluetoothDevice.DEVICE_TYPE_DUAL: return "dual";
            default: return "desconocido";
        }
    }

    private static String bondStateStr(int st) {
        switch (st) {
            case BluetoothDevice.BOND_BONDED: return "emparejado";
            case BluetoothDevice.BOND_BONDING: return "emparejando";
            case BluetoothDevice.BOND_NONE: return "sin emparejar";
            default: return "estado " + st;
        }
    }

    private static String wifiStandardStr(int std) {
        switch (std) {
            case ScanResult.WIFI_STANDARD_LEGACY: return "802.11 a/b/g (legacy)";
            case ScanResult.WIFI_STANDARD_11N: return "Wi-Fi 4 (802.11n)";
            case ScanResult.WIFI_STANDARD_11AC: return "Wi-Fi 5 (802.11ac)";
            case ScanResult.WIFI_STANDARD_11AX: return "Wi-Fi 6 (802.11ax)";
            case ScanResult.WIFI_STANDARD_11AD: return "802.11ad (WiGig)";
            default: return "desconocido (" + std + ")";
        }
    }

    private static String networkTypeName(int t) {
        switch (t) {
            case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS (2G)";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE (2G)";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS (3G)";
            case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA (3G)";
            case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA (3G)";
            case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA (3G)";
            case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPA+ (3G)";
            case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO rev.0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO rev.A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO rev.B";
            case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_LTE: return "LTE (4G)";
            case TelephonyManager.NETWORK_TYPE_NR: return "NR (5G)";
            case TelephonyManager.NETWORK_TYPE_GSM: return "GSM (2G)";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA: return "TD-SCDMA";
            case TelephonyManager.NETWORK_TYPE_IWLAN: return "IWLAN (WiFi calling)";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN: return "desconocido";
            default: return "tipo " + t;
        }
    }

    // ============================ HELPERS ============================

    private String globalSetting(String key) {
        try {
            return android.provider.Settings.Global.getString(ctx.getContentResolver(), key);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String safeSerial() {
        try {
            return Build.SERIAL;
        } catch (Throwable t) {
            return null;
        }
    }

    private String safeRadio() {
        try {
            return Build.getRadioVersion();
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private String legacyAbi() {
        return Build.CPU_ABI;
    }

    private String abiList() {
        if (Build.VERSION.SDK_INT >= 21) {
            return join(Build.SUPPORTED_ABIS);
        }
        return legacyAbi();
    }

    private boolean isProbablyEmulator() {
        String f = (Build.FINGERPRINT == null ? "" : Build.FINGERPRINT).toLowerCase(Locale.ROOT);
        String m = (Build.MODEL == null ? "" : Build.MODEL).toLowerCase(Locale.ROOT);
        String p = (Build.PRODUCT == null ? "" : Build.PRODUCT).toLowerCase(Locale.ROOT);
        return f.contains("generic") || f.contains("emulator") || m.contains("emulator")
                || m.contains("sdk") || p.contains("sdk") || p.contains("emulator")
                || "goldfish".equals(Build.HARDWARE) || "ranchu".equals(Build.HARDWARE);
    }

    private int countCpuCores() {
        try {
            File dir = new File("/sys/devices/system/cpu/");
            File[] files = dir.listFiles();
            if (files == null) return -1;
            int n = 0;
            for (File f : files) {
                if (f.getName().matches("cpu[0-9]+")) n++;
            }
            return n > 0 ? n : Runtime.getRuntime().availableProcessors();
        } catch (Throwable t) {
            return Runtime.getRuntime().availableProcessors();
        }
    }

    private String cpuFreqRange() {
        String min = trim(readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"));
        String max = trim(readFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
        long mn = parseLong(min), mx = parseLong(max);
        if (mn < 0 && mx < 0) return Formats.UNKNOWN;
        return Formats.khzToHuman(mn) + "  -  " + Formats.khzToHuman(mx);
    }

    private long[] statFs(String path) {
        if (path == null) return null;
        try {
            StatFs st = new StatFs(path);
            long total, avail;
            if (Build.VERSION.SDK_INT >= 18) {
                total = st.getTotalBytes();
                avail = st.getAvailableBytes();
            } else {
                total = (long) st.getBlockCount() * st.getBlockSize();
                avail = (long) st.getAvailableBlocks() * st.getBlockSize();
            }
            return new long[]{total, avail};
        } catch (Throwable t) {
            return null;
        }
    }

    private String[] queryGlStrings() {
        EGLDisplay dpy = EGL14.EGL_NO_DISPLAY;
        EGLContext ctxGl = EGL14.EGL_NO_CONTEXT;
        EGLSurface sfc = EGL14.EGL_NO_SURFACE;
        try {
            dpy = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] ver = new int[2];
            if (!EGL14.eglInitialize(dpy, ver, 0, ver, 1)) return null;
            int[] cfgAttr = {
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
            };
            EGLConfig[] cfgs = new EGLConfig[1];
            int[] num = new int[1];
            if (!EGL14.eglChooseConfig(dpy, cfgAttr, 0, cfgs, 0, 1, num, 0) || num[0] == 0) return null;
            int[] ctxAttr = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
            ctxGl = EGL14.eglCreateContext(dpy, cfgs[0], EGL14.EGL_NO_CONTEXT, ctxAttr, 0);
            int[] pbAttr = {EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE};
            sfc = EGL14.eglCreatePbufferSurface(dpy, cfgs[0], pbAttr, 0);
            if (!EGL14.eglMakeCurrent(dpy, sfc, sfc, ctxGl)) return null;
            return new String[]{
                    GLES20.glGetString(GLES20.GL_RENDERER),
                    GLES20.glGetString(GLES20.GL_VENDOR),
                    GLES20.glGetString(GLES20.GL_VERSION),
                    GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION),
                    GLES20.glGetString(GLES20.GL_EXTENSIONS)
            };
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                if (dpy != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(dpy, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                    if (sfc != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(dpy, sfc);
                    if (ctxGl != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(dpy, ctxGl);
                    EGL14.eglTerminate(dpy);
                }
            } catch (Throwable ignore) {
            }
        }
    }

    // ---- small formatting helpers ----

    private static String join(String[] arr) {
        if (arr == null || arr.length == 0) return "-";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) b.append(", ");
            b.append(arr[i]);
        }
        return b.toString();
    }

    private static String join(List<String> list) {
        return join(list.toArray(new String[0]));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static long parseLong(String s) {
        try {
            return s == null ? -1 : Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String safePath(File f) {
        return f == null ? null : f.getAbsolutePath();
    }

    private static String up(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        if ("<unknown ssid>".equals(s)) return null;
        return s;
    }

    private static String intToIp(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
    }

    private static String macToStr(byte[] mac) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) b.append(':');
            b.append(String.format(Locale.US, "%02X", mac[i]));
        }
        return b.toString();
    }

    private static String humanMillis(long ms) {
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        return d + "d " + h + "h " + m + "m " + s + "s";
    }

    private String propOf(String getprop, String key) {
        for (String line : getprop.split("\n")) {
            // format: [key]: [value]
            int i = line.indexOf("]: [");
            if (i > 1 && line.startsWith("[")) {
                String k = line.substring(1, i);
                if (k.equals(key)) {
                    String v = line.substring(i + 4);
                    if (v.endsWith("]")) v = v.substring(0, v.length() - 1);
                    return v;
                }
            }
        }
        return null;
    }

    private static String densityBucket(int dpi) {
        if (dpi <= 120) return "ldpi";
        if (dpi <= 160) return "mdpi";
        if (dpi <= 213) return "tvdpi";
        if (dpi <= 240) return "hdpi";
        if (dpi <= 320) return "xhdpi";
        if (dpi <= 480) return "xxhdpi";
        if (dpi <= 640) return "xxxhdpi";
        return "xxxhdpi+";
    }

    private static String screenSizeName(Configuration c) {
        switch (c.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL: return "pequeña";
            case Configuration.SCREENLAYOUT_SIZE_NORMAL: return "normal";
            case Configuration.SCREENLAYOUT_SIZE_LARGE: return "grande";
            case Configuration.SCREENLAYOUT_SIZE_XLARGE: return "extra grande";
            default: return Formats.UNKNOWN;
        }
    }

    private static String versionName(int sdk) {
        switch (sdk) {
            case 16: return "Android 4.1 Jelly Bean";
            case 17: return "Android 4.2 Jelly Bean";
            case 18: return "Android 4.3 Jelly Bean";
            case 19: return "Android 4.4 KitKat";
            case 20: return "Android 4.4W KitKat Wear";
            case 21: return "Android 5.0 Lollipop";
            case 22: return "Android 5.1 Lollipop";
            case 23: return "Android 6.0 Marshmallow";
            case 24: return "Android 7.0 Nougat";
            case 25: return "Android 7.1 Nougat";
            case 26: return "Android 8.0 Oreo";
            case 27: return "Android 8.1 Oreo";
            case 28: return "Android 9 Pie";
            case 29: return "Android 10";
            case 30: return "Android 11";
            case 31: return "Android 12";
            case 32: return "Android 12L";
            case 33: return "Android 13 Tiramisu";
            case 34: return "Android 14 Upside Down Cake";
            case 35: return "Android 15 Vanilla Ice Cream";
            case 36: return "Android 16 Baklava";
            default: return "API " + sdk;
        }
    }

    private static String batteryStatus(int v) {
        switch (v) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "cargando";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "descargando";
            case BatteryManager.BATTERY_STATUS_FULL: return "llena";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "sin cargar";
            default: return Formats.UNKNOWN;
        }
    }

    private static String batteryHealth(int v) {
        switch (v) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "buena";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "sobrecalentada";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "agotada";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "sobrevoltaje";
            case BatteryManager.BATTERY_HEALTH_COLD: return "fría";
            default: return Formats.UNKNOWN;
        }
    }

    private static String batteryPlugged(int v) {
        switch (v) {
            case BatteryManager.BATTERY_PLUGGED_AC: return "cargador AC";
            case BatteryManager.BATTERY_PLUGGED_USB: return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return "inalámbrico";
            case 0: return "desconectado (batería)";
            default: return Formats.UNKNOWN;
        }
    }

    private static String facingName(Integer facing) {
        if (facing == null) return Formats.UNKNOWN;
        if (facing == CameraCharacteristics.LENS_FACING_FRONT) return "frontal";
        if (facing == CameraCharacteristics.LENS_FACING_BACK) return "trasera";
        return "externa";
    }

    private static String hwLevel(Integer hw) {
        if (hw == null) return Formats.UNKNOWN;
        switch (hw) {
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY: return "legacy";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED: return "limited";
            case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL: return "full";
            default: return "nivel " + hw;
        }
    }

    private static List<String> hdrTypes(int[] types) {
        List<String> out = new ArrayList<String>();
        if (types == null) return out;
        for (int t : types) {
            switch (t) {
                case Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION: out.add("Dolby Vision"); break;
                case Display.HdrCapabilities.HDR_TYPE_HDR10: out.add("HDR10"); break;
                case Display.HdrCapabilities.HDR_TYPE_HLG: out.add("HLG"); break;
                default: out.add("tipo " + t);
            }
        }
        return out;
    }

    private static String transports(NetworkCapabilities nc) {
        List<String> out = new ArrayList<String>();
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) out.add("WiFi");
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) out.add("Celular");
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) out.add("Ethernet");
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) out.add("Bluetooth");
        if (nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) out.add("VPN");
        return out.isEmpty() ? "-" : join(out);
    }

    private static String phoneType(int t) {
        switch (t) {
            case TelephonyManager.PHONE_TYPE_GSM: return "GSM";
            case TelephonyManager.PHONE_TYPE_CDMA: return "CDMA";
            case TelephonyManager.PHONE_TYPE_SIP: return "SIP";
            case TelephonyManager.PHONE_TYPE_NONE: return "ninguno";
            default: return Formats.UNKNOWN;
        }
    }

    private static String simState(int st) {
        switch (st) {
            case TelephonyManager.SIM_STATE_ABSENT: return "ausente";
            case TelephonyManager.SIM_STATE_READY: return "lista";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED: return "PIN requerido";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED: return "PUK requerido";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED: return "bloqueada por red";
            case TelephonyManager.SIM_STATE_UNKNOWN: return "desconocido";
            default: return "estado " + st;
        }
    }

    private static String sourcesOf(InputDevice d) {
        int src = d.getSources();
        List<String> out = new ArrayList<String>();
        if ((src & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) out.add("teclado");
        if ((src & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) out.add("D-pad");
        if ((src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) out.add("gamepad");
        if ((src & InputDevice.SOURCE_TOUCHSCREEN) == InputDevice.SOURCE_TOUCHSCREEN) out.add("táctil");
        if ((src & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) out.add("ratón");
        if ((src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) out.add("joystick");
        if ((src & InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS) out.add("stylus");
        return out.isEmpty() ? ("0x" + Integer.toHexString(src)) : join(out);
    }

    // ---- I/O helpers ----

    private String readFile(String path) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line;
            int max = 512 * 1024;
            while ((line = br.readLine()) != null && sb.length() < max) {
                sb.append(line).append('\n');
            }
            return sb.length() == 0 ? null : sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            closeSilently(br);
        }
    }

    private String exec(String... cmd) {
        // Fully-qualified: android.os.Process is imported and would shadow this.
        java.lang.Process p = null;
        BufferedReader br = null;
        try {
            p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            int max = 1024 * 1024;
            while ((line = br.readLine()) != null && sb.length() < max) {
                sb.append(line).append('\n');
            }
            p.waitFor();
            return sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            closeSilently(br);
            if (p != null) {
                try { p.destroy(); } catch (Throwable ignore) { }
            }
        }
    }

    private static void closeSilently(java.io.Closeable c) {
        if (c != null) {
            try { c.close(); } catch (Throwable ignore) { }
        }
    }
}
