package ar.flon.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import ar.flon.tv.BuildConfig
import ar.flon.tv.EstadoActualizacion
import kotlinx.coroutines.delay

/**
 * Botón ☰ arriba a la derecha. Si hay una versión nueva, muestra un punto naranja.
 */
@Composable
fun BotonMenu(hayNovedad: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        OutlinedButton(onClick = onClick) {
            Text("☰  Menú")
        }
        if (hayNovedad) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(Color(0xFFFFB547), CircleShape)
            )
        }
    }
}

/**
 * Panel lateral de configuración. Cada sección es un bloque; para sumar funciones
 * en el futuro alcanza con agregar otra Seccion(...) en la columna.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PanelMenu(
    abierto: Boolean,
    onCerrar: () -> Unit,
    actualizacion: EstadoActualizacion,
    onActualizar: () -> Unit,
    totalCanales: Int,
    listaVerificada: String?,
) {
    BackHandler(enabled = abierto, onBack = onCerrar)

    Box(Modifier.fillMaxSize()) {
        // Fondo oscurecido; tocarlo (o Atrás) cierra.
        AnimatedVisibility(abierto, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCerrar,
                    )
            )
        }
        AnimatedVisibility(
            abierto,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            val foco = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(150)
                runCatching { foco.requestFocus() }
            }
            Column(
                Modifier
                    // El foco no se escapa del panel hacia la grilla de atrás.
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusGroup()
                    .fillMaxHeight()
                    .width(420.dp)
                    .background(Color(0xFF151C2B))
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                Text("Menú", style = MaterialTheme.typography.headlineSmall, color = Color.White)

                Seccion("Actualización") {
                    val (texto, destacado) = textoBoton(actualizacion)
                    val mod = Modifier.focusRequester(foco)
                    if (destacado) Button(onClick = onActualizar, modifier = mod) { Text(texto) }
                    else OutlinedButton(onClick = onActualizar, modifier = mod) { Text(texto) }
                    notaActualizacion(actualizacion)?.let {
                        Text(it, color = Color(0xFFB8C2D6), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Seccion("Acerca de") {
                    Dato("Versión", BuildConfig.VERSION_NAME.substringBefore(" ("))
                    Dato("Build", if (BuildConfig.BUILD > 0) "#${BuildConfig.BUILD}" else "local")
                    Dato("Canales y radios", totalCanales.toString())
                    listaVerificada?.let { Dato("Lista verificada", it) }
                    Dato("Repositorio", BuildConfig.REPO)
                }

                Spacer(Modifier.weight(1f))
                Text("Atrás para cerrar", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(titulo.uppercase(), color = Color(0xFF7FB2FF), style = MaterialTheme.typography.labelMedium)
        contenido()
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Text(valor, color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(2.dp))
    }
}

private fun textoBoton(e: EstadoActualizacion): Pair<String, Boolean> = when (e) {
    EstadoActualizacion.Nada, EstadoActualizacion.AlDia -> "Buscar actualización" to false
    EstadoActualizacion.Buscando -> "Buscando…" to false
    is EstadoActualizacion.Disponible -> "Actualizar a build ${e.version.build}" to true
    is EstadoActualizacion.Descargando -> "Descargando ${e.porcentaje}%" to true
    is EstadoActualizacion.ListoParaInstalar -> "Instalar build ${e.build}" to true
    is EstadoActualizacion.FaltaPermiso -> "Instalar (ya di el permiso)" to true
    is EstadoActualizacion.Error -> "Reintentar" to false
}

private fun notaActualizacion(e: EstadoActualizacion): String? = when (e) {
    EstadoActualizacion.AlDia -> "Ya tenés la última versión."
    is EstadoActualizacion.Disponible -> "Hay una versión nueva en GitHub."
    is EstadoActualizacion.Error -> e.motivo.take(80)
    is EstadoActualizacion.FaltaPermiso ->
        if (e.abrioAjustes) "Habilitá TV Floñ en la pantalla que se abrió y volvé."
        else "Habilitá \"apps desconocidas\" para TV Floñ en Configuración."
    else -> null
}
