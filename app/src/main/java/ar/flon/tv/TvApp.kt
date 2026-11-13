package ar.flon.tv

import android.app.Application
import ar.flon.tv.data.Identificadores
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

/**
 * Configura la carga de imágenes (logos) para toda la app.
 *
 * Wikimedia rechaza (HTTP 403) las descargas que no se identifican con un User-Agent
 * propio; con el de fábrica ("okhttp/…") no se veían los logos de Telefe, TyC, Telemax,
 * Crónica, Canal 26 y América. Su política pide nombre de la app y un contacto.
 */
class TvApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Identificadores.iniciar(this)
    }

    override fun newImageLoader(): ImageLoader {
        val agente = "TV/${BuildConfig.VERSION_NAME.substringBefore(" ")} " +
            "(https://github.com/${BuildConfig.REPO}; app personal para Android TV)"
        val cliente = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", agente).build())
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(cliente)
            .crossfade(true)
            .build()
    }
}
