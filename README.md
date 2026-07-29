# Universal Device Info

APK **universal** de información del dispositivo con **interfaz estética para TV**.
Al abrirla muestra *toda* la información que Android permite obtener y escribe un
`.txt` con ese informe completo en la **raíz de los volúmenes secundarios**
(pendrive / tarjeta SD), reemplazándolo en cada apertura.

- **Compatibilidad:** Android 4.0 (API 14) → Android 15/16. Se instala en TV,
  teléfonos, tablets y TV-boxes.
- **Interfaz TV:** tarjetas navegables con mando (D-pad), foco resaltado, tipografía
  grande y márgenes seguros para overscan. Aparece tanto en el launcher normal como
  en la pantalla de inicio de **Android TV** (`LEANBACK_LAUNCHER`).
- **Sin dependencias externas** (solo framework de Android) → APK de ~85 KB y máxima
  compatibilidad, sin AndroidX ni floors de `minSdk`.
- **Máxima portabilidad:** un único APK universal, **sin código nativo**, así que
  corre en cualquier arquitectura (ARM, ARM64, x86, x86_64, …) y en cualquier
  densidad de pantalla. `installLocation=auto` permite instalarlo incluso en la SD.

## Descargar / Instalar

- APK ya compilado y firmado (debug, instalable directo):
  [`dist/UniversalDeviceInfo-v1.0-debug.apk`](dist/UniversalDeviceInfo-v1.0-debug.apk)
- En TV: copiá el APK a un pendrive y abrilo con un explorador de archivos, o usá
  `adb install UniversalDeviceInfo-v1.0-debug.apk`.
- Hay que permitir "orígenes desconocidos" / "instalar apps desconocidas".
- Cada push a GitHub también compila el APK por CI (ver *Actions* → artefactos
  `UniversalDeviceInfo-debug`).

### Página de descarga para TV Box

[`download/index.html`](download/index.html) es una **página de descarga estilo ficha
de tienda** (layout tipo Google Play, tipografía Roboto, icono, capturas y "Acerca de
esta app"), **auto-contenida**: el APK, la fuente y las imágenes van **embebidos**
(data URI base64), así que no necesita servidor ni internet. El botón verde
**"Instalar"** queda enfocado para navegar con el control remoto y descarga el APK
(también intenta arrancar la descarga sola al abrir). Verificado en navegador real:
renderiza con Roboto y la descarga produce un APK **byte-idéntico** al original. Para
usarla en la TV, hospedá ese único archivo en cualquier URL (Netlify, Vercel, GitHub
Pages…) y abrí el enlace en el navegador del TV box.

> Es un homenaje visual al estilo de ficha de tienda para **tu propia app**; no usa la
> marca ni el logo de Google Play ni afirma ser la tienda oficial.

## Qué información muestra (regla "MÁS = MEJOR")

22 secciones, cada una tolerante a fallos (si un dato no está disponible, sigue con
el resto):

1. **Identidad** — fabricante, marca, modelo, nombre en Ajustes, device, producto,
   board, hardware, SoC (API 31+), serial, bootloader, banda base, fingerprint,
   detección de emulador.
2. **Sistema Android** — versión + nombre comercial, API level, parche de seguridad,
   kernel, fecha de build, VM, 64-bit, ABIs.
3. **CPU** — núcleos (runtime y sysfs), arquitectura, ABIs 32/64, hardware/modelo
   desde `/proc/cpuinfo`, features/flags, frecuencia mín/máx, governor.
4. **RAM** — total/disponible/umbral, `/proc/meminfo` (Mem/Swap/Buffers/Cached),
   clase de memoria, heap de la app, native heap.
5. **Almacenamiento** — cada volumen (principal/secundario/extraíble/emulado),
   estado, total/libre/usado con `StatFs`.
6. **Pantalla** — resolución usable y real, densidad, dpi, refresco, modos
   soportados, HDR, pulgadas físicas, categoría, modo TV.
7. **GPU / OpenGL** — versión GLES, y `GL_RENDERER/VENDOR/VERSION/GLSL` +
   extensiones vía contexto EGL real.
8. **Batería** — nivel, estado, salud, tecnología, voltaje, temperatura, capacidad,
   corriente (BatteryManager).
9. **Cámaras** — enumeración Camera2 (orientación, flash, nivel HW, sensor, resolución).
10. **Sensores** — lista completa con vendor, potencia, resolución, rango, wake-up.
11. **Conectividad** — red activa, transporte, ancho de banda, WiFi (SSID/BSSID/RSSI/IP).
12. **Interfaces de red** — cada interfaz con MAC, MTU e IPs (IPv4/IPv6).
13. **Telefonía / SIM** — operador, país, tipo, estado de SIM (lo accesible sin permisos peligrosos).
14. **Bluetooth** — nombre, dirección, estado.
15. **Audio** — volúmenes, sample rate, buffer, salidas.
16. **Entrada** — teclados, D-pad, gamepads, táctil, ratón (útil en TV).
17. **Códecs multimedia** — lista completa de encoders/decoders y sus MIME types.
18. **Características del sistema** — todas las `android.hardware/software.*`.
19. **Apps instaladas** — total, del sistema vs usuario, y lista completa.
20. **Idioma / Región / Hora** — locale, zona horaria, uptime, arranque, charset.
21. **Propiedades del sistema** — volcado completo de `getprop`.
22. **Entorno** — VM, versiones Java, variables de entorno, rutas.

## El `.txt` en la raíz del USB/SD (regla 2) — comportamiento honesto por versión

El archivo se llama **`InformacionDispositivo.txt`** y se **reemplaza** en cada
apertura, dentro de una carpeta **`UniversalDeviceInfo/`**. El destino es siempre
**el disco que NO tiene el sistema operativo** (el disco secundario: pendrive o SD).
No apunta a la raíz exacta, sino a la **carpeta escribible más cercana a la raíz**:
la app recorre las carpetas desde la raíz hacia adentro y usa la primera donde
realmente puede escribir. **Al abrir, la app dice la ruta exacta** donde guardó (en
la primera tarjeta, en la línea de estado y en un aviso emergente). También hay un
botón **"Carpeta USB/SD"** (SAF) para elegir la carpeta a mano.

| Escenario | Carpeta usada (lo más cerca de la raíz posible) |
|---|---|
| **Android ≤ 10** / rooted / ROM permisiva | `…/UniversalDeviceInfo/` **pegado a la raíz** del USB/SD (nivel 1). |
| **Android 11+ de fábrica** | `…/Android/data/<app>/…/UniversalDeviceInfo/` en el **mismo** USB/SD (lo más cerca que permite el SO), o la raíz exacta con el botón *Carpeta USB/SD* (SAF). |
| **Sin USB/SD conectado** | copia interna de emergencia + aviso en pantalla para conectar un disco secundario. |

> **Nunca** se vuelca el archivo en la raíz del almacenamiento interno (el disco del
> SO). Escribir en la raíz *exacta* de un medio extraíble en Android 11+ no lo permite
> el sistema a ninguna app sin SAF/root: por eso la app usa la carpeta escribible más
> cercana a la raíz y te muestra dónde quedó.

| Escenario | ¿Escribe en la RAÍZ del USB/SD? | Detalle |
|---|---|---|
| **Android ≤ 10** (API ≤ 29) | ✅ Directo | *Legacy storage*: raíz escribible con `WRITE_EXTERNAL_STORAGE`. |
| **TV-box / dispositivo con root** (cualquier versión) | ✅ Directo o vía `su` | Muchas TV-boxes montan el USB accesible o tienen root. |
| **Android 11+ de fábrica** (teléfono/tablet) | ⚠️ Lo bloquea el SO | Cae a la carpeta de la app en el **mismo** USB/SD, **o** usá el botón *Carpeta USB/SD* (SAF) para escribir en la raíz exacta. |

> **Por qué:** desde Android 11, escribir en la raíz *exacta* de un medio extraíble
> no lo permite el sistema operativo a **ninguna** app sin SAF o root — es una
> restricción de Android verificada en su documentación oficial, no una limitación
> del código. Por eso la estrategia por capas + SAF + intento con root, para
> maximizar el éxito en **todas** las versiones.

## Compilar desde el código

Requiere JDK 17+ y el Android SDK (build-tools 35, platform 35).

```bash
./gradlew assembleDebug     # APK instalable (firmado con la clave debug)
./gradlew assembleRelease   # APK release sin firmar
# salida: app/build/outputs/apk/
```

`local.properties` con `sdk.dir=/ruta/al/android-sdk` o la variable `ANDROID_HOME`.

## Configuración clave

| Ajuste | Valor | Motivo |
|---|---|---|
| `minSdk` | 14 | Máxima compatibilidad; APIs nuevas usadas tras guardas `SDK_INT`. |
| `targetSdk` | 29 | Mantiene *legacy storage* en Android ≤10 y visibilidad total de apps. |
| `compileSdk` | 35 | Última plataforma estable. |
| Dependencias | 0 | Solo framework: APK mínimo y universal. |

## Estructura

```
app/src/main/java/com/universal/deviceinfo/
  MainActivity.java        # orquestación, permisos, SAF, render
  DeviceInfoCollector.java # recolección de las 22 secciones (guardas SDK_INT)
  StorageWriter.java       # escritura por capas (raíz → su → app-dir) + SAF
  VolumeUtil / VolumeInfo  # enumeración de volúmenes multi-estrategia
  UiBuilder.java           # UI de TV construida en código (tarjetas con foco)
  ProcParser / Formats / ReportBuilder / StoragePaths  # lógica pura (con tests)
```

La lógica pura (formateo, parseo de `/proc`, armado del informe, rutas) está cubierta
por un set de pruebas JVM (`40/40`) y verificada contra datos reales de `/proc`.
