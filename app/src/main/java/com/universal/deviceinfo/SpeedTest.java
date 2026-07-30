package com.universal.deviceinfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Internet + storage speed test. Deliberately pure {@code java.net}/{@code java.io}
 * (no android.* imports) so the measurement logic is unit-testable on a plain JVM.
 * Everything degrades to -1 / null on failure — the UI shows "no se pudo medir".
 *
 * <p>Endpoints are byte-counting only (no JSON parsing): Cloudflare's speed
 * backend for down/up and ipify's plain-text endpoint for the public IP.
 */
public final class SpeedTest {

    public static final String URL_DOWN = "https://speed.cloudflare.com/__down?bytes=";
    public static final String URL_UP = "https://speed.cloudflare.com/__up";
    public static final String URL_IP = "https://api.ipify.org";

    public static final class Result {
        public String publicIp;
        public long pingMs = -1;
        public double downloadMbps = -1;
        public double uploadMbps = -1;
        public double storageWriteMBps = -1;
        public double storageReadMBps = -1;
    }

    private SpeedTest() {
    }

    private static HttpURLConnection open(String url, int timeoutMs) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(timeoutMs);
        c.setReadTimeout(timeoutMs);
        c.setInstanceFollowRedirects(true);
        c.setUseCaches(false);
        c.setRequestProperty("User-Agent", "UniversalDeviceInfo");
        return c;
    }

    /** Plain-text GET (e.g. api.ipify.org returns just the IP). */
    public static String httpGetText(String url, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = open(url, timeoutMs);
            c.setRequestMethod("GET");
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null && sb.length() < 4096) {
                sb.append(line);
            }
            r.close();
            String s = sb.toString().trim();
            return s.isEmpty() ? null : s;
        } catch (Throwable t) {
            return null;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** Latency: time from connect to first received byte. */
    public static long pingMs(String url, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            long t0 = System.nanoTime();
            c = open(url, timeoutMs);
            c.setRequestMethod("GET");
            InputStream in = c.getInputStream();
            in.read();
            long t1 = System.nanoTime();
            in.close();
            return (t1 - t0) / 1000000L;
        } catch (Throwable t) {
            return -1;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** Downloads up to {@code maxBytes} and returns throughput in Mbps. */
    public static double downloadMbps(String url, int maxBytes, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = open(url, timeoutMs);
            c.setRequestMethod("GET");
            InputStream in = new BufferedInputStream(c.getInputStream());
            byte[] buf = new byte[65536];
            long total = 0;
            long start = System.nanoTime();
            long limit = (long) timeoutMs * 1000000L;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total >= maxBytes) {
                    break;
                }
                if (System.nanoTime() - start > limit) {
                    break;
                }
            }
            long elapsed = System.nanoTime() - start;
            in.close();
            if (total <= 0 || elapsed <= 0) {
                return -1;
            }
            return (total * 8.0) / (elapsed / 1e9) / 1e6;
        } catch (Throwable t) {
            return -1;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** Uploads {@code bytes} of data and returns throughput in Mbps. */
    public static double uploadMbps(String url, int bytes, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = open(url, timeoutMs);
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.setFixedLengthStreamingMode(bytes);
            c.setRequestProperty("Content-Type", "application/octet-stream");
            byte[] buf = new byte[65536];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) i;
            }
            OutputStream out = new BufferedOutputStream(c.getOutputStream());
            long start = System.nanoTime();
            long limit = (long) timeoutMs * 1000000L;
            int sent = 0;
            while (sent < bytes) {
                int chunk = Math.min(buf.length, bytes - sent);
                out.write(buf, 0, chunk);
                sent += chunk;
                if (System.nanoTime() - start > limit) {
                    break;
                }
            }
            out.flush();
            out.close();
            c.getResponseCode();
            long elapsed = System.nanoTime() - start;
            if (sent <= 0 || elapsed <= 0) {
                return -1;
            }
            return (sent * 8.0) / (elapsed / 1e9) / 1e6;
        } catch (Throwable t) {
            return -1;
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** Sequential write+read benchmark on {@code dir}; returns {writeMB/s, readMB/s}. */
    public static double[] storageBench(File dir, int bytes) {
        File f = new File(dir, "udi_bench.tmp");
        try {
            byte[] buf = new byte[1 << 20];
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) i;
            }
            long t0 = System.nanoTime();
            FileOutputStream fos = new FileOutputStream(f);
            OutputStream os = new BufferedOutputStream(fos);
            int written = 0;
            while (written < bytes) {
                int chunk = Math.min(buf.length, bytes - written);
                os.write(buf, 0, chunk);
                written += chunk;
            }
            os.flush();
            fos.getFD().sync();
            os.close();
            long t1 = System.nanoTime();

            InputStream is = new BufferedInputStream(new FileInputStream(f));
            long read = 0;
            int n;
            while ((n = is.read(buf)) > 0) {
                read += n;
            }
            is.close();
            long t2 = System.nanoTime();

            double wsec = (t1 - t0) / 1e9;
            double rsec = (t2 - t1) / 1e9;
            if (wsec <= 0 || rsec <= 0) {
                return null;
            }
            return new double[]{written / 1e6 / wsec, read / 1e6 / rsec};
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            } catch (Throwable ignore) {
            }
        }
    }

    /** Runs the full suite. {@code cacheDir} may be null to skip the storage test. */
    public static Result runAll(File cacheDir) {
        Result r = new Result();
        r.publicIp = httpGetText(URL_IP, 8000);
        r.pingMs = pingMs(URL_DOWN + "0", 8000);
        r.downloadMbps = downloadMbps(URL_DOWN + "25000000", 25_000_000, 15000);
        r.uploadMbps = uploadMbps(URL_UP, 8_000_000, 15000);
        if (cacheDir != null) {
            double[] sb = storageBench(cacheDir, 24 * 1024 * 1024);
            if (sb != null) {
                r.storageWriteMBps = sb[0];
                r.storageReadMBps = sb[1];
            }
        }
        return r;
    }
}
