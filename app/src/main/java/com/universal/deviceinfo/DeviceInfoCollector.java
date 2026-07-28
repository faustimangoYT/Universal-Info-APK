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
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.InputDevice;
import android.view.WindowManager;

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
        return s;
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
