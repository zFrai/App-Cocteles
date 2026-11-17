package com.barcraft.app.ui.lock

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.biometric.BiometricPrompt
import com.barcraft.app.data.local.LockStore
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.auth.requestGoogleIdToken
import com.barcraft.app.utils.appContainer
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.launch

/**
 * Protege la app de una cuenta con PIN + huella.
 * Si aún no hay PIN, pide crearlo; si la app está bloqueada, pide huella o PIN.
 */
@Composable
fun LockGate(profile: UserProfile, onLogout: () -> Unit, content: @Composable () -> Unit) {
    val container = appContainer()
    val locked by container.appLock.locked.collectAsStateWithLifecycle()
    val vm = appViewModel(key = "lock_${profile.uid}") { LockViewModel(it, profile.uid) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(locked) { if (locked) vm.reset() }

    when {
        state.needsSetup -> PinSetupScreen(state.message, onCreate = vm::createPin, onLogout = onLogout)
        locked -> LockScreen(profile, vm, state, onLogout)
        else -> content()
    }
}

@Composable
private fun LockScaffold(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

@Composable
private fun PinSetupScreen(message: String?, onCreate: (String, Boolean) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val canUseBiometric = remember { context.biometricAvailable() }
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var pin by rememberSaveable { mutableStateOf("") }
    var useBiometric by rememberSaveable { mutableStateOf(canUseBiometric) }
    var error by remember { mutableStateOf<String?>(null) }

    LockScaffold {
        LockIcon(Icons.Outlined.Lock)
        Spacer(Modifier.height(16.dp))
        Text(if (first == null) stringResource(R.string.lock_create_pin_title) else stringResource(R.string.lock_repeat_pin_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            message ?: stringResource(R.string.lock_create_pin_message, LockStore.PIN_EVERY),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        PinDots(pin.length, LockStore.PIN_LENGTH.last)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        PinPad(
            onDigit = { if (pin.length < LockStore.PIN_LENGTH.last) { pin += it; error = null } },
            onDelete = { pin = pin.dropLast(1) }
        )
        Spacer(Modifier.height(12.dp))
        if (first != null && canUseBiometric) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.lock_use_fingerprint_option)) },
                leadingContent = { Icon(Icons.Outlined.Fingerprint, null) },
                trailingContent = { Switch(checked = useBiometric, onCheckedChange = { useBiometric = it }) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
        val errorPinLength = stringResource(R.string.lock_error_pin_length, LockStore.PIN_LENGTH.first, LockStore.PIN_LENGTH.last)
        val errorPinMismatch = stringResource(R.string.lock_error_pin_mismatch)
        Button(
            onClick = {
                val f = first
                when {
                    pin.length !in LockStore.PIN_LENGTH -> error = errorPinLength
                    f == null -> { first = pin; pin = "" }
                    f != pin -> { error = errorPinMismatch; first = null; pin = "" }
                    else -> onCreate(pin, useBiometric && canUseBiometric)
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { FitText(if (first == null) stringResource(R.string.lock_continue) else stringResource(R.string.lock_save_pin)) }
        TextButton(onClick = onLogout) { FitText(stringResource(R.string.auth_logout)) }
    }
}

@Composable
private fun LockScreen(profile: UserProfile, vm: LockViewModel, state: LockUiState, onLogout: () -> Unit) {
    val context = LocalContext.current
    var pin by rememberSaveable { mutableStateOf("") }
    var forgot by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf<BiometricPrompt?>(null) }
    val useBiometric = state.biometricEnabled && !state.pinRequired && context.biometricAvailable()

    // Al mostrar la pantalla, si toca huella, se abre el diálogo directamente.
    LaunchedEffect(useBiometric) {
        if (useBiometric) {
            val activity = context.findFragmentActivity()
            if (activity == null) vm.onBiometricDismissed()
            else prompt = showBiometricPrompt(
                activity,
                onSuccess = vm::onBiometricSuccess,
                onFailed = { if (vm.onBiometricFailed()) prompt?.cancelAuthentication() },
                onError = vm::onBiometricDismissed
            )
        }
    }

    LockScaffold {
        LockIcon(if (useBiometric) Icons.Outlined.Fingerprint else Icons.Outlined.Lock)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.lock_greeting, profile.name), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            state.message ?: if (useBiometric) stringResource(R.string.lock_place_finger) else stringResource(R.string.lock_enter_pin),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.biometricEnabled && !state.pinRequired) {
            Text(
                stringResource(R.string.lock_unlocks_left, vm.unlocksLeft),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        if (!useBiometric) {
            PinDots(pin.length, LockStore.PIN_LENGTH.last)
            state.pinError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            PinPad(
                onDigit = {
                    if (pin.length < LockStore.PIN_LENGTH.last) pin += it
                    if (pin.length in LockStore.PIN_LENGTH && vm.submitPin(pin)) pin = ""
                    else if (pin.length == LockStore.PIN_LENGTH.last) pin = ""
                },
                onDelete = { pin = pin.dropLast(1) }
            )
            Spacer(Modifier.height(8.dp))
            if (vm.canRetryBiometric() && context.biometricAvailable()) {
                OutlinedButton(onClick = vm::retryBiometric) {
                    Icon(Icons.Outlined.Fingerprint, null)
                    Spacer(Modifier.width(8.dp))
                    FitText(stringResource(R.string.lock_use_fingerprint))
                }
            }
        } else {
            Button(onClick = vm::onBiometricDismissed) { FitText(stringResource(R.string.lock_use_pin)) }
        }
        TextButton(onClick = { forgot = true }) { FitText(stringResource(R.string.lock_forgot_pin)) }
        TextButton(onClick = onLogout) { FitText(stringResource(R.string.auth_logout)) }
    }

    if (forgot && vm.recoversWithGoogle) {
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { forgot = false },
            title = { Text(stringResource(R.string.lock_recover_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lock_recover_google, profile.email))
                    state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.onGoogleRecoverStarted()
                        scope.launch { vm.onGoogleRecoverResult(requestGoogleIdToken(context)) }
                    },
                    enabled = !state.checkingPassword
                ) { FitText(stringResource(R.string.auth_continue_with_google)) }
            },
            dismissButton = { TextButton(onClick = { forgot = false }) { FitText(stringResource(R.string.action_cancel)) } }
        )
    } else if (forgot) {
        var password by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { forgot = false },
            title = { Text(stringResource(R.string.lock_recover_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lock_recover_password, profile.email))
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text(stringResource(R.string.auth_password)) }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = state.passwordError != null,
                        supportingText = if (state.passwordError != null) { { Text(state.passwordError.orEmpty()) } } else null
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.recoverWithPassword(password) }, enabled = password.isNotEmpty() && !state.checkingPassword) {
                    FitText(stringResource(R.string.lock_check))
                }
            },
            dismissButton = { TextButton(onClick = { forgot = false }) { FitText(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun LockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(14.dp).background(
                    if (i < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    CircleShape
                )
            )
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(68.dp))
                        "⌫" -> IconButton(onClick = onDelete, modifier = Modifier.size(68.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, stringResource(R.string.lock_delete_digit))
                        }
                        else -> FilledTonalIconButton(onClick = { onDigit(key) }, modifier = Modifier.size(68.dp)) {
                            Text(key, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
