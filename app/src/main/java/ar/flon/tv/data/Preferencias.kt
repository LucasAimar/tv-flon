package ar.flon.tv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferencias")

/** Ajustes de reproducción de YouTube (menú ☰ → Reproducción). */
data class AjustesYoutube(
    /** true = los canales de YouTube se abren en la app oficial de YouTube del TV. */
    val enAppOficial: Boolean = false,
    /** Calidad máxima a pedir dentro de TV: "hd1080" o "hd720". */
    val calidadMaxima: String = "hd1080",
)

/** Favoritos, último canal visto y ajustes. */
class Preferencias(private val context: Context) {

    private val kFavoritos = stringSetPreferencesKey("favoritos")
    private val kUltimo = stringPreferencesKey("ultimo_canal")
    private val kYtEnApp = booleanPreferencesKey("youtube_en_app")
    private val kYtCalidad = stringPreferencesKey("youtube_calidad")

    val ajustesYoutube: Flow<AjustesYoutube> = context.dataStore.data.map {
        AjustesYoutube(
            enAppOficial = it[kYtEnApp] ?: false,
            calidadMaxima = it[kYtCalidad] ?: "hd1080",
        )
    }

    suspend fun guardarAjustesYoutube(a: AjustesYoutube) {
        context.dataStore.edit {
            it[kYtEnApp] = a.enAppOficial
            it[kYtCalidad] = a.calidadMaxima
        }
    }

    val favoritos: Flow<Set<String>> = context.dataStore.data.map { it[kFavoritos] ?: emptySet() }

    suspend fun alternarFavorito(id: String) {
        context.dataStore.edit { p ->
            val actual = p[kFavoritos] ?: emptySet()
            p[kFavoritos] = if (id in actual) actual - id else actual + id
        }
    }

    suspend fun ultimoCanal(): String? = context.dataStore.data.first()[kUltimo]

    suspend fun guardarUltimo(id: String) {
        context.dataStore.edit { it[kUltimo] = id }
    }
}
