package ar.flon.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                App(vm)
            }
        }
    }
}

@Composable
private fun App(vm: MainViewModel) {
    val lista by vm.lista.collectAsStateWithLifecycle()
    val canal by vm.canalActual.collectAsStateWithLifecycle()
    val favoritos by vm.favoritos.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        val l = lista
        when {
            l == null -> Text(
                "Cargando canales…",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.headlineSmall,
            )
            canal != null -> PantallaReproductor(canal = canal!!, onVolver = vm::cerrar)
            else -> PantallaInicio(
                lista = l,
                favoritos = favoritos,
                onAbrir = vm::abrir,
                onFavorito = vm::alternarFavorito,
            )
        }
    }
}
