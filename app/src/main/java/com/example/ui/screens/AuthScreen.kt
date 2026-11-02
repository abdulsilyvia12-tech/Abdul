package com.example.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.data.model.CustomerTier
import com.example.data.model.User
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(
    onLoginSuccess: (identifier: String) -> Unit,
    onCreateAccountSuccess: (username: String, displayName: String, email: String, bio: String) -> Unit,
    onOpenCrm: (() -> Unit)? = null,
    onContinueOffline: (() -> Unit)? = null,
    onStartAnonymousBlink: () -> Unit = {},
    demoUsers: List<User>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Portal Navigation Tabs: 0: Customer Sign In, 1: Quick Customer Access, 2: New Customer
    var selectedPortalTab by remember { mutableStateOf(0) }

    // Login state
    var loginIdentifier by remember { mutableStateOf("alex") }
    var loginPassword by remember { mutableStateOf("Snail1961") }

    // Sign up state
    var signupUsername by remember { mutableStateOf("") }
    var signupDisplayName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupBio by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf(CustomerTier.PRO) }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun performGoogleSignIn() {
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            errorMessage = "Google Web Client ID missing"
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        coroutineScope.launch {
            try {
                isGoogleLoading = true
                val result = credentialManager.getCredential(context as Activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        onLoginSuccess(firebaseUser.displayName ?: firebaseUser.email ?: "google_user")
                    }
                } else {
                    errorMessage = "Unexpected credential format"
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
                isGoogleLoading = false
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                errorMessage = e.localizedMessage ?: "Google Sign-In unavailable on this device"
                isGoogleLoading = false
            } finally {
                isGoogleLoading = false
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
            Spacer(modifier = Modifier.height(12.dp))

            // App Brand Logo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(BlinkCyanLight, BlinkPrimary, BlinkDarkBg)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Blinkchat Logo",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Blinkchat Customer Portal",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Universal login portal for customers to get full access to the app",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Instant Anonymous Random Chat Entry (No Profile Needed)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BlinkPrimary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, BlinkCyan.copy(alpha = 0.6f)),
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
                                    text = "No Profile",
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

            // Portal Segmented Tabs
            TabRow(
                selectedTabIndex = selectedPortalTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BlinkPrimaryLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedPortalTab == 0,
                    onClick = { selectedPortalTab = 0 },
                    text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_customer_signin")
                )
                Tab(
                    selected = selectedPortalTab == 1,
                    onClick = { selectedPortalTab = 1 },
                    text = { Text("Quick Access", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_quick_access")
                )
                Tab(
                    selected = selectedPortalTab == 2,
                    onClick = { selectedPortalTab = 2 },
                    text = { Text("New Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_new_customer")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error banner if any
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = BlinkPink.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = BlinkPinkLight,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // --- Tab 0: Customer Sign In ---
            if (selectedPortalTab == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Customer Account Login",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sign in to unlock full app privileges, feeds, stories, and chats",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = loginIdentifier,
                            onValueChange = { loginIdentifier = it },
                            label = { Text("Customer Username or Email") },
                            placeholder = { Text("e.g. alex or alex@blinkchat.com") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BlinkPrimaryLight) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password (or Snail1961)") },
                            placeholder = { Text("Enter password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BlinkPrimaryLight) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (loginIdentifier.isBlank()) {
                                    errorMessage = "Please enter your customer username or email."
                                } else {
                                    onLoginSuccess(loginIdentifier.trim())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In to Full Access", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Google Sign In
                        OutlinedButton(
                            onClick = { performGoogleSignIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_signin_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGoogleLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("G", fontWeight = FontWeight.Black, color = BlinkPrimaryLight, fontSize = 17.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // --- Tab 1: Quick Customer Access ---
            if (selectedPortalTab == 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Customer Profile for Full Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Tap any verified customer account to launch full app access instantly:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                            Text(text = "@${user.username} • 📍 ${user.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = { onLoginSuccess(user.username) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Tab 2: New Customer Registration ---
            if (selectedPortalTab == 2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Register New Customer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Create your customer profile with instant full app access",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = signupDisplayName,
                            onValueChange = { signupDisplayName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Jordan Blake") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signupUsername,
                            onValueChange = { signupUsername = it },
                            label = { Text("Username") },
                            placeholder = { Text("e.g. jordan_b") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { signupEmail = it },
                            label = { Text("Customer Email") },
                            placeholder = { Text("e.g. jordan@example.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = signupBio,
                            onValueChange = { signupBio = it },
                            label = { Text("Bio / Status (Optional)") },
                            placeholder = { Text("Exploring the Blink community ✨") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_bio_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (signupUsername.isBlank() || signupEmail.isBlank()) {
                                    errorMessage = "Please enter both a username and email."
                                } else {
                                    onCreateAccountSuccess(
                                        signupUsername.trim(),
                                        signupDisplayName.ifBlank { signupUsername.trim() },
                                        signupEmail.trim(),
                                        signupBio
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create & Get Full Access", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Offline Cache Mode button
            if (onContinueOffline != null) {
                OutlinedButton(
                    onClick = onContinueOffline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("offline_mode_portal_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = BlinkCyanLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continue with Offline Cached Data ⚡", fontSize = 12.sp, color = BlinkCyanLight)
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Admin CRM Dashboard access link
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
}
