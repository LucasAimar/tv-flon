package ar.flon.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import ar.flon.tv.data.Canal
import coil.compose.AsyncImage

/**
 * Logo de un canal. Prueba en orden las direcciones de `canal.logosCandidatos`
 * (primero logos/<id>.png del repo, después el del JSON). Si ninguna carga,
 * avisa con `onSinLogo` para que quien lo usa muestre otra cosa (el nombre en grande).
 */
@Composable
fun LogoCanal(
    canal: Canal,
    modifier: Modifier = Modifier,
    onSinLogo: () -> Unit,
) {
    val candidatos = canal.logosCandidatos
    var indice by remember(canal.id, candidatos) { mutableIntStateOf(0) }
    val url = candidatos.getOrNull(indice)
    if (url == null) {
        LaunchedEffect(canal.id) { onSinLogo() }
    } else {
        AsyncImage(
            model = url,
            contentDescription = canal.nombre,
            contentScale = ContentScale.Fit,
            onError = { indice++ },
            modifier = modifier,
        )
    }
}
