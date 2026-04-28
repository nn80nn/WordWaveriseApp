package n.startapp.wordwaveriseapp.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import n.startapp.wordwaveriseapp.BuildConfig
import n.startapp.wordwaveriseapp.ui.theme.*

@Composable
fun AuthScreen(
    state: AuthState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onLoginWithGoogle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Вход", "Регистрация")
    val context = LocalContext.current

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                task.result?.idToken?.let { onLoginWithGoogle(it) }
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "WordWaverise",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryCyan
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Изучайте английский легко",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = tabPositions[selectedTab].left)
                            .width(tabPositions[selectedTab].width)
                            .height(3.dp)
                            .background(PrimaryCyan)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    selectedContentColor = PrimaryCyan,
                    unselectedContentColor = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                AuthForm(
                    email = state.email,
                    password = state.password,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSubmit = if (selectedTab == 0) onLogin else onRegister,
                    isLoading = state.isLoading,
                    error = state.error,
                    isLogin = selectedTab == 0
                )

                // Google Sign-In button — shown only if client ID is configured
                if (BuildConfig.GOOGLE_CLIENT_ID.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                        Text(
                            text = "  или  ",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderLight)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                            .background(BackgroundPrimary)
                            .clickable(enabled = !state.isLoading) {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    googleLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = "G", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                            Text(
                                text = "Войти через Google",
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedTab == 0) {
            Text(
                text = "Нет аккаунта? Перейдите на вкладку \"Регистрация\"",
                fontSize = 13.sp,
                color = TextTertiary
            )
        } else {
            Text(
                text = "Уже есть аккаунт? Перейдите на вкладку \"Вход\"",
                fontSize = 13.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun AuthForm(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    error: String?,
    isLogin: Boolean
) {
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            placeholder = { Text("example@email.com") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Email", tint = PrimaryCyan)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BackgroundSecondary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderLight,
                cursorColor = PrimaryCyan
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль") },
            placeholder = { Text("Минимум 6 символов") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Пароль", tint = PrimaryCyan)
            },
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "👁" else "👁‍🗨",
                        fontSize = 20.sp,
                        color = TextTertiary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BackgroundSecondary,
                unfocusedContainerColor = BackgroundSecondary,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = BorderLight,
                cursorColor = PrimaryCyan
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus(); onSubmit() }
            )
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = error, color = Error, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(24.dp))

        val submitEnabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (submitEnabled) listOf(PrimaryBright, PrimaryCyan)
                                 else listOf(PrimaryBright.copy(alpha = 0.5f), PrimaryCyan.copy(alpha = 0.5f))
                    )
                )
                .clickable(enabled = submitEnabled) { onSubmit() },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = if (isLogin) "Войти" else "Зарегистрироваться",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
