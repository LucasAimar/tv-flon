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

    /** Pestaña elegida en la grilla; se conserva al entrar y salir de un canal. */
    private val _categoria = MutableStateFlow<String?>(null)
    val categoria: StateFlow<String?> = _categoria.asStateFlow()

    /** Último canal abierto, para dejarle el foco al volver a la grilla. */
    private val _ultimoCanal = MutableStateFlow<String?>(null)
    val ultimoCanal: StateFlow<String?> = _ultimoCanal.asStateFlow()

    val favoritos: StateFlow<Set<String>> =
        prefs.favoritos.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch {
            val l = repo.cargar()
            _lista.value = l
            // Como un televisor: arranca en el último canal visto.
            prefs.ultimoCanal()?.let { id -> l.canales.find { it.id == id } }?.let {
                _categoria.value = it.categoria
                abrir(it)
            }
        }
    }

    fun elegirCategoria(nombre: String) {
        _categoria.value = nombre
    }

    fun abrir(canal: Canal) {
        _canalActual.value = canal
        _ultimoCanal.value = canal.id
        viewModelScope.launch { prefs.guardarUltimo(canal.id) }
    }

    fun cerrar() {
        _canalActual.value = null
    }

    fun alternarFavorito(canal: Canal) {
        viewModelScope.launch { prefs.alternarFavorito(canal.id) }
    }
}
