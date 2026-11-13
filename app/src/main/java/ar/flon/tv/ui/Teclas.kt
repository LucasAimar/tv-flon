package ar.flon.tv.ui

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Teclas del control vistas antes que nadie (incluido un WebView de YouTube, que si no
 * se queda con las flechas). La Activity las pasa por acá en dispatchKeyEvent.
 */
class InterceptorTeclas {
    private val escuchas = mutableListOf<(KeyEvent) -> Boolean>()

    fun registrar(f: (KeyEvent) -> Boolean) { escuchas += f }
    fun quitar(f: (KeyEvent) -> Boolean) { escuchas -= f }

    /** true = la tecla se consumió y no sigue camino. */
    fun despachar(e: KeyEvent): Boolean = escuchas.toList().asReversed().any { it(e) }
}

val LocalTeclas = staticCompositionLocalOf { InterceptorTeclas() }

/** Escucha teclas mientras el composable esté en pantalla. */
@Composable
fun EscucharTeclas(alRecibir: (KeyEvent) -> Boolean) {
    val teclas = LocalTeclas.current
    val actual = rememberUpdatedState(alRecibir)
    DisposableEffect(teclas) {
        val f: (KeyEvent) -> Boolean = { actual.value(it) }
        teclas.registrar(f)
        onDispose { teclas.quitar(f) }
    }
}
