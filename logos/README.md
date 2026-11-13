# Logos de TV Floñ

Si un canal **no tiene** `logo` en `canales.json`, la app busca automáticamente
`logos/<id>.png` en este repo. Alcanza con subir el archivo con el nombre exacto:
no hace falta tocar el JSON ni recompilar (la app lo baja al abrir la grilla).

## Cómo tienen que ser
- **Formato:** PNG (JPG también anda, pero el nombre tiene que terminar en `.png`).
- **Fondo:** transparente o blanco. La tarjeta pone el logo sobre un fondo gris muy claro.
- **Tamaño:** entre 300 y 600 px de ancho. Horizontal o cuadrado, da igual: se ajusta solo.
- **Peso:** idealmente menos de 200 KB.
- **Nombre:** exactamente el `id` del canal + `.png`, en minúsculas.

## Faltan
| Archivo | Canal | Categoría |
|---|---|---|
| `3564.png` | 3564 Stream TV | Córdoba |
| `cadena3.png` | Cadena 3 | Radios |
| `lapopu.png` | La Popu | Radios |
| `nacionalcba.png` | Radio Nacional Córdoba | Radios |
| `nacional.png` | Radio Nacional AM 870 | Radios |
| `nacionalrock.png` | Nacional Rock 93.7 | Radios |
| `nacionalfolk.png` | Nacional Folklórica 98.7 | Radios |
| `continental.png` | Continental 590 | Radios |
| `rivadavia.png` | Radio Rivadavia | Radios |
| `radio10.png` | Radio 10 | Radios |
| `aspen.png` | Aspen 102.3 | Radios |
| `cosquinrock.png` | Cosquín Rock 95.5 | Radios |

## Para reemplazar un logo que ya existe
Esos canales tienen el logo puesto en `canales.json` (campo `logo`). Borrá esa línea del JSON
y subí el archivo acá con el nombre del `id`.
