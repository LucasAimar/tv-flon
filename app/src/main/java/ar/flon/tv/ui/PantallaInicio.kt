package ar.flon.tv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import ar.flon.tv.data.Canal
import ar.flon.tv.data.ListaCanales
import ar.flon.tv.data.ordenados
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

private const val FAVORITOS = "★ Favoritos"

/** Pestaña general: todos los canales de TV (sin las radios). La app arranca acá. */
const val CATEGORIA_TODOS = "Todos"

/**
 * Pestañas de categorías arriba (el foco cambia la categoría) y grilla de canales abajo.
 * OK abre el canal; mantener OK presionado lo agrega o quita de favoritos.
 */
@Composable
fun PantallaInicio(
    lista: ListaCanales,
    favoritos: Set<String>,
    categoriaElegida: String?,
    canalEnfocado: String?,
    onCategoria: (String) -> Unit,
    onAbrir: (Canal) -> Unit,
    onFavorito: (Canal) -> Unit,
    /** Lo que va arriba a la derecha, al lado de las pestañas (botón de actualizar). */
    extraArriba: @Composable () -> Unit = {},
) {
    val categorias = remember(lista, favoritos.isEmpty()) {
        buildList {
            add(CATEGORIA_TODOS)
            if (favoritos.isNotEmpty()) add(FAVORITOS)
            addAll(lista.categorias.filter { c -> lista.canales.any { it.categoria == c } })
        }
    }
    // La categoría vive en el ViewModel: al volver de un canal se mantiene la misma pestaña.
    val seleccion = categorias.indexOf(categoriaElegida).takeIf { it >= 0 } ?: 0
    val categoria = categorias.getOrNull(seleccion)

    val canales = remember(categoria, lista, favoritos) {
        when (categoria) {
            CATEGORIA_TODOS -> lista.ordenados { !it.esRadio }
            FAVORITOS -> lista.canales.filter { it.id in favoritos }
            else -> lista.canales.filter { it.categoria == categoria }
        }
    }

    // Al entrar, el foco va al canal que se estaba viendo (o al primero de la grilla).
    val indiceFoco = canales.indexOfFirst { it.id == canalEnfocado }.takeIf { it >= 0 } ?: 0
    val focoGrilla = remember { FocusRequester() }
    val estadoGrilla = rememberLazyGridState()
    LaunchedEffect(Unit) {
        estadoGrilla.scrollToItem(indiceFoco)
        delay(150)
        runCatching { focoGrilla.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            TabRow(selectedTabIndex = seleccion, modifier = Modifier.weight(1f)) {
                categorias.forEachIndexed { i, nombre ->
                    Tab(selected = i == seleccion, onFocus = { onCategoria(nombre) }) {
                        Text(
                            nombre,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.width(24.dp))
            extraArriba()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            contentPadding = PaddingValues(vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            state = estadoGrilla,
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(canales, key = { _, c -> c.id }) { i, canal ->
                TarjetaCanal(
                    canal = canal,
                    modifier = if (i == indiceFoco) Modifier.focusRequester(focoGrilla) else Modifier,
                    esFavorito = canal.id in favoritos,
                    onClick = { onAbrir(canal) },
                    onLongClick = { onFavorito(canal) },
                )
            }
        }
    }
}
