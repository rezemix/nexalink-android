package online.nexalink.app.ui.account

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import online.nexalink.app.R
import online.nexalink.app.ui.base.BaseComponentActivity
import online.nexalink.app.ui.compose.AppTopBar

class NexalinkAccountActivity : BaseComponentActivity() {

    private val viewModel: NexalinkAccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        NexalinkAccountScreen(viewModel = viewModel, onBackClick = { finish() })
    }
}

@Composable
fun NexalinkAccountScreen(
    viewModel: NexalinkAccountViewModel,
    onBackClick: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val savedEmail by viewModel.savedEmail.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
        topBar = {
            AppTopBar(
                title = "Аккаунт NEXALINK",
                onBackClick = onBackClick,
                isLoading = isLoading
            )
        }
    ) { innerPadding ->
        if (isLoggedIn) {
            LoggedInContent(
                email = savedEmail,
                innerPadding = innerPadding,
                onLogout = { viewModel.logout() }
            )
        } else {
            LoginForm(
                isLoading = isLoading,
                errorMessage = errorMessage,
                innerPadding = innerPadding,
                onClearError = { viewModel.clearError() },
                onSubmit = { email, password, isRegister ->
                    viewModel.submit(email, password, isRegister)
                }
            )
        }
    }
}

@Composable
private fun LoggedInContent(
    email: String,
    innerPadding: PaddingValues,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Вы вошли как:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = email,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Подписка NEXALINK уже добавлена в список серверов и будет обновляться автоматически.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onLogout) {
            Text("Выйти из аккаунта")
        }
    }
}

@Composable
private fun LoginForm(
    isLoading: Boolean,
    errorMessage: String?,
    innerPadding: PaddingValues,
    onClearError: () -> Unit,
    onSubmit: (email: String, password: String, isRegister: Boolean) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    LaunchedEffect(email, password) {
        if (errorMessage != null) onClearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isRegister) "Создать аккаунт NEXALINK" else "Войти в аккаунт NEXALINK",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Тот же аккаунт, что на сайте nexalink.online и в Telegram-боте. " +
                "Подписка добавится и обновится автоматически.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = { onSubmit(email, password, isRegister) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRegister) "Зарегистрироваться" else "Войти")
            }
        }

        TextButton(
            onClick = { isRegister = !isRegister },
            enabled = !isLoading
        ) {
            Text(
                if (isRegister) "Уже есть аккаунт? Войти"
                else "Нет аккаунта? Зарегистрироваться"
            )
        }
    }
}
