package ar.flon.tv.data

import kotlinx.serialization.Serializable

/** Raíz de canales.json */
@Serializable
data class ListaCanales(
    val version: Int = 1,
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

    /** Un canal es "radio" si su primera fuente es solo audio. */
    val esRadio: Boolean get() = fuentesOrdenadas.firstOrNull()?.tipo == TipoFuente.AUDIO
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
    val prioridad: Int = 1,
    val notas: String? = null,
)

object TipoFuente {
    const val HLS = "hls"
    const val YOUTUBE = "youtube"
    const val WEB = "web"
    const val AUDIO = "audio"
}
