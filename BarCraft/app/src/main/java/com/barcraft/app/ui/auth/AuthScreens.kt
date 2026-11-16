@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.utils.appViewModel

/** Login y registro, con opción de seguir sin cuenta (solo recetas). */
@Composable
fun AuthFlow(onContinueAsGuest: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var registering by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background),
                    endY = 1400f
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.height(32.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(targetState = registering, label = "auth") { isRegister ->
                    Column(Modifier.padding(24.dp)) {
                        if (isRegister) {
                            RegisterForm(
                                loading = state.loading,
                                onRegister = vm::register,
                                onGoLogin = { vm.clearMessages(); registering = false }
                            )
                        } else {
                            LoginForm(
                                loading = state.loading,
                                onLogin = vm::login,
                                onForgot = vm::resetPassword,
                                onGoRegister = { vm.clearMessages(); registering = true }
                            )
                        }
                        state.error?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                        }
                        state.info?.let {
                            Spacer(Modifier.height(12.dp))
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onContinueAsGuest) {
                Text("Entrar como comensal sin cuenta")
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(84.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocalBar, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp))
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("BarCraft", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
    Text(
        "Tu recetario de coctelería y su comunidad",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LoginForm(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onForgot: (String) -> Unit,
    onGoRegister: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Iniciar sesión", style = MaterialTheme.typography.headlineSmall)
        AuthField(email, { email = it }, "Email", Icons.Outlined.Email, KeyboardType.Email)
        PasswordField(password, { password = it }, "Contraseña", ImeAction.Done) { onLogin(email, password) }
        TextButton(onClick = { onForgot(email) }, modifier = Modifier.align(Alignment.End)) {
            Text("¿Olvidaste tu contraseña?")
        }
        PrimaryAction("Entrar", loading) { onLogin(email, password) }
        OutlinedButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Crear una cuenta")
        }
    }
}

@Composable
private fun RegisterForm(
    loading: Boolean,
    onRegister: (String, String, String, String, Role) -> Unit,
    onGoLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(Role.BARMAN) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Crear cuenta", style = MaterialTheme.typography.headlineSmall)
        Text("¿Cómo vas a usar BarCraft?", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Role.selfAssignable.forEachIndexed { i, r ->
                SegmentedButton(
                    selected = role == r,
                    onClick = { role = r },
                    shape = SegmentedButtonDefaults.itemShape(i, Role.selfAssignable.size),
                    label = { Text(r.label) }
                )
            }
        }
        Text(
            role.description + ". El rol de Admin solo lo puede dar otro Admin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AuthField(name, { name = it }, "Nombre visible", Icons.Outlined.Person, KeyboardType.Text)
        AuthField(email, { email = it }, "Email", Icons.Outlined.Email, KeyboardType.Email)
        PasswordField(password, { password = it }, "Contraseña (mín. 6)", ImeAction.Next)
        PasswordField(confirm, { confirm = it }, "Repite la contraseña", ImeAction.Done) {
            onRegister(name, email, password, confirm, role)
        }
        PrimaryAction("Registrarme", loading) { onRegister(name, email, password, confirm, role) }
        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Ya tengo cuenta")
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    onDone: () -> Unit = {}
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Outlined.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrimaryAction(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(label)
        }
    }
}

/** Mientras se carga el perfil del usuario. */
@Composable
fun SessionLoadingScreen(onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Preparando la barra…", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onLogout) { Text("Cerrar sesión") }
    }
}

/** Pantalla para cuentas bloqueadas por un Admin. */
@Composable
fun BannedScreen(profile: UserProfile, onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Block, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Cuenta bloqueada", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "${profile.name}, un administrador ha bloqueado tu cuenta. Si crees que es un error, contacta con el equipo de BarCraft.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogout) { Text("Cerrar sesión") }
    }
}
