package ar.flon.tv.ui.reproductores

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import ar.flon.tv.data.Fuente
import ar.flon.tv.data.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Reproduce el reproductor oficial de un sitio web dentro de un WebView.
 *
 * - Con `extraerIframe` (ej. Telefe): baja el HTML de la página, busca el iframe cuyo src
 *   contiene ese fragmento (con su token del momento) y abre solo ese reproductor,
 *   que ya ocupa toda la pantalla.
 * - Sin `extraerIframe`: abre la página entera e inyecta CSS para agrandar el primer <video>.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReproductorWeb(fuente: Fuente, onError: () -> Unit) {
    val pagina = fuente.url.orEmpty()
    var destino by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pagina) {
        val patron = fuente.extraerIframe
        destino = if (patron == null) pagina else extraerIframe(pagina, patron)
        if (destino == null) onError()
    }

    val url = destino
    if (url == null) {
        Box(Modifier.fillMaxSize()) { Text("Conectando…", modifier = Modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(Color.BLACK)
                keepScreenOn = true
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString = Http.USER_AGENT
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        if (fuente.extraerIframe == null) view.evaluateJavascript(JS_PANTALLA_COMPLETA, null)
                    }
                }
                loadUrl(url, mapOf("Referer" to pagina))
                requestFocus()
            }
        },
        onRelease = { it.destroy() },
    )
}

private suspend fun extraerIframe(pagina: String, patron: String): String? = withContext(Dispatchers.IO) {
    try {
        val html = Http.cliente.newCall(Request.Builder().url(pagina).build()).execute()
            .use { it.body?.string() } ?: return@withContext null
        val regex = Regex("""<iframe[^>]+src=["']([^"']*${Regex.escape(patron)}[^"']*)["']""")
        val src = regex.find(html)?.groupValues?.get(1)?.replace("&amp;", "&") ?: return@withContext null
        val absoluto = if (src.startsWith("//")) "https:$src" else src
        if ("autoplay" in absoluto) absoluto
        else absoluto + (if ("?" in absoluto) "&" else "?") + "autoplay=true"
    } catch (e: Exception) {
        null
    }
}

/** Pone el primer <video> de la página fijo y a pantalla completa, por encima de todo. */
private const val JS_PANTALLA_COMPLETA = """
(function(){
  function aplicar(){
    var v = document.querySelector('video');
    if(!v) return false;
    v.style.cssText = 'position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;z-index:2147483647!important;background:#000!important;object-fit:contain!important';
    document.body.style.overflow = 'hidden';
    v.muted = false; v.play && v.play();
    return true;
  }
  if(!aplicar()){ var n=0, t=setInterval(function(){ if(aplicar()||++n>20) clearInterval(t); }, 1000); }
})();
"""
