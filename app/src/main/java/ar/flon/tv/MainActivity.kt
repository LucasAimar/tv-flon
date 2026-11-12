package ar.flon.tv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import ar.flon.tv.ui.PantallaInicio
import ar.flon.tv.ui.PantallaReproductor

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                App(vm, onPantallaEncendida = ::pantallaEncendida)
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
            c != null -> PantallaReproductor(canal = c, onVolver = vm::cerrar)
            else -> {
                PantallaInicio(
                    lista = l,
                    favoritos = favoritos,
                    categoriaElegida = categoria,
                    canalEnfocado = ultimo,
                    onCategoria = vm::elegirCategoria,
                    onAbrir = vm::abrir,
                    onFavorito = vm::alternarFavorito,
                )
                // Versión instalada, en un rincón.
                Text(
                    "v${BuildConfig.VERSION_NAME}",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
