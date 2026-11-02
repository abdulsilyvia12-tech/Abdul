package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.User
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Production-ready Firebase Authentication Login & Registration Screen for Blinkchat.
 * Features:
 * 1. Firebase Email & Password Sign-In with validation and error states
 * 2. Firebase Email & Password Account Registration with display name update
 * 3. Google Sign-In via Jetpack CredentialManager + FirebaseAuth
 * 4. Password Reset flow via Firebase sendPasswordResetEmail
 * 5. Quick Demo Profile & Offline Cache Access bypass
 */
@Composable
fun AuthScreen(
    onLoginSuccess: (identifier: String) -> Unit,
    onFirebaseLoginSuccess: ((FirebaseUser) -> Unit)? = null,
    onCreateAccountSuccess: (username: String, displayName: String, email: String, bio: String) -> Unit,
    onOpenCrm: (() -> Unit)? = null,
    onContinueOffline: (() -> Unit)? = null,
    onStartAnonymousBlink: () -> Unit = {},
    demoUsers: List<User>
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Segmented Navigation Tabs: 0 = Sign In, 1 = Create Account, 2 = Quick Demo
    var selectedTab by remember { mutableIntStateOf(0) }

    // Sign In State
    var emailInput by remember { mutableStateOf("alex@blinkchat.com") }
    var passwordInput by remember { mutableStateOf("Snail1961") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Sign Up State
    var registerDisplayName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerConfirmPassword by remember { mutableStateOf("") }
    var registerBio by remember { mutableStateOf("") }
    var isRegisterPasswordVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    // Google Sign In State
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Password Reset Dialog State
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isSendingResetEmail by remember { mutableStateOf(false) }

    // Status Feedback
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // --- Firebase Authentication Functions ---

    // 1. Firebase Email / Password Sign-In
    fun performEmailPasswordSignIn() {
        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isBlank()) {
            errorMessage = "Please enter your email address."
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Please enter a valid email address (e.g. name@example.com)."
            return
        }
        if (password.isBlank()) {
            errorMessage = "Please enter your password."
            return
        }

        coroutineScope.launch {
            try {
                isSigningIn = true
                errorMessage = null
                successMessage = null

                val authResult = Firebase.auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    if (onFirebaseLoginSuccess != null) {
                        onFirebaseLoginSuccess(firebaseUser)
                    } else {
                        onLoginSuccess(firebaseUser.displayName ?: firebaseUser.email ?: email)
                    }
                } else {
                    errorMessage = "Firebase user session not found. Please try again."
                }
            } catch (e: FirebaseAuthInvalidUserException) {
                errorMessage = "No account found for $email. Switch to 'Create Account' to register."
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                errorMessage = "Invalid password or email. Please check your credentials and retry."
            } catch (e: FirebaseNetworkException) {
                errorMessage = "Network error: Unable to reach Firebase. Please check your internet connection."
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Sign in error", e)
                errorMessage = e.localizedMessage ?: "Authentication failed. Please verify credentials."
            } finally {
                isSigningIn = false
            }
        }
    }

    // 2. Firebase Email / Password Account Creation
    fun performEmailPasswordRegistration() {
        val name = registerDisplayName.trim()
        val email = registerEmail.trim()
        val password = registerPassword.trim()
        val confirmPassword = registerConfirmPassword.trim()

        if (name.isBlank()) {
            errorMessage = "Please enter your full name."
            return
        }
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            errorMessage = "Firebase requires password to be at least 6 characters."
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match. Please verify your password entry."
            return
        }

        coroutineScope.launch {
            try {
                isRegistering = true
                errorMessage = null
                successMessage = null

                val authResult = Firebase.auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Update user profile display name
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        firebaseUser.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w("FirebaseAuth", "Profile display name update non-fatal error: ${pe.message}")
                    }

                    if (onFirebaseLoginSuccess != null) {
                        onFirebaseLoginSuccess(firebaseUser)
                    } else {
                        val username = email.substringBefore("@").lowercase().ifBlank { "user_${firebaseUser.uid.take(6)}" }
                        onCreateAccountSuccess(username, name, email, registerBio)
                    }
                }
            } catch (e: FirebaseAuthUserCollisionException) {
                errorMessage = "An account with email $email already exists. Please switch to Sign In."
            } catch (e: FirebaseAuthWeakPasswordException) {
                errorMessage = "Password is too weak. Please use a mix of letters and numbers."
            } catch (e: FirebaseNetworkException) {
                errorMessage = "Network connection failed. Please check your internet connection."
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Registration error", e)
                errorMessage = e.localizedMessage ?: "Account registration failed. Please try again."
            } finally {
                isRegistering = false
            }
        }
    }

    // 3. Google Sign-In with CredentialManager + FirebaseAuth
    fun performGoogleSignIn() {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            errorMessage = "Google Web Client ID missing in application configuration."
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        coroutineScope.launch {
            try {
                isGoogleLoading = true
                errorMessage = null
                successMessage = null

                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        if (onFirebaseLoginSuccess != null) {
                            onFirebaseLoginSuccess(firebaseUser)
                        } else {
                            onLoginSuccess(firebaseUser.displayName ?: firebaseUser.email ?: "google_user")
                        }
                    }
                } else {
                    errorMessage = "Unexpected credential format returned from Google."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("FirebaseAuth", "Google Sign-In cancelled by user")
            } catch (e: Exception) {
                Log.e("FirebaseAuth", "Google Sign-In failed", e)
                errorMessage = e.localizedMessage ?: "Google Sign-In unavailable on this device. Use Email/Password or Demo login."
            } finally {
                isGoogleLoading = false
            }
        }
    }

    // 4. Password Reset via Firebase Auth
    fun sendPasswordReset() {
        val email = forgotPasswordEmail.trim()
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Please enter a valid email address for password reset."
            return
        }

        coroutineScope.launch {
            try {
                isSendingResetEmail = true
                Firebase.auth.sendPasswordResetEmail(email).await()
                showForgotPasswordDialog = false
                successMessage = "Password reset link sent to $email! Check your inbox."
                forgotPasswordEmail = ""
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Failed to send password reset email."
            } finally {
                isSendingResetEmail = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // --- App Brand Logo & Firebase Auth Security Badge ---
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BlinkCyanLight, BlinkPrimary, BlinkDarkBg)
                        )
                    )
                    .border(2.dp, BlinkCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Firebase Auth Logo",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Blinkchat Authentication",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Firebase Security Badge Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = BlinkPrimary.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, BlinkCyan.copy(alpha = 0.4f)),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = BlinkCyanLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Firebase Auth • Cloud Identity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BlinkCyanLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Instant Anonymous Random Chat Entry (No Profile Needed) ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BlinkPrimary.copy(alpha = 0.15f),
                border = BorderStroke(1.2.dp, BlinkCyan.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartAnonymousBlink() }
                    .testTag("auth_start_anonymous_blink_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(BlinkPrimary, BlinkPink, BlinkCyan))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Blink Match ⚡",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BlinkPink.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "No Account Needed",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlinkPinkLight,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real-time 2-min anonymous random chat",
                            fontSize = 11.sp,
                            color = BlinkCyanLight
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = BlinkCyanLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Segmented Tab Row: Sign In vs Create Account vs Quick Demo ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BlinkPrimaryLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        errorMessage = null
                    },
                    text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_customer_signin")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        errorMessage = null
                    },
                    text = { Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_new_customer")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        errorMessage = null
                    },
                    text = { Text("Quick Demo", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_quick_access")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Feedback Banners (Error or Success) ---
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("auth_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = BlinkPink.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, BlinkPink.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = BlinkPinkLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = BlinkPinkLight,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = BlinkPinkLight, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("auth_success_banner"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { successMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // =================================================================
            // TAB 0: FIREBASE EMAIL & PASSWORD SIGN-IN + GOOGLE SIGN-IN
            // =================================================================
            if (selectedTab == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Email & Password Login",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Authenticate securely with your Firebase credentials",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Email Field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. alex@blinkchat.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            trailingIcon = {
                                if (emailInput.isNotBlank()) {
                                    IconButton(onClick = { emailInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password Field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    performEmailPasswordSignIn()
                                }
                            )
                        )

                        // Forgot Password Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    forgotPasswordEmail = emailInput
                                    showForgotPasswordDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    color = BlinkCyanLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Sign In with Firebase Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                performEmailPasswordSignIn()
                            },
                            enabled = !isSigningIn && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying with Firebase...", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Email", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Divider with text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text(
                                text = "  OR  ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Sign-In Button (Jetpack CredentialManager)
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                performGoogleSignIn()
                            },
                            enabled = !isSigningIn && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, BlinkCyan.copy(alpha = 0.6f))
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BlinkCyanLight,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting to Google...", fontSize = 13.sp)
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Google Multi-color G logo badge
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color(0xFF4285F4)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Sign In with Google",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 1: FIREBASE EMAIL & PASSWORD ACCOUNT REGISTRATION
            // =================================================================
            if (selectedTab == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Create Firebase Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Register a new user account with Firebase Authentication",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Full Name Field
                        OutlinedTextField(
                            value = registerDisplayName,
                            onValueChange = {
                                registerDisplayName = it
                                errorMessage = null
                            },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Jordan Blake") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Email Field
                        OutlinedTextField(
                            value = registerEmail,
                            onValueChange = {
                                registerEmail = it
                                errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("e.g. jordan@example.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = registerPassword,
                            onValueChange = {
                                registerPassword = it
                                errorMessage = null
                            },
                            label = { Text("Password (min. 6 chars)") },
                            placeholder = { Text("Create password") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isRegisterPasswordVisible = !isRegisterPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isRegisterPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (isRegisterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = registerConfirmPassword,
                            onValueChange = {
                                registerConfirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            leadingIcon = {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            visualTransformation = if (isRegisterPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_confirm_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Bio / Status Field
                        OutlinedTextField(
                            value = registerBio,
                            onValueChange = { registerBio = it },
                            label = { Text("Bio / Status (Optional)") },
                            placeholder = { Text("Blinking on the social feed ✨") },
                            leadingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = BlinkPrimaryLight)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_bio_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                performEmailPasswordRegistration()
                            })
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Create Account Submit Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                performEmailPasswordRegistration()
                            },
                            enabled = !isRegistering && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRegistering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registering in Firebase...", fontSize = 14.sp)
                            } else {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Firebase Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Sign In fallback for new accounts
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                performGoogleSignIn()
                            },
                            enabled = !isRegistering && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signup_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, BlinkCyan.copy(alpha = 0.6f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("G", fontWeight = FontWeight.Black, color = Color(0xFF4285F4), fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Or Sign Up with Google", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 2: QUICK DEMO PROFILES (Instant Testing Bypass)
            // =================================================================
            if (selectedTab == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = BlinkCyanLight, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instant Demo Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "Tap any verified customer account for 1-tap testing without entering passwords:",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val customerList = demoUsers.ifEmpty {
                            listOf(
                                User("user_me", "alex", "Alex Rivers", "alex@blinkchat.com", avatarColorHex = "#8B5CF6", location = "Tokyo, Japan"),
                                User("user_maya", "maya_lin", "Maya Lin", "maya@blinkchat.com", avatarColorHex = "#EC4899", location = "San Francisco, USA"),
                                User("user_kai", "kai_storm", "Kai Storm", "kai@blinkchat.com", avatarColorHex = "#06B6D4", location = "Berlin, Germany"),
                                User("user_elena", "elena_v", "Elena Vance", "elena@blinkchat.com", avatarColorHex = "#10B981", location = "London, UK"),
                                User("user_lucas", "lucas_silva", "Lucas Silva", "lucas@blinkchat.com", avatarColorHex = "#F59E0B", location = "Rio de Janeiro, Brazil")
                            )
                        }

                        customerList.forEach { user ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onLoginSuccess(user.username) }
                                    .testTag("quick_login_${user.username}"),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AvatarBadge(name = user.displayName, colorHex = user.avatarColorHex, size = 42.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "${user.email} • 📍 ${user.location}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { onLoginSuccess(user.username) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Launch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // --- Offline Mode Fallback ---
            if (onContinueOffline != null) {
                OutlinedButton(
                    onClick = onContinueOffline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_mode_portal_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BlinkCyanLight.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = BlinkCyanLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Offline Cached Data ⚡", fontSize = 12.sp, color = BlinkCyanLight)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // --- Admin CRM Dashboard Link ---
            if (onOpenCrm != null) {
                TextButton(
                    onClick = onOpenCrm,
                    modifier = Modifier.testTag("portal_crm_link")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = BlinkPrimaryLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin CRM Dashboard (Password: Snail1961)", fontSize = 12.sp, color = BlinkPrimaryLight)
                }
            }
        }
    }

    // --- Forgot Password Dialog ---
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingResetEmail) showForgotPasswordDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = BlinkPrimaryLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Password", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your registered email address and Firebase will send a secure password reset link to your inbox.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Registered Email") },
                        placeholder = { Text("e.g. name@example.com") },
                        modifier = Modifier.fillMaxWidth().testTag("forgot_password_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { sendPasswordReset() },
                    enabled = !isSendingResetEmail,
                    colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                    modifier = Modifier.testTag("send_password_reset_button")
                ) {
                    if (isSendingResetEmail) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending...")
                    } else {
                        Text("Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSendingResetEmail
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
