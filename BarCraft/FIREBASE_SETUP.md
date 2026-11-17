# Conectar BarCraft con Firebase (chat, comunidad y roles)

Las recetas funcionan sin esto. Para el chat entre usuarios, la comunidad y los roles
hace falta un proyecto de Firebase (gratis, plan Spark). Son unos 10 minutos.

## 1. Crear el proyecto
1. Entra en https://console.firebase.google.com y pulsa **Crear un proyecto** (por ejemplo `barcraft`).
2. Google Analytics no hace falta: puedes desactivarlo.

## 2. Registrar la app Android
1. En la página del proyecto pulsa el icono de **Android**.
2. Nombre del paquete: `com.barcraft.app` (tiene que ser exactamente este).
3. Descarga **google-services.json** y cópialo en la carpeta `app/` del proyecto:
   `BarCraft/app/google-services.json`
4. Los pasos del SDK que propone la consola ya están hechos en el proyecto: pulsa Siguiente hasta terminar.

## 3. Activar el inicio de sesión
1. Menú **Compilación > Authentication > Comenzar**.
2. Pestaña **Método de acceso** > **Correo electrónico/contraseña** > Habilitar > Guardar.

## 4. Crear la base de datos
1. Menú **Compilación > Firestore Database > Crear base de datos**.
2. Ubicación: `eur3 (europe-west)` o la más cercana.
3. Elige **modo de producción**.
4. Abre la pestaña **Reglas**, borra lo que haya, pega el contenido de `firestore.rules`
   (está en la raíz del proyecto) y pulsa **Publicar**.

## 5. Compilar
En Android Studio: **File > Sync Project with Gradle Files** y luego ▶ Run.
El plugin de Google Services se activa solo cuando encuentra `app/google-services.json`.

## 6. Nombrar al primer Admin
Al registrarse cada persona elige **Barman** o **Comensal**. Para tener el primer Admin:
1. Regístrate en la app.
2. En la consola: **Firestore Database > Datos > users >** tu documento.
3. Cambia el campo `role` a `admin` (los valores válidos son `barman`, `cliente` y `admin`).
4. Vuelve a la app: en **Perfil** aparece **Panel de Admin**. Desde ahí ya puedes cambiar el rol de
   cualquiera o bloquear cuentas sin entrar en la consola.

> Si vienes de la versión 2: vuelve a publicar `firestore.rules`, han cambiado.
> Las cuentas antiguas con rol `user` se tratan como Barman.

> **Cada vez que cambie `firestore.rules` hay que volver a publicarlas** en la consola.
> Por ejemplo, las denuncias del chat (colección `reports`) no funcionan hasta publicarlas.

## Denuncias
Desde un chat, menú ⋮ > **Denunciar** (o mantén pulsado un mensaje > **Denunciar mensaje**).
Las denuncias se guardan en la colección `reports` y solo las ve el Admin, en
**Perfil > Panel de Admin > Denuncias**, donde puede marcarlas como revisadas, descartarlas o bloquear la cuenta.

## Probar el chat entre dos usuarios
Usa dos móviles o dos emuladores (o un móvil y un emulador), registra una cuenta en cada uno,
y en la pestaña **Buscar** busca al otro usuario y pulsa el icono de mensaje. Los mensajes llegan al instante.

## Seguir y seguidores
Seguir es en un solo sentido (colección `follows`, un documento `{quienSigue}_{aQuién}`).
Si la cuenta es privada (Perfil > Configuración), seguirla envía una **solicitud** que se acepta en
**Chats > Solicitudes**. Se puede chatear con quien tenga la cuenta pública o con quien os sigáis los dos.
Las amistades de la versión anterior se convierten solas en seguimiento mutuo al abrir Chats o Buscar.

## @usuario (tag único)
Cada cuenta tiene un identificador único y fijo, como `@laura #4821`: su nombre en minúsculas
(admite tildes como á à â ä, números y los símbolos `_ - . / \ |`; sin espacios, máx. 16) + 4 cifras.
El número se puede repetir con otro nombre y el nombre con otro número, pero nunca los dos.
Se crea solo al entrar en la app (también a las cuentas que ya existían; los tags ya creados no cambian),
se ve en **Perfil** y sirve para buscar a alguien en la pestaña **Buscar** (escribe `@laura` o `@laura #4821`).
Firestore guarda un documento por tag en la colección `tags`, así que no puede haber dos iguales.
Necesita las reglas actualizadas: vuelve a publicar `firestore.rules`.

## GIFs y stickers (GIPHY)
La clave de GIPHY va en el archivo `secrets.properties` de la raíz del proyecto. Copia
`secrets.properties.example` como `secrets.properties` y pon tu clave:
```
GIPHY_API_KEY=tu_clave
```
Ese archivo está en `.gitignore`, así que no se sube a GitHub. Sin clave, la app funciona
igual pero sin GIFs ni stickers.

## Notas
- `google-services.json` está en `.gitignore`: no se sube a GitHub. Cada persona que compile
  el proyecto necesita el suyo (o el mismo, compartido por privado).
- No se usa Firebase Storage (desde 2024 exige plan de pago). Las fotos y notas de voz del chat
  y la comunidad se guardan en Firestore (colección `media`, hasta ~900 KB cada una: las fotos se
  comprimen y los audios duran como mucho 3 minutos). Se descargan al móvil la primera vez que
  se abren y desde Perfil se pueden borrar las descargas para liberar espacio.
- El plan gratuito de Firestore da 1 GB de datos: unos 3.000-5.000 fotos o audios. Si se queda
  corto, el siguiente paso sería pasar las fotos a Cloudinary o a Firebase Storage.
