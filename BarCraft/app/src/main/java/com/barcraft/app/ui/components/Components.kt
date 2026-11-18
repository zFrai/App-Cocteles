package com.barcraft.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.barcraft.app.utils.appContainer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import coil.compose.AsyncImage
import com.barcraft.app.R
import com.barcraft.app.model.Role
import com.barcraft.app.data.remote.LoadProblem

/** Mensaje centrado para listas vacías. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { FitText(actionLabel) }
        }
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

/** Círculo con las iniciales del usuario, de color estable según el nombre. */
@Composable
fun UserAvatar(
    name: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    /** Si se indica, se busca su foto de perfil (y si no tiene, se ve la inicial). */
    uid: String? = null,
    /** Foto ya conocida (p. ej. del perfil completo); evita leer el perfil otra vez. */
    photoId: String? = null
) {
    val palette = listOf(
        Color(0xFFE9C46A), Color(0xFF8ED3C0), Color(0xFFD3BBF5), Color(0xFFFFB59A),
        Color(0xFF9ECAFF), Color(0xFFF4A3C0), Color(0xFFB5D98A)
    )
    val color = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    val photo = avatarPhotoId(uid, photoId)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        // La inicial se ve mientras la foto se descarga o si no hay conexión.
        Text(
            initials,
            color = Color(0xFF1B1B1F),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp
        )
        if (photo.isNotBlank()) {
            val file = rememberMediaFileOrNull(photo)
            if (file != null) {
                AsyncImage(
                    model = file,
                    contentDescription = stringResource(R.string.avatar_photo_of, name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Id de la foto de perfil que hay que mostrar ("" = ninguna). */
@Composable
private fun avatarPhotoId(uid: String?, photoId: String?): String {
    if (uid.isNullOrBlank()) return photoId.orEmpty()
    val container = appContainer()
    if (!container.firebaseEnabled) return ""
    val cache = container.avatarCache
    if (photoId != null) {
        LaunchedEffect(uid, photoId) { cache.set(uid, photoId) }
        return photoId
    }
    LaunchedEffect(uid) { cache.load(uid) }
    val photos by cache.photos.collectAsState()
    return photos[uid].orEmpty()
}

/**
 * Etiqueta con el rol, de un color distinto para cada uno:
 * Admin (escudo), Barman (coctelera) y Comensal. El de comensal solo se pinta si [showUser].
 */
@Composable
fun RoleBadge(role: Role, showUser: Boolean = false) {
    if (role == Role.CLIENTE && !showUser) return
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (role) {
        Role.ADMIN -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        Role.BARMAN -> scheme.primaryContainer to scheme.onPrimaryContainer
        Role.CLIENTE -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    val icon = when (role) {
        Role.ADMIN -> Icons.Filled.Shield
        Role.BARMAN -> Icons.Filled.LocalBar
        Role.CLIENTE -> Icons.Filled.Person
    }
    Surface(shape = CircleShape, color = bg, contentColor = fg) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(role.labelRes), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Fila de estrellas (1–6). Si [onRate] no es null, se puede pulsar para votar. */
@Composable
fun StarRating(
    value: Float,
    modifier: Modifier = Modifier,
    max: Int = 6,
    starSize: Dp = 16.dp,
    onRate: ((Int) -> Unit)? = null
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { i ->
            val filled = i + 1 <= value + 0.25f
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (onRate != null) pluralStringResource(R.plurals.rating_stars, i + 1, i + 1) else null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(starSize)
                    .then(if (onRate != null) Modifier.clickable { onRate(i + 1) } else Modifier)
            )
        }
    }
}

/** Puntos de dificultad (1–5). */
@Composable
fun DifficultyDots(level: Int, max: Int = 5) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { i ->
            Box(
                Modifier.size(7.dp).clip(CircleShape).background(
                    if (i < level) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

/** Imagen de cóctel con marcador elegante si no hay foto o falla la carga. */
@Composable
fun CocktailImage(model: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val placeholder: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.LocalBar, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
        }
    }
    Box(modifier) {
        if (model.isBlank()) placeholder()
        else SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { placeholder() },
            error = { placeholder() }
        )
    }
}

/** Título de sección dentro de una pantalla. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 4.dp)
    )
}

/** Tarjeta de sección con fondo de contenedor. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(Modifier.padding(contentPadding)) { content() }
    }
}

/**
 * Aviso cuando Firestore no deja cargar algo (en vez de un círculo cargando para siempre).
 * Si falta un índice y el error trae el enlace, el botón abre la consola para crearlo.
 */
@Composable
fun LoadProblemBanner(problem: LoadProblem, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val (title, message) = when (problem.kind) {
        LoadProblem.Kind.MISSING_INDEX ->
            stringResource(R.string.load_problem_index_title) to stringResource(R.string.load_problem_index_message)
        LoadProblem.Kind.PERMISSION_DENIED ->
            stringResource(R.string.load_problem_rules_title) to stringResource(R.string.load_problem_rules_message)
        LoadProblem.Kind.OTHER ->
            stringResource(R.string.load_problem_other_title) to stringResource(R.string.load_problem_other_message)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            val url = problem.indexUrl
            if (url != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { runCatching { uriHandler.openUri(url) } }) {
                    FitText(stringResource(R.string.load_problem_create_index))
                }
            }
        }
    }
}
