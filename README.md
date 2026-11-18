# 🍸 BarCraft

Recetario de coctelería para Android con su propia comunidad. Los barmans guardan su carta de cócteles en el móvil (funciona **sin conexión**) y, si quieren, la comparten con sus seguidores o con todo el mundo. Además hay muro de publicaciones, chats en tiempo real, perfiles con @usuario y QR, y un panel de moderación para los administradores.

**Android** · **Kotlin** · **Jetpack Compose** · **Firebase** · **minSdk 26 (Android 8.0)** · **Versión 3.0**

📲 **[Descargar el APK](https://github.com/zFrai/App-Cocteles/releases/latest)** desde GitHub Releases.

---

## Índice

- [Descargar](#descargar)
- [Funcionalidades](#funcionalidades)
- [Pantallas](#pantallas)
- [Roles y permisos](#roles-y-permisos)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Puesta en marcha](#puesta-en-marcha)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Modelo de datos](#modelo-de-datos)
- [Hoja de ruta](#hoja-de-ruta)
- [Cómo contribuir](#cómo-contribuir)
- [Autor](#autor)
- [Licencia](#licencia)

---

## Descargar

1. Abre la página de **[Releases](https://github.com/zFrai/App-Cocteles/releases)** y descarga el `.apk` de la última versión.
2. En el móvil, ábrelo y permite **instalar apps de origen desconocido** si Android lo pide.
3. Necesitas Android 8.0 (API 26) o superior.

---

## Funcionalidades

### Recetario
- **Crear, editar y eliminar** cócteles. Al eliminar se pide confirmación y se borran también sus ingredientes, pasos y valoraciones.
- Cada ficha guarda **nombre** (obligatorio), **descripción**, **categoría** (Clásico, Tropical, Sour, Tiki, Highball, Sin alcohol…), **dificultad** (1 a 5) y **tiempo de preparación**.
- **Ingredientes** como lista editable, con cantidad y tipo: Alcohol, Mezcla, Garnish o Extra.
- **Procedimiento** como pasos numerados que se pueden reordenar.
- **Humo:** indica si el cóctel se sirve ahumado.
- **Garnish:** indica si lleva decoración y permite describirla.
- **Favoritos** y filtro "Solo favoritos".
- **Exportar recetas** a JSON como copia de seguridad.
- Todo se guarda en el móvil con Room: las recetas funcionan sin cuenta y sin conexión.

### Imágenes
- Foto del cóctel desde la **cámara**, la **galería** o pegando una **URL**.
- Si no hay imagen, se muestra un icono por defecto.

### Lista y búsqueda
- Tarjetas expandibles con foto, categoría, tiempo, ingredientes, humo, garnish y **promedio de estrellas**.
- **Buscador** en tiempo real por nombre o ingrediente, y filtros por categoría.

### Valoraciones
- Cualquiera puede votar un cóctel con su **nombre**, una puntuación de **1 a 6 estrellas** y un comentario opcional.
- La app calcula y muestra el **promedio** y el número de votos.
- Solo Barman y Admin pueden **borrar** valoraciones.

### Visibilidad de las recetas
Cada receta puede ser:

| Visibilidad | Quién la ve |
|---|---|
| **Privada** | Solo tú, en este móvil. No se sube. |
| **Seguidores** | Tus seguidores, en Comunidad > Recetas. |
| **Pública** | Cualquiera, en Comunidad > Recetas. |

Las recetas compartidas se pueden **guardar en tu recetario** con el crédito de su autor. Las fotos guardadas en el móvil no se suben.

### Comunidad
- Muro con dos pestañas: **Recetas** compartidas y **Publicaciones**.
- Publicaciones con texto, foto y una **receta adjunta** de tu barra.
- **Me gusta**, **comentarios** y botón para escribir al autor.
- **Guardados:** el marcador guarda una publicación ajena en Perfil > Guardados. Si su autor la edita, verás "@nombre #1234 ha cambiado su post".
- **Mis publicaciones:** editar o borrar lo que has publicado.

### Personas, seguidores y QR
- Cada cuenta tiene un **@usuario** único y fijo, como `@laura #4821`.
- **Buscar** personas por nombre o por `@nombre #1234`.
- **Seguir** en un solo sentido. Si la cuenta es **privada**, seguirla envía una **solicitud** que se acepta o se rechaza en Chats > Solicitudes.
- Pestañas de **Seguidores**, **Siguiendo**, **Solicitudes** y **Bloqueados**, con favoritos marcados con ★.
- **QR del perfil:** quien lo escanee desde Buscar te sigue automáticamente (o te envía una solicitud si tu cuenta es privada).

### Chats
- Mensajes en **tiempo real** con cuentas públicas o con quien os seguís los dos.
- **Texto, fotos, notas de voz** (hasta 3 minutos), **GIFs y stickers** de GIPHY.
- **Reacciones**, **borrar para todos** y guardar fotos en la galería.
- **Bloquear** a una persona y **denunciar** una cuenta o un mensaje concreto.
- Las fotos y audios se descargan la primera vez que se abren, y desde Perfil se pueden borrar las descargas para liberar espacio.

### Perfil
- **Foto de perfil**, nombre visible y **descripción** de hasta 250 caracteres.
- **Dónde encontrarme** (opcional): Instagram, TikTok, X, web y el local donde trabajas.
- **Cuenta privada** y avisos de comentarios, mensajes y seguidores (las notificaciones push llegarán más adelante).
- **9 temas** con versión clara y oscura: Deluxe/Champán, Volcán/Terracota, Abisal/Laguna, Tormenta/Cielo, Absenta/Menta, Granadina/Fresa, Medianoche/Lavanda, Negroni/Spritz y Espresso/Crema. Desliza la ficha a la derecha para el modo oscuro o a la izquierda para el claro.

### Cuenta y seguridad
- Registro e inicio de sesión con **email y contraseña** o con **Google**.
- **Entrar con huella** después de cerrar sesión: el acceso se guarda cifrado con una clave del Android Keystore.
- **Bloqueo de la app con huella y PIN**: cada 30 entradas con huella se pide el PIN. El PIN olvidado se recupera con la contraseña o con la misma cuenta de Google.
- **Modo sin cuenta:** se puede entrar como comensal invitado y usar solo el recetario.
- **Modo local:** sin `google-services.json` la app compila y funciona igual, solo con recetas.

### Moderación (Admin)
- **Panel de Admin** en Perfil > Configuración, con recuento de usuarios por rol.
- **Usuarios:** buscar, cambiar el rol y bloquear o desbloquear cuentas.
- **Publicaciones:** ver y borrar cualquier publicación.
- **Denuncias:** marcarlas como revisadas, descartarlas o bloquear la cuenta denunciada.

### Textos
- Todos los textos de la app están en `res/values/strings.xml`, listos para editarlos o traducirlos.

---

## Pantallas

```
Inicio de sesión ─┬─ Email / Google / Huella
                  ├─ Crear cuenta (Barman o Comensal)
                  └─ Entrar sin cuenta
        │
        ▼
Bloqueo (huella / PIN)
        │
        ▼
┌──────────┬────────────┬────────┬─────────┬──────────┐
│ Recetas  │ Comunidad  │ Chats  │ Buscar  │ Perfil   │
└──────────┴────────────┴────────┴─────────┴──────────┘
 Recetas ──┬── [ + ] ─────────────→ Nuevo cóctel
           └── [Tarjeta] ─────────→ Detalle ─┬─ Editar
                                             ├─ Eliminar → confirmar
                                             ├─ Valorar (nombre + estrellas)
                                             └─ Compartir en la comunidad
 Comunidad ─┬─ Recetas ───────────→ Receta compartida → Guardar en mis recetas
            └─ Publicaciones ─────→ Publicación (comentarios) · [Publicar]
 Chats ─────┬─ Chats ─────────────→ Conversación
            └─ Seguidores · Siguiendo · Solicitudes · Bloqueados
 Buscar ────── Personas · Escanear QR
 Perfil ────┬─ Editar perfil · Mi QR · Dónde encontrarme · Tema
            ├─ Mis publicaciones · Guardados
            └─ Configuración ─────→ Panel de Admin (solo Admin)
```

| Pantalla | Archivo | Qué hace |
|---|---|---|
| Acceso | `ui/auth/AuthScreens.kt` | Iniciar sesión, registrarse, Google, huella y modo invitado. |
| Bloqueo | `ui/lock/LockScreens.kt` | Crear PIN, desbloquear con huella o PIN y recuperar el PIN. |
| Recetas | `ui/cocktails/CocktailListScreen.kt` | Lista, búsqueda, filtros y favoritos. |
| Detalle | `ui/cocktails/CocktailDetailScreen.kt` | Ingredientes, preparación, valoraciones y acciones. |
| Editor | `ui/cocktails/CocktailEditorScreen.kt` | Formulario completo del cóctel, su foto y su visibilidad. |
| Comunidad | `ui/social/FeedScreen.kt` | Muro de recetas compartidas y publicaciones. |
| Publicación | `ui/social/PostDetailScreen.kt` | Publicación con sus comentarios. |
| Nueva publicación | `ui/social/ComposePostScreen.kt` | Texto, foto y receta adjunta. |
| Chats | `ui/chat/ChatListScreen.kt` | Conversaciones, seguidores, solicitudes y bloqueados. |
| Conversación | `ui/chat/ChatScreen.kt` | Mensajes, fotos, audio, GIFs, reacciones y denuncias. |
| Buscar | `ui/chat/PeopleScreen.kt` | Buscar personas y escanear su QR. |
| Perfil | `ui/profile/ProfileScreen.kt` | Datos, tema, QR, ajustes, seguridad y exportar. |
| Admin | `ui/admin/AdminScreen.kt` | Usuarios, publicaciones y denuncias. |

---

## Roles y permisos

| Acción | Invitado | Comensal | Barman | Admin |
|---|:-:|:-:|:-:|:-:|
| Recetario en el móvil | ✅ | ✅ | ✅ | ✅ |
| Valorar cócteles | ✅ | ✅ | ✅ | ✅ |
| Borrar valoraciones | | | ✅ | ✅ |
| Compartir recetas, comunidad y chat | | ✅ | ✅ | ✅ |
| Comentar y dar me gusta | | ✅ | ✅ | ✅ |
| Publicar en el muro | | | ✅ | ✅ |
| Moderar, cambiar roles y bloquear | | | | ✅ |

Al registrarse cada persona elige **Barman** o **Comensal**. El rol de **Admin** solo lo da otro Admin (el primero se pone a mano en Firestore, ver [Puesta en marcha](#puesta-en-marcha)). Los permisos están en `model/Role.kt` y se repiten en `firestore.rules`.

---

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.1 (JVM 17) |
| Plataforma | Android SDK 35 (mínimo API 26, Android 8.0) |
| Build | Gradle 8.14 (wrapper incluido) con Android Gradle Plugin 8.10 y Kotlin DSL |
| Interfaz | Jetpack Compose, Material 3, Navigation Compose |
| Persistencia local | Room 2.7 (con KSP) |
| Backend | Firebase Authentication y Cloud Firestore |
| Inicio de sesión | Credential Manager + Google ID, AndroidX Biometric |
| Imágenes y GIFs | Coil |
| GIFs y stickers | API de GIPHY |
| QR | ZXing (generar) y Google Code Scanner (leer, sin permiso de cámara) |
| Asíncrono | Kotlin Coroutines y Flow |

---

## Requisitos

- Android Studio Meerkat Feature Drop (2024.3.2) o más reciente.
- JDK 17 (el que incluye Android Studio sirve).
- Un móvil o emulador con Android 8.0 (API 26) o superior.
- Para la comunidad y el chat: un proyecto de **Firebase** (el plan gratuito Spark basta).
- Para GIFs y stickers: una clave gratuita de **GIPHY** (opcional).

---

## Puesta en marcha

1. Clona el repositorio y cambia a la rama de desarrollo:
   ```bash
   git clone https://github.com/zFrai/App-Cocteles.git
   cd App-Cocteles
   git checkout Development
   ```
2. En Android Studio, abre la carpeta **`BarCraft`** con **File → Open** y espera a que termine la sincronización de Gradle.
3. Elige un dispositivo o emulador y pulsa **Run ▶**. Así ya funciona en modo local (solo recetas).

También se puede compilar desde la terminal:
```bash
cd BarCraft
./gradlew assembleDebug
```

### Conectar Firebase (comunidad, chat y roles)
La guía completa paso a paso está en [`BarCraft/FIREBASE_SETUP.md`](BarCraft/FIREBASE_SETUP.md). En resumen:

1. Crea un proyecto en la [consola de Firebase](https://console.firebase.google.com) y registra una app Android con el paquete **`com.barcraft.app`**.
2. Descarga **`google-services.json`** y cópialo en `BarCraft/app/google-services.json`.
3. En **Authentication**, activa **Correo electrónico/contraseña** y, si quieres, **Google**.
4. Para Google, añade las huellas **SHA-1** de debug y release (`./gradlew signingReport`) en la configuración de la app de Firebase y **vuelve a descargar** `google-services.json`.
5. Crea **Firestore Database** en modo producción, pega el contenido de [`BarCraft/firestore.rules`](BarCraft/firestore.rules) en la pestaña **Reglas** y pulsa **Publicar**.
6. Crea el índice de **Comunidad > Recetas**: en Firestore > Índices > **Exenciones de campo único**, colección `recipes`, campo `visibility`, ámbito **Grupo de colecciones**, ascendente. (También aparece un enlace para crearlo en Logcat la primera vez que abres esa pestaña).
7. Para el primer Admin: regístrate, abre tu documento en `users` y cambia `role` a `admin`.

> **Cada vez que cambie `firestore.rules` hay que volver a publicarlas** en la consola de Firebase.

### Clave de GIPHY
Copia `BarCraft/secrets.properties.example` como `BarCraft/secrets.properties` y pon tu clave:
```properties
GIPHY_API_KEY=tu_clave_de_giphy
```
Sin clave, la app funciona igual pero sin GIFs ni stickers.

> `google-services.json` y `secrets.properties` están en `.gitignore`: **no se suben al repositorio**. Cada persona usa los suyos.

---

## Estructura del proyecto

```
App-Cocteles/
├── README.md
├── LICENSE
└── BarCraft/                        Proyecto de Android Studio
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle/libs.versions.toml    Versiones de todas las dependencias
    ├── firestore.rules              Reglas de seguridad de Firestore
    ├── FIREBASE_SETUP.md            Guía para conectar Firebase
    ├── secrets.properties.example   Plantilla para la clave de GIPHY
    └── app/
        ├── build.gradle.kts         Configuración del módulo (SDK, dependencias)
        ├── schemas/                 Esquemas exportados de Room
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/barcraft/app/
            │   ├── data/local/      Room (AppDatabase, CocktailDao, entidades), PIN, ajustes y huella
            │   ├── data/remote/     Firebase y conversión de documentos de Firestore
            │   ├── model/           Roles, visibilidad, usuarios, chats, denuncias…
            │   ├── repository/      Recetas, cuentas, seguidores, chat, comunidad, GIPHY, media
            │   ├── ui/              Pantallas Compose: auth, lock, cocktails, social, chat,
            │   │                    profile, qr, admin, navigation, components y theme
            │   └── utils/           Bloqueo de la app, audio, imágenes, fechas y textos
            └── res/                 strings.xml, colores, temas e iconos
```

---

## Modelo de datos

### En el móvil (Room)

```
Cocktail 1 ──┬── N Ingredient
             ├── N Step
             └── N Rating
```

**cocktails**

| Campo | Tipo | Descripción |
|---|---|---|
| id | Long | Clave primaria autoincremental |
| name | String | Nombre del cóctel (obligatorio) |
| description | String | Descripción |
| imageUri | String | Foto local o URL |
| category | String | Clásico, Tropical, Sour… |
| difficulty | Int | De 1 a 5 |
| prepTimeMinutes | Int | Tiempo de preparación |
| hasSmoke | Boolean | Se sirve ahumado |
| hasGarnish / garnishDescription | Boolean / String | Lleva garnish y cuál |
| isFavorite | Boolean | Marcado como favorito |
| averageRating / ratingCount | Float / Int | Promedio y número de votos |
| visibility | String | `private`, `followers` o `public` |
| createdAt | Long | Fecha de creación |

- **ingredients** · `cocktailId`, `name`, `amount`, `type` (Alcohol, Mezcla, Garnish, Extra), `position`
- **steps** · `cocktailId`, `position`, `instruction`
- **ratings** · `cocktailId`, `voterName`, `stars` (1 a 6), `comment`, `createdAt`

Ingredientes, pasos y valoraciones se borran en cascada con su cóctel.

### En la nube (Firestore)

| Colección | Qué guarda |
|---|---|
| `users/{uid}` | Perfil: nombre, rol, @tag, foto, descripción, enlaces, cuenta privada, bloqueo |
| `users/{uid}/recipes` | Recetas compartidas (Seguidores o Pública) |
| `users/{uid}/saved` | Publicaciones guardadas (privado) |
| `users/{uid}/private` | Ajustes privados, como la lista de bloqueados |
| `tags/{tag}` | Reserva del @usuario para que no se repita |
| `follows/{a}_{b}` | Seguimientos y solicitudes |
| `chats/{roomId}/messages` | Conversaciones y mensajes |
| `posts/{postId}/comments` | Publicaciones del muro y sus comentarios |
| `media/{id}` | Fotos y notas de voz (hasta ~900 KB cada una) |
| `reports` | Denuncias, solo visibles para el Admin |

No se usa Firebase Storage: las fotos se comprimen y se guardan en Firestore, así todo cabe en el plan gratuito.

---

## Hoja de ruta

El trabajo pendiente está en los [issues](https://github.com/zFrai/App-Cocteles/issues) del repositorio:

- [ ] **Build:** compilar en cada PR con GitHub Actions (#31), ejecutar tests y lint en el CI (#80) y activar R8 en la release (#76).
- [ ] **Base de datos:** que Room no borre las recetas al subir la versión de la BD (#73) y guardar su esquema en el repo (#74).
- [ ] **Recetas:** importar recetas desde JSON (#75).
- [ ] **Formulario:** confirmar antes de salir con cambios (#46) y deshacer al eliminar un cóctel (#47).
- [ ] **Valoraciones:** editar un voto (#62).
- [ ] **Tests:** permisos por rol (#77), CocktailDao (#78) y bloqueo con PIN (#79).
- [ ] **Tablet:** diseño adaptado a pantallas grandes (#67).
- [ ] **Notificaciones push** con Firebase Cloud Messaging.

---

## Cómo contribuir

1. Crea una rama desde `Development` (por ejemplo `feature/importar-json`).
2. Haz commits pequeños y descriptivos.
3. Si cambias `firestore.rules`, dilo en el PR para volver a publicarlas.
4. Abre un Pull Request hacia `Development` enlazando el issue que resuelve.

---

## Autor

**Fräi** · [@zFrai](https://github.com/zFrai)

---

## Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para ver el texto completo.

Copyright (c) 2026 Fräi

### Servicios de terceros

**GIPHY.** Los GIFs y stickers que muestra la app se obtienen a través de la [API de GIPHY](https://developers.giphy.com/). Ese contenido no está cubierto por la licencia MIT de este proyecto: pertenece a GIPHY y a sus autores, y su uso se rige por las [condiciones de GIPHY](https://support.giphy.com/hc/en-us/articles/360020027752-GIPHY-Terms-of-Service). Como exigen esas condiciones, la app muestra la atribución **"Powered by GIPHY"** allí donde aparece contenido de GIPHY.

La clave de la API de GIPHY es personal y no se incluye en el repositorio.
