package ar.flon.tv.ui

import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import ar.flon.tv.data.Canal
import kotlinx.coroutines.delay

private const val SEGUNDOS_VISIBLE = 8

/**
 * Fila para cambiar de canal o radio sin salir de pantalla completa.
 * - Flecha abajo (o arriba / OK / Menú) mientras se reproduce: aparece abajo.
 * - Izquierda/derecha para elegir, OK para cambiar (la fila queda abierta unos segundos).
 * - Atrás o 8 s sin tocar nada: se cierra.
 */
@Composable
fun FilaCambio(
    actual: Canal,
    canales: List<Canal>,
    titulo: String,
    onCambiar: (Canal) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var ultimaTecla by remember { mutableLongStateOf(0L) }

    EscucharTeclas { e ->
        if (visible) {
            // Con la fila abierta, las teclas siguen su camino normal (mover el foco, OK).
            ultimaTecla = SystemClock.elapsedRealtime()
            false
        } else if (e.keyCode in TECLAS_ABRIR) {
            // Se consumen tanto el "apretar" como el "soltar" para que no lleguen al video.
            if (e.action == KeyEvent.ACTION_DOWN) {
                visible = true
                ultimaTecla = SystemClock.elapsedRealtime()
            }
            true
        } else {
            false
        }
    }

    // Atrás cierra la fila antes que salir del canal (este BackHandler va después, gana).
    BackHandler(enabled = visible) { visible = false }

    LaunchedEffect(visible, ultimaTecla) {
        if (visible) {
            delay(SEGUNDOS_VISIBLE * 1000L)
            if (SystemClock.elapsedRealtime() - ultimaTecla >= SEGUNDOS_VISIBLE * 1000L - 50) visible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            val indiceActual = canales.indexOfFirst { it.id == actual.id }.coerceAtLeast(0)
            val foco = remember { FocusRequester() }
            val estado = rememberLazyListState(initialFirstVisibleItemIndex = (indiceActual - 2).coerceAtLeast(0))
            LaunchedEffect(Unit) {
                delay(100)
                runCatching { foco.requestFocus() }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000))))
                    .padding(top = 48.dp, bottom = 28.dp),
            ) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 48.dp, bottom = 12.dp),
                )
                LazyRow(
                    state = estado,
                    contentPadding = PaddingValues(horizontal = 48.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(canales, key = { _, c -> c.id }) { i, c ->
                        TarjetaCanal(
                            canal = c,
                            activo = c.id == actual.id,
                            compacta = true,
                            modifier = Modifier
                                .width(200.dp)
                                .then(if (i == indiceActual) Modifier.focusRequester(foco) else Modifier),
                            onClick = {
                                ultimaTecla = SystemClock.elapsedRealtime()
                                if (c.id != actual.id) onCambiar(c)
                            },
                        )
                    }
                }
            }
        }
    }
}

private val TECLAS_ABRIR = setOf(
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_MENU,
)
