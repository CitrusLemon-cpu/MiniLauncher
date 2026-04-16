package com.example.minilauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.minilauncher.util.PasswordUtils
import kotlinx.coroutines.delay

@Composable
fun PasswordEntryDialog(
    storedHash: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Enter Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = "Password",
                    focusRequester = focusRequester,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (PasswordUtils.verifyPassword(password, storedHash)) {
                            keyboardController?.hide()
                            onSuccess()
                        } else {
                            error = "Incorrect password"
                            password = ""
                            keyboardController?.show()
                        }
                    }
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (PasswordUtils.verifyPassword(password, storedHash)) {
                        keyboardController?.hide()
                        onSuccess()
                    } else {
                        error = "Incorrect password"
                        password = ""
                        keyboardController?.show()
                    }
                }
            ) {
                Text(text = "Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun SetPasswordDialog(
    onDismiss: () -> Unit,
    onSetPassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter a PIN or password to protect Secret Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = "Password",
                    focusRequester = focusRequester,
                    imeAction = ImeAction.Next
                )
                PasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    label = "Confirm Password",
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        validateNewPassword(password, confirmPassword)?.let {
                            error = it
                        } ?: run {
                            keyboardController?.hide()
                            onSetPassword(PasswordUtils.hashPassword(password))
                        }
                    }
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validateNewPassword(password, confirmPassword)?.let {
                        error = it
                    } ?: run {
                        keyboardController?.hide()
                        onSetPassword(PasswordUtils.hashPassword(password))
                    }
                }
            ) {
                Text(text = "Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    storedHash: String,
    onDismiss: () -> Unit,
    onChangePassword: (String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        error = null
                    },
                    label = "Current Password",
                    focusRequester = focusRequester,
                    imeAction = ImeAction.Next
                )
                PasswordField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = null
                    },
                    label = "New Password",
                    imeAction = ImeAction.Next
                )
                PasswordField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = null
                    },
                    label = "Confirm New Password",
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        validatePasswordChange(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                            confirmPassword = confirmPassword,
                            storedHash = storedHash
                        )?.let {
                            error = it
                            if (it == "Incorrect current password") {
                                currentPassword = ""
                                keyboardController?.show()
                            }
                        } ?: run {
                            keyboardController?.hide()
                            onChangePassword(PasswordUtils.hashPassword(newPassword))
                        }
                    }
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    validatePasswordChange(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        storedHash = storedHash
                    )?.let {
                        error = it
                        if (it == "Incorrect current password") {
                            currentPassword = ""
                            keyboardController?.show()
                        }
                    } ?: run {
                        keyboardController?.hide()
                        onChangePassword(PasswordUtils.hashPassword(newPassword))
                    }
                }
            ) {
                Text(text = "Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction,
    onImeAction: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onImeAction() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
    )
}

private fun validateNewPassword(password: String, confirmPassword: String): String? {
    return when {
        password.isBlank() || confirmPassword.isBlank() -> "Password cannot be empty"
        password.length < 4 -> "Password must be at least 4 characters"
        password != confirmPassword -> "Passwords do not match"
        else -> null
    }
}

private fun validatePasswordChange(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    storedHash: String
): String? {
    return when {
        !PasswordUtils.verifyPassword(currentPassword, storedHash) -> "Incorrect current password"
        else -> validateNewPassword(newPassword, confirmPassword)
    }
}
