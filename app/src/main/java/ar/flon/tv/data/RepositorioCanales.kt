package ar.flon.tv.data

import android.content.Context
import android.util.Log
import ar.flon.tv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Carga la lista de canales en este orden:
 * 1. JSON remoto (BuildConfig.CANALES_URL) -> se guarda como caché
 * 2. Última copia en caché
 * 3. canales.json incluido en el APK (assets)
 */
class RepositorioCanales(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = File(context.filesDir, "canales.json")

    suspend fun cargar(): ListaCanales = withContext(Dispatchers.IO) {
        descargar()?.let { texto ->
            parsear(texto)?.let { lista ->
                cache.writeText(texto)
                return@withContext lista
            }
        }
        if (cache.exists()) parsear(cache.readText())?.let { return@withContext it }
        val incluido = context.assets.open("canales.json").bufferedReader().use { it.readText() }
        parsear(incluido) ?: ListaCanales()
    }

    private fun descargar(): String? = try {
        val req = Request.Builder().url(BuildConfig.CANALES_URL).build()
        Http.cliente.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo descargar la lista remota: ${e.message}")
        null
    }

    private fun parsear(texto: String): ListaCanales? = try {
        json.decodeFromString<ListaCanales>(texto).takeIf { it.canales.isNotEmpty() }
    } catch (e: Exception) {
        Log.w(TAG, "JSON inválido: ${e.message}")
        null
    }

    companion object { private const val TAG = "RepositorioCanales" }
}

/** Cliente HTTP compartido (lista de canales y extracción de iframes). */
object Http {
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Safari/537.36"

    val cliente: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()
}
