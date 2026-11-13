# TV Floñ

App personal de canales y radios para Android TV y Fire TV.
Kotlin + Compose for TV + Media3 ExoPlayer.

## Cómo está armada

```
app/src/main/
├── AndroidManifest.xml          TV + Fire TV (LEANBACK_LAUNCHER, banner, sin touch)
├── assets/canales.json          Lista incluida en el APK (respaldo)
└── java/ar/flon/tv/
    ├── MainActivity.kt          Alterna entre grilla y reproductor
    ├── MainViewModel.kt         Carga la lista, favoritos, último canal
    ├── data/
    │   ├── Modelo.kt            Canal, Fuente (hls | youtube | web | audio)
    │   ├── RepositorioCanales.kt  JSON remoto → caché → assets
    │   └── Preferencias.kt      DataStore: favoritos y último canal
    └── ui/
        ├── PantallaInicio.kt    Pestañas de categorías + grilla
        ├── PantallaReproductor.kt  Prueba las fuentes en orden, pasa a la siguiente si falla
        └── reproductores/
            ├── ReproductorExo.kt      HLS y radios
            ├── ReproductorYouTube.kt  Embed oficial apuntando al vivo del canal
            └── ReproductorWeb.kt      WebView; extrae el iframe (Telefe) o agranda el <video>
```

## Uso con el control

- **Arriba/abajo/izquierda/derecha**: moverse. El foco en una pestaña cambia la categoría.
- **OK**: abrir canal.
- **Mantener OK**: agregar o quitar de favoritos (aparece la pestaña "★ Favoritos").
- **Atrás**: volver a la grilla.
- Al abrir la app arranca en el último canal visto.

## Compilar

1. Abrir la carpeta en **Android Studio** (Ladybug o más nuevo). Va a sincronizar Gradle y bajar dependencias.
2. *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
3. El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Por línea de comandos: `./gradlew assembleDebug` (necesita el Android SDK instalado y `ANDROID_HOME` configurado).

## Compilar en GitHub (sin instalar nada)

Cada push a `main` dispara `.github/workflows/build.yml`, que compila el APK y:
- lo deja como artefacto en la pestaña **Actions**;
- lo publica como **release**. La URL fija del último APK es
  `https://github.com/LucasAimar/tv-flon/releases/latest/download/tv-flon.apk`.

## Actualizar desde la app

Arriba a la derecha de la pantalla de inicio está la versión instalada y el botón
**Buscar actualización**. Al abrirse, la app ya se fija sola si hay un build nuevo en las
releases de GitHub; si lo hay, el botón dice **Actualizar a build N**: lo baja y abre el
instalador del sistema (Android siempre pide confirmar con un clic).

La primera vez hay que permitir que TV Floñ instale apps:
- **Android TV:** el botón abre la pantalla del permiso; activalo y volvé.
- **Fire TV:** *Configuración → Mi Fire TV → Opciones para desarrolladores →
  Instalar apps desconocidas → TV Floñ: activado*.

El número de versión sale del número de compilación de GitHub Actions (`versionCode = 100 + run_number`).

## Instalar

**Android TV**
1. En el TV: *Configuración → Preferencias del dispositivo → Información*, tocar 7 veces "Compilación" para activar opciones de desarrollador.
2. *Opciones de desarrollador → Depuración por red/USB*: activar.
3. Desde la PC: `adb connect IP_DEL_TV` y `adb install -r app-debug.apk`.

Alternativa sin PC: subir el APK a Drive y usar la app "Downloader" o "Send Files to TV".

**Fire TV Stick**
1. *Configuración → Mi Fire TV → Acerca de*, tocar 7 veces el nombre del dispositivo.
2. *Opciones para desarrolladores*: activar "Depuración ADB" y "Apps de origen desconocido".
3. `adb connect IP_DEL_FIRE` y `adb install -r app-debug.apk`, o usar la app Downloader.

## Lista de canales remota

En `app/build.gradle.kts`, cambiar `CANALES_URL` por la URL de tu `canales.json`
(un repo de GitHub con el archivo crudo, o tu propio servidor). Así se corrige un stream
caído editando el JSON, sin recompilar. Si la descarga falla, la app usa la última copia
o la incluida en el APK.

Formato de una fuente:

```json
{ "tipo": "hls", "url": "https://...m3u8", "headers": { "Referer": "https://..." }, "prioridad": 1 }
{ "tipo": "youtube", "canalId": "UC...", "prioridad": 1 }
{ "tipo": "web", "url": "https://pagina", "extraerIframe": "fragmento-del-src", "prioridad": 2 }
{ "tipo": "audio", "url": "https://...mp3", "prioridad": 1 }
```

## Pendientes conocidos

- **No se compiló todavía**: el esqueleto se escribió sin el SDK de Android a mano. Puede haber
  algún ajuste menor de imports o versiones al sincronizar en Android Studio.
- YouTube a veces rechaza embeds según el origen (errores 152/153). Si pasa, probar cambiar
  `ORIGEN` en `ReproductorYouTube.kt`.
- Telefe: el token del iframe de Mediastream puede requerir que la extracción se ajuste si
  cambian el HTML.
- Logos de canales (campo `logo` en el JSON) y banner definitivo.
- Encuentro, Pakapaka y DeporTV: sin fuente estable todavía.
