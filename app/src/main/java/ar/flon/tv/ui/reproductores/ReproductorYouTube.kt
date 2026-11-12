package ar.flon.tv.ui.reproductores

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import ar.flon.tv.data.Fuente

/**
 * Reproduce el vivo actual de un canal de YouTube con el reproductor embebido oficial
 * (IFrame API, videoId "live_stream" + channel). No hace falta conocer el ID del video:
 * siempre apunta a lo que el canal esté transmitiendo.
 *
 * Si YouTube responde con error (canal sin vivo, embed bloqueado) o no arranca en 25 s,
 * se llama a onError y se prueba la fuente siguiente.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReproductorYouTube(fuente: Fuente, onError: () -> Unit) {
    val onErrorActual = rememberUpdatedState(onError)
    val canalId = fuente.canalId.orEmpty()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.BLACK)
                keepScreenOn = true
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun error(codigo: Int) {
                        Handler(Looper.getMainLooper()).post { onErrorActual.value() }
                    }
                }, "Android")

                // El origen https es necesario: YouTube rechaza embeds sin un referer/origen válido.
                loadDataWithBaseURL(ORIGEN, html(canalId), "text/html", "utf-8", null)
                requestFocus()
            }
        },
        onRelease = { it.destroy() },
    )
}

private const val ORIGEN = "https://ar.flon.tv"

private fun html(canalId: String) = """
<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style>
</head><body>
<div id="p"></div>
<script>
  var arranco = false;
  setTimeout(function(){ if(!arranco) Android.error(-1); }, 25000);
  var tag = document.createElement('script');
  tag.src = "https://www.youtube.com/iframe_api";
  document.head.appendChild(tag);
  function onYouTubeIframeAPIReady() {
    new YT.Player('p', {
      width: '100%', height: '100%',
      videoId: 'live_stream',
      playerVars: { channel: '$canalId', autoplay: 1, controls: 0, rel: 0,
                    playsinline: 1, iv_load_policy: 3, origin: '$ORIGEN' },
      events: {
        onReady: function(e){ e.target.playVideo(); },
        onStateChange: function(e){ if (e.data === YT.PlayerState.PLAYING) arranco = true; },
        onError: function(e){ Android.error(e.data); }
      }
    });
  }
</script>
</body></html>
""".trimIndent()
