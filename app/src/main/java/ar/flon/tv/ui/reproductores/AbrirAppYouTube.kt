package ar.flon.tv.ui.reproductores

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import ar.flon.tv.data.Fuente

/** Apps oficiales de YouTube según el aparato. */
private val PAQUETES_YOUTUBE = listOf(
    "com.google.android.youtube.tv",     // Android TV / Google TV
    "com.amazon.firetv.youtube",         // Fire TV
    "com.amazon.firetv.youtube.tv",      // Fire TV (versión nueva)
    "com.google.android.youtube",        // celular/tablet, por si acaso
)

/**
 * Último recurso para canales de YouTube: abre el vivo del canal en la app oficial
 * de YouTube del televisor. Con Atrás se vuelve a TV Floñ.
 */
@Composable
fun AbrirAppYouTube(fuente: Fuente, onError: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val url = "https://www.youtube.com/channel/${fuente.canalId}/live"
        if (!abrir(context, url)) onError("no hay app de YouTube instalada")
    }
    Column(
        Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Abierto en la app de YouTube", style = MaterialTheme.typography.headlineSmall)
        Text("Volvé con Atrás.", color = Color.Gray)
    }
}

private fun abrir(context: Context, url: String): Boolean {
    for (paquete in PAQUETES_YOUTUBE) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(paquete)
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // probar el siguiente
        }
    }
    return false
}
