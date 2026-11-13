package ar.flon.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import ar.flon.tv.ui.BotonMenu
import ar.flon.tv.ui.PanelMenu
import ar.flon.tv.ui.InterceptorTeclas
import ar.flon.tv.ui.LocalTeclas
import ar.flon.tv.ui.PantallaInicio
import ar.flon.tv.ui.PantallaReproductor

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val teclas = InterceptorTeclas()

    /** Todas las teclas pasan primero por acá (antes que un WebView de YouTube). */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        teclas.despachar(event) || super.dispatchKeyEvent(event)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalTeclas provides teclas) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    App(vm, onPantallaEncendida = ::pantallaEncendida)
                }
            }
        }
    }

    /**
     * Mientras se reproduce algo (video o radio), la pantalla no se apaga ni entra
     * el protector de pantalla. Va en la ventana, no en una vista: en las radios la
     * vista del video está oculta y por eso antes no funcionaba.
     */
    private fun pantallaEncendida(encendida: Boolean) {
        if (encendida) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun App(vm: MainViewModel, onPantallaEncendida: (Boolean) -> Unit) {
    val lista by vm.lista.collectAsStateWithLifecycle()
    val canal by vm.canalActual.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()
    val categoria by vm.categoria.collectAsStateWithLifecycle()
    val ultimo by vm.ultimoCanal.collectAsStateWithLifecycle()
    val actualizacion by vm.actualizacion.collectAsStateWithLifecycle()
    var menuAbierto by remember { mutableStateOf(false) }
    var menuUsado by remember { mutableStateOf(false) }
    val focoMenu = remember { FocusRequester() }
    // Al cerrar el menú, el foco vuelve al botón ☰.
    LaunchedEffect(menuAbierto) {
        if (menuAbierto) menuUsado = true
        else if (menuUsado) runCatching { focoMenu.requestFocus() }
    }

    val reproduciendo = canal != null
    DisposableEffect(reproduciendo) {
        onPantallaEncendida(reproduciendo)
        onDispose { onPantallaEncendida(false) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val l = lista
        val c = canal
        when {
            l == null -> Text(
                "Cargando canales…",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineSmall,
            )
            c != null -> PantallaReproductor(
                canal = c,
                onVolver = vm::cerrar,
                vecinos = l.canales.filter { it.categoria == c.categoria },
                onCambiar = vm::abrir,
            )
            else -> PantallaInicio(
                lista = l,
                favoritos = favoritos,
                categoriaElegida = categoria,
                canalEnfocado = ultimo,
                onCategoria = vm::elegirCategoria,
                onAbrir = vm::abrir,
                onFavorito = vm::alternarFavorito,
                // Menú ☰ (actualizar, versión y, a futuro, más opciones).
                extraArriba = {
                    BotonMenu(
                        hayNovedad = actualizacion is EstadoActualizacion.Disponible,
                        onClick = { menuAbierto = true },
                        modifier = Modifier.focusRequester(focoMenu),
                    )
                },
            )
        }
        if (c == null && l != null) {
            PanelMenu(
                abierto = menuAbierto,
                onCerrar = { menuAbierto = false },
                actualizacion = actualizacion,
                onActualizar = vm::accionActualizacion,
                totalCanales = l.canales.size,
                listaVerificada = l.verificado,
            )
        }
    }
}
