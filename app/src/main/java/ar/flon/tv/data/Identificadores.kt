package ar.flon.tv.data

import android.content.Context
import java.util.UUID

/**
 * Identificador anónimo y fijo de esta instalación (no es el del aparato ni de la cuenta).
 * Lo usan servicios gratuitos como Pluto TV para contar dispositivos y servir publicidad.
 */
object Identificadores {
    lateinit var dispositivo: String
        private set

    fun iniciar(context: Context) {
        val prefs = context.getSharedPreferences("identificadores", Context.MODE_PRIVATE)
        dispositivo = prefs.getString("dispositivo", null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString("dispositivo", it).apply() }
    }
}
