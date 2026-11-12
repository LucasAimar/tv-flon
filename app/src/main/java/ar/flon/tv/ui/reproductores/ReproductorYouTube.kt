package ar.flon.tv.ui.reproductores

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import ar.flon.tv.data.Fuente
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.delay

/**
 * Reproduce el vivo actual de un canal de YouTube abriendo directamente el reproductor
 * embebido oficial (www.youtube.com/embed/live_stream?channel=...).
 *
 * Cambios respecto de la v0.1 (se escuchaba pero no se veía):
 * - Se carga la URL del embed directo, sin una página propia con un iframe adentro.
 * - WebView en capa de hardware, para que pueda dibujar el video.
 * - Si el reproductor pide pantalla completa (onShowCustomView), se muestra esa vista;
 *   antes se ignoraba y solo quedaba el audio.
 * - Un script revisa el estado: error de YouTube -> fuente siguiente; además informa
 *   la resolución del video para diagnosticar (cartel abajo a la derecha).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReproductorYouTube(fuente: Fuente, onError: (String) -> Unit) {
    val onErrorActual = rememberUpdatedState(onError)
    var diagnostico by remember { mutableStateOf("Cargando YouTube…") }
    var mostrarDiagnostico by remember { mutableStateOf(true) }
    val url = "https://www.youtube.com/embed/live_stream?channel=${fuente.canalId}" +
        "&autoplay=1&mute=0&controls=0&rel=0&playsinline=1&iv_load_policy=3&enablejsapi=1"

    LaunchedEffect(Unit) { delay(15_000); mostrarDiagnostico = false }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val contenedor = FrameLayout(ctx).apply { setBackgroundColor(Color.BLACK) }
                val main = Handler(Looper.getMainLooper())
                var h264PorOnPageStarted = false
                var avisado = false // un solo aviso de error por fuente
                val web = WebView(ctx).apply {
                    setBackgroundColor(Color.BLACK)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    keepScreenOn = true
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true

                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun error(motivo: String) = main.post {
                            if (avisado) return@post
                            avisado = true
                            diagnostico = "YouTube: $motivo"
                            onErrorActual.value(motivo)
                        }

                        @JavascriptInterface
                        fun estado(texto: String) = main.post { diagnostico = texto }
                    }, "Android")

                    webChromeClient = object : WebChromeClient() {
                        private var vistaCompleta: View? = null

                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            vistaCompleta = view
                            contenedor.addView(
                                view,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                ),
                            )
                        }

                        override fun onHideCustomView() {
                            vistaCompleta?.let { contenedor.removeView(it) }
                            vistaCompleta = null
                        }

                        // Evita el ícono gris de "play" que algunos WebView ponen sobre el video.
                        override fun getDefaultVideoPoster(): Bitmap =
                            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            if (h264PorOnPageStarted) view.evaluateJavascript(JS_SOLO_H264, null)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            view.evaluateJavascript(JS_VIGILAR, null)
                        }
                    }

                    // Antes de que cargue el reproductor: ocultar VP9/AV1 para que YouTube
                    // mande H.264, que todos los decodificadores de TV dibujan bien.
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        WebViewCompat.addDocumentStartJavaScript(this, JS_SOLO_H264, setOf("*"))
                    } else {
                        h264PorOnPageStarted = true
                    }

                    // YouTube exige un Referer para los embeds (si falta: error 152/153).
                    loadUrl(url, mapOf("Referer" to REFERER))
                }
                contenedor.addView(
                    web,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                web.requestFocus()
                contenedor
            },
            onRelease = { contenedor ->
                (contenedor as ViewGroup).let { g ->
                    (0 until g.childCount).map { g.getChildAt(it) }
                        .filterIsInstance<WebView>().forEach { it.destroy() }
                }
            },
        )

        if (mostrarDiagnostico) {
            Text(
                diagnostico,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(androidx.compose.ui.graphics.Color(0xAA000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = androidx.compose.ui.graphics.Color.LightGray,
            )
        }
    }
}

private const val REFERER = "https://ar.flon.tv/"

/**
 * Corre dentro del embed: cada 2 s revisa si hay error de YouTube, si el video avanza
 * y qué resolución tiene. Si a los 30 s no arrancó, avisa error para pasar a otra fuente.
 */
private const val JS_VIGILAR = """
(function(){
  if (window.__vigilando) return; window.__vigilando = true;
  var inicio = Date.now(), arranco = false;
  var t = setInterval(function(){
    var err = document.querySelector('.ytp-error, .ytp-error-content-wrap');
    if (err && err.offsetParent !== null) {
      clearInterval(t); Android.error((err.innerText || 'error').trim().slice(0,80)); return;
    }
    var v = document.querySelector('video');
    if (v) {
      if (v.paused) { try { v.play(); } catch(e){} }
      if (v.currentTime > 0) arranco = true;
      Android.estado('Video ' + v.videoWidth + 'x' + v.videoHeight +
                     (window.__h264 ? ' · H.264' : ' · códec libre') +
                     (arranco ? ' · reproduciendo' : ' · cargando'));
      if (arranco && v.videoWidth === 0 && v.currentTime > 15) {
        clearInterval(t); Android.error('solo audio, sin video'); return;
      }
    }
    if (!arranco && Date.now() - inicio > 30000) { clearInterval(t); Android.error('no arrancó'); }
    if (arranco && v && v.videoWidth > 0 && Date.now() - inicio > 20000) clearInterval(t);
  }, 2000);
})();
"""

/**
 * Igual que la extensión "h264ify": le dice al reproductor que el aparato no soporta
 * VP9 ni AV1, así YouTube elige H.264. Corre en todos los frames antes que la página.
 */
private const val JS_SOLO_H264 = """
(function(){
  if (window.__h264) return; window.__h264 = true;
  var no = /vp0?9|av0?1/i;
  function envolver(obj){
    if (!obj || !obj.isTypeSupported) return;
    var orig = obj.isTypeSupported.bind(obj);
    obj.isTypeSupported = function(t){ return no.test(t) ? false : orig(t); };
  }
  envolver(window.MediaSource); envolver(window.ManagedMediaSource);
  var cpt = HTMLMediaElement.prototype.canPlayType;
  HTMLMediaElement.prototype.canPlayType = function(t){ return no.test(t) ? '' : cpt.call(this, t); };
})();
"""
