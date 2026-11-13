package ar.flon.tv.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import ar.flon.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Request
import java.io.File

/** Última versión publicada en GitHub (release "build-N"). */
data class Version(val build: Int, val urlApk: String, val bytes: Long)

/**
 * Busca, baja e instala versiones nuevas desde las releases del repo de GitHub
 * que arma el workflow de Actions.
 */
class Actualizador(private val context: Context) {

    private val carpeta get() = File(context.cacheDir, "actualizaciones").apply { mkdirs() }

    /** Devuelve la última versión si es más nueva que la instalada; null si ya está al día. */
    suspend fun buscar(): Version? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${BuildConfig.REPO}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .build()
        val texto = Http.cliente.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("GitHub respondió HTTP ${r.code}")
            r.body?.string() ?: error("respuesta vacía")
        }
        val json = Json.parseToJsonElement(texto).jsonObject
        val tag = json["tag_name"]?.jsonPrimitive?.content.orEmpty()
        val build = tag.removePrefix("build-").toIntOrNull() ?: error("tag inesperado: $tag")
        val apk = json["assets"]?.jsonArray
            ?.map { it.jsonObject }
            ?.firstOrNull { it["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true }
            ?: error("la release no tiene APK")
        val url = apk["browser_download_url"]!!.jsonPrimitive.content
        val bytes = apk["size"]?.jsonPrimitive?.long ?: 0L
        if (build > BuildConfig.BUILD) Version(build, url, bytes) else null
    }

    /** Baja el APK informando el progreso (0..100). */
    suspend fun descargar(v: Version, progreso: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        carpeta.listFiles()?.forEach { it.delete() }
        val destino = File(carpeta, "tv-flon-${v.build}.apk")
        val req = Request.Builder().url(v.urlApk).build()
        Http.cliente.newBuilder()
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            .newCall(req).execute().use { r ->
                if (!r.isSuccessful) error("descarga: HTTP ${r.code}")
                val cuerpo = r.body ?: error("descarga vacía")
                val total = cuerpo.contentLength().takeIf { it > 0 } ?: v.bytes
                cuerpo.byteStream().use { entrada ->
                    destino.outputStream().use { salida ->
                        val buf = ByteArray(64 * 1024)
                        var leidos = 0L
                        var ultimo = -1
                        while (true) {
                            val n = entrada.read(buf)
                            if (n < 0) break
                            salida.write(buf, 0, n)
                            leidos += n
                            if (total > 0) {
                                val pct = (leidos * 100 / total).toInt()
                                if (pct != ultimo) { ultimo = pct; progreso(pct) }
                            }
                        }
                    }
                }
            }
        destino
    }

    /** ¿La app tiene permiso para instalar? (Android 8+ lo pide por app.) */
    fun puedeInstalar(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla del sistema para habilitar "instalar apps desconocidas" para TV Floñ. */
    fun pedirPermiso(): Boolean = try {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false // algunos Fire TV no tienen esa pantalla: hay que hacerlo a mano en Configuración
    }

    /** Abre el instalador del sistema con el APK descargado. */
    fun instalar(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
