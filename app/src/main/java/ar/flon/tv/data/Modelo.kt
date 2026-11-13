package ar.flon.tv.data

import kotlinx.serialization.Serializable

/** Raíz de canales.json */
@Serializable
data class ListaCanales(
    val version: Int = 1,
    /** Fecha en que se verificaron las fuentes (se muestra en el menú). */
    val verificado: String? = null,
    val categorias: List<String> = emptyList(),
    val canales: List<Canal> = emptyList(),
)

@Serializable
data class Canal(
    val id: String,
    val nombre: String,
    val categoria: String,
    val logo: String? = null,
    val fuentes: List<Fuente> = emptyList(),
) {
    /** Fuentes en el orden en que se prueban. */
    val fuentesOrdenadas: List<Fuente> get() = fuentes.sortedBy { it.prioridad }

    /**
     * Dónde buscar el logo, en orden:
     * 1. logos/<id>.png en el repo de GitHub (lo que Lucas sube manda sobre todo lo demás);
     * 2. la dirección del campo `logo` del JSON, si la hay.
     */
    val logosCandidatos: List<String>
        get() = listOfNotNull(
            "https://raw.githubusercontent.com/${ar.flon.tv.BuildConfig.REPO}/main/logos/$id.png",
            logo,
        )

    /** Radio: está en la categoría Radios o solo tiene fuentes de audio. */
    val esRadio: Boolean
        get() = categoria == "Radios" || fuentes.all { it.tipo == TipoFuente.AUDIO }
}

@Serializable
data class Fuente(
    /** hls | youtube | web | audio */
    val tipo: String,
    val url: String? = null,
    /** Para tipo youtube: ID del canal (UC...). Se reproduce el vivo actual del canal. */
    val canalId: String? = null,
    /** Headers HTTP extra (Referer, User-Agent) que algunos HLS exigen. */
    val headers: Map<String, String> = emptyMap(),
    /** Para tipo web: fragmento de URL del iframe a extraer del HTML (ej. "mdstrm.com/live-stream/"). */
    val extraerIframe: String? = null,
    /**
     * Para hls con token (ej. Telefe): URL que devuelve un JSON. Cada campo del JSON
     * reemplaza su marcador {campo} en `url` antes de reproducir.
     * Ej.: url ".../ID.m3u8?access_token={access_token}"
     */
    val tokenUrl: String? = null,
    /** Para tipo web: selector CSS del elemento a llevar a pantalla completa (por defecto "video"). */
    val selector: String? = null,
    val prioridad: Int = 1,
    val notas: String? = null,
)

object TipoFuente {
    const val HLS = "hls"
    const val YOUTUBE = "youtube"
    const val WEB = "web"
    const val AUDIO = "audio"
    /** Abre el vivo del canal en la app oficial de YouTube del TV (sale de TV Floñ). */
    const val YOUTUBE_APP = "youtube-app"
}
