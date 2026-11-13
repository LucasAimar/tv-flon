package ar.flon.tv.ui.reproductores

import android.os.SystemClock
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.delay

/**
 * ExoPlayer para fuentes hls (video) y audio (radios: mp3, aac o HLS de audio).
 * Reintenta solo ante BehindLiveWindowException (muy común en vivo);
 * cualquier otro error pasa a la fuente siguiente.
 */
@OptIn(UnstableApi::class)
@Composable
fun ReproductorExo(fuente: Fuente, soloAudio: Boolean, canal: Canal, onError: (String) -> Unit) {
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

    // Resuelve la URL (pide el token si hace falta) y arranca. `intentos` sube para
    // reconectar: token vencido, o un vivo que ya sonaba y se cortó.
    var intentos by remember { mutableIntStateOf(0) }
    LaunchedEffect(intentos) {
        if (intentos > 0) delay(2_000) // pausa corta antes de reconectar
        val url = try {
            resolverUrl(fuente)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e.message ?: e.javaClass.simpleName)
            return@LaunchedEffect
        }
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
    }

    DisposableEffect(player) {
        // Reconexión: si la fuente ya llegó a sonar, un corte no la descarta; se reconecta
        // hasta 5 veces seguidas. El contador vuelve a 0 tras un minuto sonando bien.
        // (Las radios de StreamTheWorld cierran la conexión cada tanto.)
        var yaSono = false
        var reconexiones = 0
        var sonandoDesde = 0L

        fun reconectar(): Boolean {
            if (!yaSono) return false
            if (sonandoDesde > 0 && SystemClock.elapsedRealtime() - sonandoDesde > 60_000) reconexiones = 0
            if (reconexiones >= 5) return false
            reconexiones++
            sonandoDesde = 0L
            intentos++
            return true
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    yaSono = true
                    sonandoDesde = SystemClock.elapsedRealtime()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                // Un stream en vivo no "termina": si el servidor cerró la conexión, reconectar.
                if (state == Player.STATE_ENDED && !reconectar()) onError("el stream terminó")
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                } else if (fuente.tokenUrl != null && intentos < 2) {
                    intentos++
                } else if (!reconectar()) {
                    onError(describir(error))
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
            // Radio: logo grande sobre fondo claro (o un ícono si no hay logo) y el nombre.
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                var logoFallo by remember(canal.logoUrl) { mutableStateOf(false) }
                if (!logoFallo) {
                    Box(
                        Modifier
                            .size(width = 420.dp, height = 240.dp)
                            .background(Color(0xFFF2F4F7), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = canal.logoUrl,
                            contentDescription = canal.nombre,
                            contentScale = ContentScale.Fit,
                            onError = { logoFallo = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Text("📻", style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.height(24.dp))
                Text(canal.nombre, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text("↓ para cambiar de radio", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

/** Texto corto del error de ExoPlayer, con el código HTTP si lo hay. */
@OptIn(UnstableApi::class)
private fun describir(e: PlaybackException): String {
    var causa: Throwable? = e.cause
    while (causa != null) {
        if (causa is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
            return "HTTP ${causa.responseCode} (${e.errorCodeName})"
        }
        causa = causa.cause
    }
    return e.errorCodeName + (e.cause?.message?.let { " · " + it.take(60) } ?: "")
}
