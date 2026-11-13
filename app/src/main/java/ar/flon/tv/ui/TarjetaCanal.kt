package ar.flon.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import ar.flon.tv.data.Canal
import coil.compose.AsyncImage

/**
 * Tarjeta de un canal o radio: logo sobre fondo claro (casi todos los logos están pensados
 * para fondo blanco) y el nombre abajo. Si no hay logo o no carga, el nombre va en grande.
 * Se usa en la grilla de inicio y en la fila de cambio rápido.
 */
@Composable
fun TarjetaCanal(
    canal: Canal,
    modifier: Modifier = Modifier,
    esFavorito: Boolean = false,
    activo: Boolean = false,
    compacta: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    var logoFallo by remember(canal.id) { mutableStateOf(false) }
    val conLogo = !logoFallo
    val acento = if (canal.esRadio) Color(0xFFFFB547) else Color(0xFF7FB2FF)

    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.aspectRatio(16f / 9f),
        scale = CardDefaults.scale(focusedScale = if (compacta) 1.08f else 1.12f),
        border = CardDefaults.border(
            border = if (activo) Border(BorderStroke(3.dp, acento)) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, Color.White)),
        ),
        colors = CardDefaults.colors(
            containerColor = if (canal.esRadio) Color(0xFF3A2E1B) else Color(0xFF1B2A49),
            focusedContainerColor = if (canal.esRadio) Color(0xFF6B5326) else Color(0xFF2E4A7D),
        ),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (conLogo) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
                            .background(Color(0xFFF2F4F7), RoundedCornerShape(6.dp))
                            .padding(if (compacta) 6.dp else 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LogoCanal(
                            canal = canal,
                            modifier = Modifier.fillMaxSize(),
                            onSinLogo = { logoFallo = true },
                        )
                    }
                    Text(
                        canal.nombre,
                        style = if (compacta) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            } else {
                Text(
                    canal.nombre,
                    style = if (compacta) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                )
            }
            if (esFavorito) {
                Text(
                    "★",
                    color = Color(0xFFFFB547),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
            if (activo) {
                Text(
                    "▶",
                    color = acento,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }
        }
    }
}
