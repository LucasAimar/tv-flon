package ar.flon.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ar.flon.tv.data.Actualizador
import ar.flon.tv.data.AjustesYoutube
import ar.flon.tv.data.Canal
import ar.flon.tv.data.Version
import java.io.File
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

    // ---- Actualización desde GitHub ----
    private val actualizador = Actualizador(app)
    private val _actualizacion = MutableStateFlow<EstadoActualizacion>(EstadoActualizacion.Nada)
    val actualizacion: StateFlow<EstadoActualizacion> = _actualizacion.asStateFlow()
    private var apkBajado: File? = null

    /** Al abrir la app se busca en silencio; con el botón, se busca y se avisa el resultado. */
    fun buscarActualizacion(silencioso: Boolean = false) {
        if (_actualizacion.value is EstadoActualizacion.Descargando) return
        viewModelScope.launch {
            if (!silencioso) _actualizacion.value = EstadoActualizacion.Buscando
            _actualizacion.value = try {
                actualizador.buscar()?.let { EstadoActualizacion.Disponible(it) }
                    ?: if (silencioso) EstadoActualizacion.Nada else EstadoActualizacion.AlDia
            } catch (e: Exception) {
                if (silencioso) EstadoActualizacion.Nada
                else EstadoActualizacion.Error(e.message ?: "error")
            }
        }
    }

    /** Botón de la barra: según el estado busca, baja o instala. */
    fun accionActualizacion() {
        when (val e = _actualizacion.value) {
            is EstadoActualizacion.Disponible -> descargarEInstalar(e.version)
            is EstadoActualizacion.ListoParaInstalar, is EstadoActualizacion.FaltaPermiso -> instalar()
            is EstadoActualizacion.Buscando, is EstadoActualizacion.Descargando -> Unit
            else -> buscarActualizacion()
        }
    }

    private fun descargarEInstalar(v: Version) {
        viewModelScope.launch {
            _actualizacion.value = EstadoActualizacion.Descargando(v.build, 0)
            try {
                apkBajado = actualizador.descargar(v) { pct ->
                    _actualizacion.value = EstadoActualizacion.Descargando(v.build, pct)
                }
                _actualizacion.value = EstadoActualizacion.ListoParaInstalar(v.build)
                instalar()
            } catch (e: Exception) {
                _actualizacion.value = EstadoActualizacion.Error(e.message ?: "error en la descarga")
            }
        }
    }

    private fun instalar() {
        val apk = apkBajado ?: return buscarActualizacion()
        if (!actualizador.puedeInstalar()) {
            val abrio = actualizador.pedirPermiso()
            _actualizacion.value = EstadoActualizacion.FaltaPermiso(abrio)
            return
        }
        try {
            actualizador.instalar(apk)
        } catch (e: Exception) {
            _actualizacion.value = EstadoActualizacion.Error("no se pudo abrir el instalador: ${e.message}")
        }
    }

    val ajustesYoutube: StateFlow<AjustesYoutube> =
        prefs.ajustesYoutube.stateIn(viewModelScope, SharingStarted.Eagerly, AjustesYoutube())

    fun cambiarAjustesYoutube(a: AjustesYoutube) {
        viewModelScope.launch { prefs.guardarAjustesYoutube(a) }
    }

    val favoritos: StateFlow<Set<String>> =
        prefs.favoritos.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        buscarActualizacion(silencioso = true)
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

sealed interface EstadoActualizacion {
    data object Nada : EstadoActualizacion
    data object Buscando : EstadoActualizacion
    data object AlDia : EstadoActualizacion
    data class Disponible(val version: Version) : EstadoActualizacion
    data class Descargando(val build: Int, val porcentaje: Int) : EstadoActualizacion
    data class ListoParaInstalar(val build: Int) : EstadoActualizacion
    /** abrioAjustes = se abrió la pantalla del sistema; si no, hay que hacerlo a mano. */
    data class FaltaPermiso(val abrioAjustes: Boolean) : EstadoActualizacion
    data class Error(val motivo: String) : EstadoActualizacion
}
