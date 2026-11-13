package ar.flon.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.Request

/**
 * Arma la URL final de una fuente:
 * - {dispositivo}: identificador fijo de este aparato (lo piden servicios como Pluto TV);
 * - {sesion}: identificador nuevo en cada reproducción;
 * - si hay tokenUrl, pide el JSON y completa los marcadores {campo} con sus valores.
 * Devuelve la URL lista, o lanza una excepción con el motivo (que se muestra en pantalla).
 */
suspend fun resolverUrl(fuente: Fuente): String = withContext(Dispatchers.IO) {
    val base = (fuente.url ?: error("sin URL"))
        .replace("{dispositivo}", Identificadores.dispositivo)
        .replace("{sesion}", java.util.UUID.randomUUID().toString())
    val tokenUrl = fuente.tokenUrl ?: return@withContext base
    val req = Request.Builder().url(tokenUrl).apply {
        // Mismos Referer/Origin que usa la página oficial.
        fuente.headers.forEach { (k, v) -> header(k, v) }
    }.build()
    val texto = Http.cliente.newCall(req).execute().use { r ->
        if (!r.isSuccessful) error("token: HTTP ${r.code}")
        r.body?.string() ?: error("token: respuesta vacía")
    }
    val obj = try {
        Json.parseToJsonElement(texto).jsonObject
    } catch (e: Exception) {
        error("token: respuesta no es JSON (${texto.take(40)})")
    }
    var url = base
    obj.forEach { (k, v) -> if (v is JsonPrimitive) url = url.replace("{$k}", v.content) }
    if ("{" in url) error("token: faltan campos en la respuesta")
    url
}
