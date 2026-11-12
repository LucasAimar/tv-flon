package ar.flon.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import ar.flon.tv.ui.reproductores.ReproductorExo
import ar.flon.tv.ui.reproductores.ReproductorWeb
import ar.flon.tv.ui.reproductores.ReproductorYouTube
import kotlinx.coroutines.delay

/**
 * Reproduce un canal probando sus fuentes en orden de prioridad.
 * Si una fuente falla, pasa automáticamente a la siguiente.
 */
@Composable
fun PantallaReproductor(canal: Canal, onVolver: () -> Unit) {
    BackHandler(onBack = onVolver)

    val fuentes = remember(canal.id) { canal.fuentesOrdenadas }
    var indice by remember(canal.id) { mutableIntStateOf(0) }
    val siguiente: () -> Unit = { indice++ }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val fuente = fuentes.getOrNull(indice)
        if (fuente == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No se pudo reproducir ${canal.nombre}", style = MaterialTheme.typography.headlineSmall)
                Text("Probadas ${fuentes.size} fuente(s). Volvé con Atrás.", color = Color.Gray)
            }
        } else {
            // key: cada fuente crea su reproductor de cero y libera el anterior.
            key(canal.id, indice) {
                when (fuente.tipo) {
                    TipoFuente.HLS -> ReproductorExo(fuente, soloAudio = false, canal = canal, onError = siguiente)
                    TipoFuente.AUDIO -> ReproductorExo(fuente, soloAudio = true, canal = canal, onError = siguiente)
                    TipoFuente.YOUTUBE -> ReproductorYouTube(fuente, onError = siguiente)
                    TipoFuente.WEB -> ReproductorWeb(fuente, onError = siguiente)
                    else -> LaunchedEffect(Unit) { siguiente() }
                }
            }
            CartelCanal(canal, indice, fuentes.size, fuente.tipo)
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
            }
        }
    }
}
