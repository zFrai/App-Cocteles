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

### Iniciar sesión con Google (opcional)
1. En la misma pestaña **Método de acceso** > **Agregar proveedor nuevo** > **Google** > Habilitar,
   elige el correo de asistencia y Guardar.
2. Google exige la huella **SHA-1** del certificado con el que se firma la app. Sácala en Android Studio
   con el panel **Gradle > BarCraft > Tasks > android > signingReport** (o `gradlew signingReport`).
   Aparecen la de `debug` y, si tienes keystore de publicación, la de `release`.
3. En **Configuración del proyecto** (rueda dentada) > **Tus apps** > la app Android > **Agregar huella digital**:
   añade la SHA-1 de debug y la de release (y la SHA-256 si quieres).
4. **Vuelve a descargar `google-services.json`** y sustituye el de `app/`: ahora incluye el ID de cliente
   web que necesita el botón de Google. Sin esto el botón muestra "Google no está configurado".
5. Si publicas en Google Play con "Firma de apps de Google Play", añade también la SHA-1 que aparece en
   Play Console > Integridad de la app.

Las cuentas nuevas creadas con Google desde **Iniciar sesión** son Comensal; desde **Crear cuenta**
usan el rol elegido arriba. Como no tienen contraseña, el PIN olvidado se recupera eligiendo la misma
cuenta de Google.

### Iniciar sesión con huella
No necesita nada en Firebase. Al entrar con email y contraseña, marca **Entrar con huella la próxima vez**:
el acceso se guarda cifrado en el móvil con una clave del Android Keystore que solo se abre con la huella.
Después de cerrar sesión aparece **Entrar con huella**. Si cambias la contraseña o añades huellas nuevas
en el móvil, hay que volver a activarlo. Las cuentas de Google no lo necesitan: entran con un toque.

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
En **Perfil > Mi QR** está el QR de la cuenta: quien lo escanee desde **Buscar** (icono de QR arriba)
te seguirá automáticamente, o te enviará una solicitud si tu cuenta es privada.
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

## Recetas en Comunidad
Al editar una receta eliges **Privada** (solo en tu móvil), **Seguidores** o **Pública**. Las dos últimas
se copian a `users/{uid}/recipes/{id}` y salen en **Comunidad > Recetas**: las públicas para todo el mundo
y las de Seguidores solo para quien te sigue. Las fotos guardadas en el móvil no se suben.

La lista de recetas públicas usa una consulta de grupo y **necesita un índice**: la primera vez que abras
Comunidad > Recetas, en Logcat (filtro `BarCraft`) aparecerá un error de Firestore con un enlace para crearlo.
Ábrelo, pulsa **Crear índice** y espera unos minutos. También puedes crearlo a mano en
Firestore Database > Índices > **Exenciones de campo único**: colección `recipes`, campo `visibility`,
ámbito **Grupo de colecciones**, ascendente activado.
