# 🍹 App-Cocteles

Aplicación Android para que bartenders creen, organicen y valoren su carta de cócteles. Funciona **sin conexión**: todos los datos se guardan en el propio dispositivo.

**Android** · **Java** · **minSdk 24 (Android 7.0)** · **Versión 1.0**

---

## Índice

- [Funcionalidades](#funcionalidades)
- [Pantallas](#pantallas)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Puesta en marcha](#puesta-en-marcha)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Modelo de datos](#modelo-de-datos)
- [Hoja de ruta](#hoja-de-ruta)
- [Cómo contribuir](#cómo-contribuir)
- [Autor](#autor)

---

## Funcionalidades

### Carta de cócteles
- **Crear, editar y eliminar** cócteles. Al eliminar se pide confirmación.
- Cada ficha guarda **nombre** (obligatorio), **descripción** y **procedimiento** de preparación.
- **Alcoholes y mezclas** como listas separadas.
- **Humo:** indica si el cóctel se sirve ahumado.
- **Topping:** indica si lleva decoración y permite describirla.

### Imágenes
- Foto del cóctel elegida desde la **galería** del dispositivo, con vista previa en el formulario.
- Si no hay imagen, se muestra un icono por defecto.

### Lista y búsqueda
- Lista con foto, nombre y **promedio de estrellas** de cada cóctel.
- **Buscador** en tiempo real por nombre o descripción.
- Mensaje de bienvenida cuando la carta está vacía.

### Valoraciones
- Cualquiera puede votar un cóctel indicando su **nombre** y una puntuación de **1 a 6 estrellas**.
- La app calcula y muestra el **promedio** de cada cóctel.
- El detalle muestra la lista de votos con su autor.

---

## Pantallas

```
Lista de cócteles ──┬── [ + ] ──────────→ Crear cóctel
  (buscador)        │
                    └── [Tocar tarjeta] ─→ Detalle del cóctel
                                              ├── Editar   → Editar cóctel
                                              ├── Eliminar → confirmar → Lista
                                              └── + Añadir valoración (nombre + estrellas)
```

| Pantalla | Activity | Qué hace |
|---|---|---|
| **Lista** | `MainActivity` | Muestra la carta, permite buscar y abre el resto de pantallas. |
| **Detalle** | `DetailCocktailActivity` | Ficha completa, valoraciones y acciones de editar y eliminar. |
| **Crear / Editar** | `EditCocktailActivity` | Formulario con todos los campos del cóctel y su imagen. |

---

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 8 |
| Plataforma | Android SDK 34 (mínimo API 24, Android 7.0) |
| Build | Gradle con Android Gradle Plugin 8.2.2 |
| Persistencia | SQLite (`SQLiteOpenHelper`) |
| Interfaz | AndroidX AppCompat, Material Components, RecyclerView, ConstraintLayout, CoordinatorLayout |

---

## Requisitos

- **Android Studio** Hedgehog (2023.1) o más reciente.
- **JDK 17** (el que incluye Android Studio sirve).
- Un móvil o emulador con **Android 7.0 (API 24)** o superior.

---

## Puesta en marcha

1. Clona el repositorio y cambia a la rama de desarrollo:
   ```bash
   git clone https://github.com/zFrai/App-Cocteles.git
   cd App-Cocteles
   git checkout Development
   ```
2. En Android Studio, abre la carpeta del proyecto con **File → Open**.
3. Espera a que termine la sincronización de Gradle.
4. Elige un dispositivo o emulador y pulsa **Run ▶**.

> **Nota:** el proyecto todavía no incluye el Gradle wrapper, así que de momento se compila desde Android Studio. Compilar con `./gradlew assembleDebug` llegará con el issue #28.

---

## Estructura del proyecto

```
App-Cocteles/
├── build.gradle                 Configuración de Gradle del proyecto
├── settings.gradle
└── app/
    ├── build.gradle             Configuración del módulo (SDK, dependencias)
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/bartender/cocktailapp/
        │   ├── activity/        Pantallas: lista, detalle y formulario
        │   ├── adapter/         CocktailAdapter (RecyclerView de la lista)
        │   ├── database/        DatabaseHelper (SQLite)
        │   └── model/           Cocktail y Rating
        └── res/                 Layouts, menús, drawables, colores y textos
```

---

## Modelo de datos

```
Cocktail 1 ──── N Rating
```

**`cocktails`**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | INTEGER | Clave primaria autoincremental |
| `name` | TEXT | Nombre del cóctel (obligatorio) |
| `description` | TEXT | Descripción |
| `image_url` | TEXT | URI de la imagen |
| `procedure` | TEXT | Preparación |
| `alcohols` / `mixers` | TEXT | Listas de alcoholes y mezclas |
| `has_smoke` | INTEGER | 1 si se sirve ahumado |
| `has_topping` | INTEGER | 1 si lleva topping |
| `topping_description` | TEXT | Descripción del topping |

**`ratings`**

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | INTEGER | Clave primaria autoincremental |
| `cocktail_id` | INTEGER | Cóctel valorado (se borra en cascada) |
| `voter_name` | TEXT | Nombre de quien vota |
| `stars` | INTEGER | Puntuación de 1 a 6 |

---

## Hoja de ruta

El trabajo pendiente está en los [issues del repositorio](https://github.com/zFrai/App-Cocteles/issues), divididos en tareas pequeñas:

- [x] **Proyecto en el repositorio** (#26)
- [ ] **Build:** `.gitignore`, Gradle wrapper, limpieza del manifest y compilación en cada PR con GitHub Actions.
- [ ] **Base de datos:** conservar los datos al actualizar la app y migrar de SQLite a **Room**, con tablas propias para ingredientes y votos.
- [ ] **Arquitectura:** `CocktailRepository` y paquetes `data / model / repository / ui / utils`.
- [ ] **Formulario:** no perder datos al girar la pantalla, confirmar antes de descartar cambios y deshacer al borrar.
- [ ] **Imágenes:** carga por URL con Coil o Glide y placeholder si la imagen ya no existe.
- [ ] **Ingredientes:** lista editable con tipo (alcohol o mezcla) y cantidad.
- [ ] **Garnish:** renombrar "Topping" a "Garnish".
- [ ] **Lista y detalle:** tarjetas expandibles y procedimiento como pasos numerados.
- [ ] **Valoraciones:** editar y borrar votos.
- [ ] **Extras:** perfiles Barman y Cliente, chat entre ambos y diseño adaptado a tablet.
- [ ] **Versión 1.1**

---

## Cómo contribuir

1. Crea una rama desde `Development` (por ejemplo `feature/ratings`).
2. Haz commits pequeños y descriptivos.
3. Abre un Pull Request hacia `Development` enlazando el issue que resuelve.

---

## Autor

**Fräi** · [@zFrai](https://github.com/zFrai)

## Licencia

- [Licencia](#licencia)

Este proyecto se distribuye bajo la **licencia MIT**. Consulta el archivo [LICENSE](LICENSE) para ver el texto completo.

Copyright (c) 2026 Fräi

### Servicios de terceros

**GIPHY.** Los GIFs y stickers que muestra la app se obtienen a través de la [API de GIPHY](https://developers.giphy.com/). Ese contenido no está cubierto por la licencia MIT de este proyecto: pertenece a GIPHY y a sus autores, y su uso se rige por las [condiciones de GIPHY](https://giphy.com/terms). Como exigen esas condiciones, la app muestra la atribución **"Powered by GIPHY"** allí donde aparece contenido de GIPHY.

La clave de la API de GIPHY es personal y no se incluye en el repositorio.
