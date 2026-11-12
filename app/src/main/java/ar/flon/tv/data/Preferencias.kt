package ar.flon.tv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferencias")

/** Favoritos y último canal visto. */
class Preferencias(private val context: Context) {

    private val kFavoritos = stringSetPreferencesKey("favoritos")
    private val kUltimo = stringPreferencesKey("ultimo_canal")

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
