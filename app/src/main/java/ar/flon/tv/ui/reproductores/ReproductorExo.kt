package ar.flon.tv.ui.reproductores

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import ar.flon.tv.data.Canal
import ar.flon.tv.data.Fuente
import ar.flon.tv.data.Http
import ar.flon.tv.data.resolverUrl

/**
 * ExoPlayer para fuentes hls (video) y audio (radios: mp3, aac o HLS de audio).
 * Reintenta solo ante BehindLiveWindowException (muy común en vivo);
 * cualquier otro error pasa a la fuente siguiente.
 */
@OptIn(UnstableApi::class)
@Composable
fun ReproductorExo(fuente: Fuente, soloAudio: Boolean, canal: Canal, onError: () -> Unit) {
    val context = LocalContext.current

    val player = remember {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(fuente.headers["User-Agent"] ?: Http.USER_AGENT)
            .setDefaultRequestProperties(fuente.headers - "User-Agent")
            .setAllowCrossProtocolRedirects(true) // streamtheworld redirige https -> http
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(15_000)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(http))
            .build()
            .apply { playWhenReady = true }
    }

    // Resuelve la URL (pide el token si hace falta) y arranca. `intentos` sube cuando
    // un token vence: se pide uno nuevo hasta 2 veces antes de pasar a la fuente siguiente.
    var intentos by remember { mutableIntStateOf(0) }
    LaunchedEffect(intentos) {
        val url = resolverUrl(fuente)
        if (url == null) { onError(); return@LaunchedEffect }
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                } else if (fuente.tokenUrl != null && intentos < 2) {
                    intentos++
                } else {
                    onError()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false // vivo: no hace falta barra de progreso
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    if (soloAudio) visibility = View.INVISIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (soloAudio) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📻", style = MaterialTheme.typography.displayLarge)
                Text(canal.nombre, style = MaterialTheme.typography.displaySmall, color = Color.White)
            }
        }
    }
}
