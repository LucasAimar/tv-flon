package ar.flon.tv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import ar.flon.tv.BuildConfig
import ar.flon.tv.EstadoActualizacion

/**
 * Botón arriba a la derecha (a la derecha de las pestañas): muestra la versión y,
 * según el estado, busca, baja o instala la última versión publicada en GitHub.
 */
@Composable
fun BotonActualizar(estado: EstadoActualizacion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (texto, destacado) = when (estado) {
        EstadoActualizacion.Nada, EstadoActualizacion.AlDia -> "Buscar actualización" to false
        EstadoActualizacion.Buscando -> "Buscando…" to false
        is EstadoActualizacion.Disponible -> "Actualizar a build ${estado.version.build}" to true
        is EstadoActualizacion.Descargando -> "Descargando ${estado.porcentaje}%" to true
        is EstadoActualizacion.ListoParaInstalar -> "Instalar build ${estado.build}" to true
        is EstadoActualizacion.FaltaPermiso -> "Instalar (ya di el permiso)" to true
        is EstadoActualizacion.Error -> "Reintentar" to false
    }
    val nota = when (estado) {
        EstadoActualizacion.AlDia -> "Ya tenés la última versión"
        is EstadoActualizacion.Error -> estado.motivo.take(60)
        is EstadoActualizacion.FaltaPermiso ->
            if (estado.abrioAjustes) "Habilitá TV Floñ en la pantalla que se abrió y volvé"
            else "Habilitá \"apps desconocidas\" para TV Floñ en Configuración"
        else -> "v${BuildConfig.VERSION_NAME}"
    }

    Column(modifier, horizontalAlignment = Alignment.End) {
        if (destacado) {
            Button(onClick = onClick) { Text(texto) }
        } else {
            OutlinedButton(onClick = onClick) { Text(texto) }
        }
        Text(
            nota,
            color = Color.Gray,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp, end = 4.dp),
        )
    }
}
