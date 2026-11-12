package ar.flon.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ar.flon.tv.data.Canal
import ar.flon.tv.data.ListaCanales
import ar.flon.tv.data.Preferencias
import ar.flon.tv.data.RepositorioCanales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RepositorioCanales(app)
    private val prefs = Preferencias(app)

    private val _lista = MutableStateFlow<ListaCanales?>(null)
    val lista: StateFlow<ListaCanales?> = _lista.asStateFlow()

    /** Canal en reproducción; null = grilla. */
    private val _canalActual = MutableStateFlow<Canal?>(null)
    val canalActual: StateFlow<Canal?> = _canalActual.asStateFlow()

    val favoritos: StateFlow<Set<String>> =
        prefs.favoritos.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            val l = repo.cargar()
            _lista.value = l
            // Como un televisor: arranca en el último canal visto.
            prefs.ultimoCanal()?.let { id -> l.canales.find { it.id == id } }?.let { abrir(it) }
        }
    }

    fun abrir(canal: Canal) {
        _canalActual.value = canal
        viewModelScope.launch { prefs.guardarUltimo(canal.id) }
    }

    fun cerrar() {
        _canalActual.value = null
    }

    fun alternarFavorito(canal: Canal) {
        viewModelScope.launch { prefs.alternarFavorito(canal.id) }
    }
}
