package com.example.coflow.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coflow.R
import com.example.coflow.ui.components.*
import com.example.coflow.ui.theme.*

@Composable
fun AuthScreen(
    state: AuthUiState,
    onUsernameChange: (String) -> Unit,
    onContactChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(coFlowBackgroundBrush()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CoFlowSpacing.ScreenHorizontal, vertical = CoFlowSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CoFlowAuthCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.auth_welcome_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = CoFlowColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(CoFlowSpacing.Small))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(if (state.mode == AuthMode.LOGIN) R.string.auth_subtitle_login else R.string.auth_subtitle_register),
                    style = MaterialTheme.typography.titleMedium,
                    color = CoFlowColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(CoFlowSpacing.Medium))

                if (state.mode == AuthMode.REGISTER) {
                    CoFlowAuthTextField(
                        value = state.username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.auth_label_username),
                        placeholder = stringResource(R.string.auth_placeholder_username),
                        isError = state.usernameError != null,
                        supportingText = state.usernameError?.let { { Text(it) } }
                    )
                    Spacer(modifier = Modifier.height(CoFlowSpacing.FieldGap))
                }

                CoFlowAuthTextField(
                    value = state.contact,
                    onValueChange = onContactChange,
                    label = stringResource(R.string.auth_label_contact),
                    placeholder = stringResource(R.string.auth_placeholder_contact),
                    isError = state.contactError != null,
                    supportingText = state.contactError?.let { { Text(it) } }
                )

                Spacer(modifier = Modifier.height(CoFlowSpacing.FieldGap))

                CoFlowAuthTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.auth_label_password),
                    placeholder = stringResource(R.string.auth_placeholder_password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { { Text(it) } }
                )

                if (state.mode == AuthMode.REGISTER) {
                    Spacer(modifier = Modifier.height(CoFlowSpacing.FieldGap))
                    CoFlowAuthTextField(
                        value = state.confirmPassword,
                        onValueChange = onConfirmChange,
                        label = stringResource(R.string.auth_label_confirm_password),
                        placeholder = stringResource(R.string.auth_placeholder_confirm_password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = state.confirmError != null,
                        supportingText = state.confirmError?.let { { Text(stringResource(R.string.auth_error_not_equal_password)) } }
                    )
                }

                Spacer(modifier = Modifier.height(CoFlowSpacing.Medium))

                CoFlowPrimaryButton(
                    text = stringResource(if (state.mode == AuthMode.LOGIN) R.string.auth_button_login else R.string.auth_button_register),
                    onClick = onSubmit,
                    enabled = !state.isSubmitting,
                    height = 48.dp
                )

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(CoFlowSpacing.Small))
                    Text(
                        text = state.errorMessage!!,
                        modifier = Modifier.fillMaxWidth(),
                        color = CoFlowColors.Error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(CoFlowSpacing.Small))

                TextButton(onClick = onToggleMode) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(if (state.mode == AuthMode.LOGIN) R.string.auth_toggle_to_register else R.string.auth_toggle_to_login),
                        color = CoFlowColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AuthRoute(onAuthSuccess: () -> Unit,
              viewModel: AuthViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAuthorized) {
        if (state.isAuthorized) onAuthSuccess()
    }
    AuthScreen(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onContactChange = viewModel::onContactChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onToggleMode = viewModel::onToggleMode,
        onSubmit = { viewModel.onSubmit() }
    )
}

// ============================================================================
// ПРЕВЬЮ (Все 4 варианта)
// ============================================================================

// 1. Пустая форма авторизации
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AuthLoginPreview() {
    CoFlowTheme {
        AuthScreen(
            state = AuthUiState(),
            onUsernameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleMode = {},
            onSubmit = {}
        )
    }
}

// 2. Форма авторизации с ошибками валидации
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AuthLoginErrorPreview() {
    CoFlowTheme {
        AuthScreen(
            state = AuthUiState(
                contact = "liza@mail",
                password = "123",
                contactError = "Проверьте формат почты",
                passwordError = "Минимум 8 символов"
            ),
            onUsernameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleMode = {},
            onSubmit = {}
        )
    }
}

// 3. Пустая форма регистрации
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AuthRegisterPreview() {
    CoFlowTheme {
        AuthScreen(
            state = AuthUiState(
                mode = AuthMode.REGISTER,
                username = "Лиза"
            ),
            onUsernameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleMode = {},
            onSubmit = {}
        )
    }
}

// 4. Форма регистрации с ошибкой несовпадения паролей
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AuthRegisterMismatchPreview() {
    CoFlowTheme {
        AuthScreen(
            state = AuthUiState(
                mode = AuthMode.REGISTER,
                username = "Лиза",
                contact = "liza@mail.ru",
                password = "parol123",
                confirmPassword = "parol124",
                confirmError = "Пароли не совпадают"
            ),
            onUsernameChange = {},
            onContactChange = {},
            onPasswordChange = {},
            onConfirmChange = {},
            onToggleMode = {},
            onSubmit = {}
        )
    }
}