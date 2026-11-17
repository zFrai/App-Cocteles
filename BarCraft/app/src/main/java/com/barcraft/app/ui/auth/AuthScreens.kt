@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.auth

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.lock.findFragmentActivity
import com.barcraft.app.ui.lock.showCryptoBiometricPrompt
import com.barcraft.app.ui.lock.strongBiometricAvailable
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.launch

/** Login y registro, con opción de seguir sin cuenta (solo recetas). */
@Composable
fun AuthFlow(onContinueAsGuest: () -> Unit) {
    val vm = appViewModel { AuthViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var registering by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fingerprintAvailable = remember { context.strongBiometricAvailable() }

    // Google: selector de cuentas del sistema -> token -> Firebase.
    val onGoogle: (Role) -> Unit = { role ->
        vm.onGoogleStarted()
        scope.launch { vm.onGoogleResult(requestGoogleIdToken(context), role) }
    }

    // Email + contraseña; si se marca "huella", primero se confirma la huella para cifrar el acceso.
    val onLogin: (String, String, Boolean) -> Unit = login@{ email, password, rememberFingerprint ->
        if (!vm.validateLogin(email, password)) return@login
        val activity = context.findFragmentActivity()
        if (!rememberFingerprint || activity == null) return@login vm.login(email, password)
        val cipher = vm.cipherToSaveFingerprint() ?: return@login
        showCryptoBiometricPrompt(
            activity, cipher,
            title = context.getString(R.string.auth_fingerprint_enable_title),
            subtitle = context.getString(R.string.auth_fingerprint_enable_subtitle),
            onSuccess = { vm.login(email, password, it) },
            onError = { message -> message?.let(vm::showError) }
        )
    }

    // Entrar con huella: la huella desbloquea el acceso guardado y se inicia sesión.
    val onFingerprintLogin: () -> Unit = fingerprint@{
        val activity = context.findFragmentActivity() ?: return@fingerprint
        val cipher = vm.cipherToLoginWithFingerprint() ?: return@fingerprint
        showCryptoBiometricPrompt(
            activity, cipher,
            title = context.getString(R.string.auth_fingerprint_login_title),
            subtitle = state.fingerprintEmail.orEmpty(),
            onSuccess = vm::loginWithFingerprint,
            onError = { message -> message?.let(vm::showError) }
        )
    }

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
                                onGoogle = onGoogle,
                                onGoLogin = { vm.clearMessages(); registering = false }
                            )
                        } else {
                            LoginForm(
                                loading = state.loading,
                                fingerprintAvailable = fingerprintAvailable,
                                fingerprintEmail = state.fingerprintEmail.takeIf { fingerprintAvailable },
                                onLogin = onLogin,
                                onFingerprintLogin = onFingerprintLogin,
                                onForgetFingerprint = vm::forgetFingerprint,
                                onGoogle = { onGoogle(Role.CLIENTE) },
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
                FitText(stringResource(R.string.auth_continue_as_guest))
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
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
    Text(
        stringResource(R.string.auth_tagline),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LoginForm(
    loading: Boolean,
    fingerprintAvailable: Boolean,
    fingerprintEmail: String?,
    onLogin: (String, String, Boolean) -> Unit,
    onFingerprintLogin: () -> Unit,
    onForgetFingerprint: () -> Unit,
    onGoogle: () -> Unit,
    onForgot: (String) -> Unit,
    onGoRegister: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberFingerprint by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.auth_login_title), style = MaterialTheme.typography.headlineSmall)
        if (fingerprintEmail != null) {
            FilledTonalButton(
                onClick = onFingerprintLogin,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Outlined.Fingerprint, null)
                Spacer(Modifier.width(8.dp))
                FitText(stringResource(R.string.auth_login_with_fingerprint))
            }
            Text(
                fingerprintEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            OrDivider()
        }
        AuthField(email, { email = it }, stringResource(R.string.auth_email), Icons.Outlined.Email, KeyboardType.Email)
        PasswordField(password, { password = it }, stringResource(R.string.auth_password), ImeAction.Done) {
            onLogin(email, password, rememberFingerprint)
        }
        TextButton(onClick = { onForgot(email) }, modifier = Modifier.align(Alignment.End)) {
            FitText(stringResource(R.string.auth_forgot_password))
        }
        if (fingerprintAvailable) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rememberFingerprint, onCheckedChange = { rememberFingerprint = it })
                Text(
                    if (fingerprintEmail == null) stringResource(R.string.auth_remember_fingerprint) else stringResource(R.string.auth_use_account_with_fingerprint),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        PrimaryAction(stringResource(R.string.auth_login_button), loading) { onLogin(email, password, rememberFingerprint) }
        GoogleButton(loading, onGoogle)
        OutlinedButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            FitText(stringResource(R.string.auth_go_to_register))
        }
        if (fingerprintEmail != null) {
            TextButton(onClick = onForgetFingerprint, modifier = Modifier.fillMaxWidth()) {
                FitText(stringResource(R.string.auth_forget_fingerprint))
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            stringResource(R.string.auth_or_divider),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        FitText(stringResource(R.string.auth_continue_with_google))
    }
}

@Composable
private fun RegisterForm(
    loading: Boolean,
    onRegister: (String, String, String, String, Role) -> Unit,
    onGoogle: (Role) -> Unit,
    onGoLogin: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf(Role.BARMAN) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.auth_register_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.auth_register_role_question), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Role.selfAssignable.forEachIndexed { i, r ->
                SegmentedButton(
                    selected = role == r,
                    onClick = { role = r },
                    shape = SegmentedButtonDefaults.itemShape(i, Role.selfAssignable.size),
                    label = { FitText(stringResource(r.labelRes)) }
                )
            }
        }
        Text(
            stringResource(R.string.auth_register_role_hint, stringResource(role.descriptionRes)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AuthField(name, { name = it }, stringResource(R.string.auth_display_name), Icons.Outlined.Person, KeyboardType.Text)
        AuthField(email, { email = it }, stringResource(R.string.auth_email), Icons.Outlined.Email, KeyboardType.Email)
        PasswordField(password, { password = it }, stringResource(R.string.auth_password_min), ImeAction.Next)
        PasswordField(confirm, { confirm = it }, stringResource(R.string.auth_password_repeat), ImeAction.Done) {
            onRegister(name, email, password, confirm, role)
        }
        PrimaryAction(stringResource(R.string.auth_register_button), loading) { onRegister(name, email, password, confirm, role) }
        OrDivider()
        // Con Google no hace falta nombre ni contraseña: se usan los de la cuenta de Google y el rol elegido arriba.
        GoogleButton(loading) { onGoogle(role) }
        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            FitText(stringResource(R.string.auth_go_to_login))
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
                    contentDescription = if (visible) stringResource(R.string.auth_hide_password) else stringResource(R.string.auth_show_password)
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
            FitText(label)
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
        Text(stringResource(R.string.auth_loading), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onLogout) { FitText(stringResource(R.string.auth_logout)) }
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
        Text(stringResource(R.string.auth_banned_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.auth_banned_message, profile.name),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogout) { FitText(stringResource(R.string.auth_logout)) }
    }
}
