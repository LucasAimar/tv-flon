package ar.flon.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.Request

/**
 * Si la fuente tiene tokenUrl, pide el JSON y completa los marcadores {campo} de la url.
 * Devuelve null si falla (y el reproductor pasa a la fuente siguiente).
 */
suspend fun resolverUrl(fuente: Fuente): String? = withContext(Dispatchers.IO) {
    val base = fuente.url ?: return@withContext null
    val tokenUrl = fuente.tokenUrl ?: return@withContext base
    try {
        val req = Request.Builder().url(tokenUrl).apply {
            // Mismos Referer/Origin que usa la página oficial.
            fuente.headers.forEach { (k, v) -> header(k, v) }
        }.build()
        val texto = Http.cliente.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@withContext null
            r.body?.string()
        } ?: return@withContext null
        val obj: JsonObject = Json.parseToJsonElement(texto).jsonObject
        var url = base
        obj.forEach { (k, v) -> if (v is JsonPrimitive) url = url.replace("{$k}", v.content) }
        url.takeIf { "{" !in it }
    } catch (e: Exception) {
        null
    }
}
