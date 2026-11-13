package ar.flon.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import ar.flon.tv.data.Canal
import ar.flon.tv.data.TipoFuente
import ar.flon.tv.ui.reproductores.AbrirAppYouTube
import ar.flon.tv.ui.reproductores.ReproductorExo
import ar.flon.tv.ui.reproductores.ReproductorWeb
import ar.flon.tv.ui.reproductores.ReproductorYouTube
import kotlinx.coroutines.delay

/**
 * Reproduce un canal probando sus fuentes en orden de prioridad.
 * Si una fuente falla, anota el motivo y pasa a la siguiente.
 * Si fallan todas, muestra el motivo de cada una (sirve para diagnosticar).
 */
@Composable
fun PantallaReproductor(
    canal: Canal,
    onVolver: () -> Unit,
    /** Canales de la misma categoría, para la fila de cambio rápido. */
    vecinos: List<Canal> = emptyList(),
    onCambiar: (Canal) -> Unit = {},
) {
    BackHandler(onBack = onVolver)

    val fuentes = remember(canal.id) { canal.fuentesOrdenadas }
    var indice by remember(canal.id) { mutableIntStateOf(0) }
    val errores = remember(canal.id) { mutableStateListOf<String>() }
    val fallo: (String) -> Unit = { motivo ->
        val f = fuentes.getOrNull(indice)
        if (f != null) errores += "${indice + 1}. ${f.tipo}: $motivo"
        indice++
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val fuente = fuentes.getOrNull(indice)
        if (fuente == null) {
            Column(
                Modifier.align(Alignment.Center).padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No se pudo reproducir ${canal.nombre}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                errores.forEach { Text(it, color = Color.LightGray) }
                Spacer(Modifier.height(16.dp))
                Text("Volvé con Atrás.", color = Color.Gray)
            }
        } else {
            // key: cada fuente crea su reproductor de cero y libera el anterior.
            key(canal.id, indice) {
                when (fuente.tipo) {
                    TipoFuente.HLS -> ReproductorExo(fuente, soloAudio = false, canal = canal, onError = fallo)
                    TipoFuente.AUDIO -> ReproductorExo(fuente, soloAudio = true, canal = canal, onError = fallo)
                    TipoFuente.YOUTUBE -> ReproductorYouTube(fuente, onError = fallo)
                    TipoFuente.WEB -> ReproductorWeb(fuente, onError = fallo)
                    TipoFuente.YOUTUBE_APP -> AbrirAppYouTube(fuente, onError = fallo)
                    else -> LaunchedEffect(Unit) { fallo("tipo desconocido") }
                }
            }
            CartelCanal(canal, indice, fuentes.size, fuente.tipo)
        }

        // Fila de cambio rápido (flecha abajo), en canales y radios.
        if (vecinos.size > 1) {
            FilaCambio(actual = canal, canales = vecinos, titulo = canal.categoria, onCambiar = onCambiar)
        }
    }
}

/** Cartel con el nombre del canal que se ve unos segundos al entrar o al cambiar de fuente. */
@Composable
private fun CartelCanal(canal: Canal, indice: Int, total: Int, tipo: String) {
    var visible by remember(canal.id, indice) { mutableStateOf(true) }
    LaunchedEffect(canal.id, indice) { delay(4000); visible = false }
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(32.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(canal.nombre, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                if (total > 1 || indice > 0) {
                    Text("Fuente ${indice + 1} de $total · $tipo", color = Color.LightGray)
                }
                Text("↓ para cambiar de canal", color = Color.Gray)
            }
        }
    }
}
