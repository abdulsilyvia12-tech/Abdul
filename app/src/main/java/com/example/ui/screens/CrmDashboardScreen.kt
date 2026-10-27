package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmDashboardScreen(
    customers: List<CustomerAccount>,
    payments: List<PaymentRecord>,
    metrics: CrmMetrics,
    onRecordPayment: (customerId: String, customerName: String, customerEmail: String, planName: String, amount: Double, status: PaymentStatus, method: String) -> Unit,
    onUpdateCustomerTier: (userId: String, newTier: CustomerTier) -> Unit,
    onToggleCustomerActive: (userId: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Password protection state for Snail1961
    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Dashboard tabs
    var selectedTab by remember { mutableStateOf(0) } // 0: Customers, 1: Payments, 2: Analytics
    var customerSearch by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf<CustomerTier?>(null) }
    var showRecordPaymentDialog by remember { mutableStateOf(false) }

    // If locked, render Password Gate
    if (!isUnlocked) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .testTag("crm_password_gate"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(BlinkPrimary, BlinkPink))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "CRM Security",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Admin CRM Security Portal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter master password to access active users telemetry & payments ledger.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = null
                        },
                        label = { Text("Master Password") },
                        placeholder = { Text("Enter CRM password") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = BlinkPrimaryLight)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = {
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = BlinkPink)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_password_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (passwordInput == "Snail1961") {
                                isUnlocked = true
                                passwordError = null
                            } else {
                                passwordError = "Incorrect password. Please verify the master access key."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("crm_unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock CRM Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("crm_cancel_button")
                    ) {
                        Text("Back to App", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        return
    }

    // --- Unlocked CRM Dashboard ---
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("crm_dashboard_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // CRM Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = BlinkPrimaryLight,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CRM & Revenue",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Live Active Users & Payments Telemetry",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Lock button
                        FilledTonalButton(
                            onClick = { isUnlocked = false },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("crm_lock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock", fontSize = 12.sp)
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close CRM")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Key Metrics Overview Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Active Users Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(BlinkGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Active Users",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${metrics.activeUsersCount} online",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${metrics.totalCustomers} total accounts",
                                fontSize = 10.sp,
                                color = BlinkCyanLight
                            )
                        }
                    }

                    // Total Revenue Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = BlinkGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Total Payments",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", metrics.totalRevenue)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", metrics.monthlyRecurringRevenue)} MRR",
                                fontSize = 10.sp,
                                color = BlinkGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Segmented Tabs: 0: Active Users, 1: Payments, 2: Analytics
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BlinkPrimaryLight,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active Users (${customers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Payments (${payments.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // --- Tab 0: Active Users / Customers ---
            if (selectedTab == 0) {
                item {
                    // Search & Tier filter
                    OutlinedTextField(
                        value = customerSearch,
                        onValueChange = { customerSearch = it },
                        placeholder = { Text("Search customers by name, @username, email...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("crm_customer_search"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTierFilter == null,
                            onClick = { selectedTierFilter = null },
                            label = { Text("All Tiers", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        CustomerTier.values().forEach { tier ->
                            FilterChip(
                                selected = selectedTierFilter == tier,
                                onClick = { selectedTierFilter = if (selectedTierFilter == tier) null else tier },
                                label = { Text(tier.label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                val filteredCustomers = customers.filter { cust ->
                    val matchesSearch = cust.displayName.contains(customerSearch, ignoreCase = true) ||
                            cust.username.contains(customerSearch, ignoreCase = true) ||
                            cust.email.contains(customerSearch, ignoreCase = true) ||
                            cust.location.contains(customerSearch, ignoreCase = true)
                    val matchesTier = selectedTierFilter == null || cust.tier == selectedTierFilter
                    matchesSearch && matchesTier
                }

                items(filteredCustomers, key = { it.userId }) { cust ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("crm_customer_item_${cust.username}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarBadge(
                                        name = cust.displayName,
                                        colorHex = cust.avatarColorHex,
                                        size = 42.dp,
                                        showOnline = true,
                                        isOnline = cust.isOnline
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cust.displayName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(android.graphics.Color.parseColor(cust.tier.badgeColorHex)).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = cust.tier.label,
                                                    fontSize = 10.sp,
                                                    color = Color(android.graphics.Color.parseColor(cust.tier.badgeColorHex)),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "@${cust.username} • ${cust.email}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", cust.totalSpent)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = BlinkGreen
                                    )
                                    Text(
                                        text = if (cust.isOnline) "🟢 Online" else "⚪ Offline",
                                        fontSize = 10.sp,
                                        color = if (cust.isOnline) BlinkGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 ${cust.location} • Joined ${cust.joinedDate}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Tier Upgrade button
                                    OutlinedButton(
                                        onClick = {
                                            val nextTier = when (cust.tier) {
                                                CustomerTier.FREE -> CustomerTier.PRO
                                                CustomerTier.PRO -> CustomerTier.CREATOR
                                                CustomerTier.CREATOR -> CustomerTier.VIP
                                                CustomerTier.VIP -> CustomerTier.ENTERPRISE
                                                CustomerTier.ENTERPRISE -> CustomerTier.PRO
                                            }
                                            onUpdateCustomerTier(cust.userId, nextTier)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Change Tier", fontSize = 11.sp)
                                    }

                                    FilledTonalButton(
                                        onClick = { onToggleCustomerActive(cust.userId) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (cust.isActive) BlinkGreen.copy(alpha = 0.15f) else BlinkPink.copy(alpha = 0.15f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (cust.isActive) "Active" else "Disabled",
                                            fontSize = 11.sp,
                                            color = if (cust.isActive) BlinkGreen else BlinkPink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Tab 1: Payments & Transactions ---
            if (selectedTab == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Payments Ledger",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${payments.size} recorded subscription transactions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showRecordPaymentDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("record_payment_button")
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(payments, key = { it.id }) { pm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("payment_row_${pm.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (pm.status) {
                                                PaymentStatus.COMPLETED -> BlinkGreen.copy(alpha = 0.15f)
                                                PaymentStatus.PENDING -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                PaymentStatus.REFUNDED -> BlinkPink.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (pm.status) {
                                            PaymentStatus.COMPLETED -> Icons.Default.CheckCircle
                                            PaymentStatus.PENDING -> Icons.Default.Pending
                                            PaymentStatus.REFUNDED -> Icons.Default.Replay
                                        },
                                        contentDescription = null,
                                        tint = when (pm.status) {
                                            PaymentStatus.COMPLETED -> BlinkGreen
                                            PaymentStatus.PENDING -> Color(0xFFF59E0B)
                                            PaymentStatus.REFUNDED -> BlinkPink
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = pm.customerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${pm.planName} • ${pm.paymentMethod}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = pm.dateFormatted,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+$${String.format(Locale.US, "%.2f", pm.amount)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = when (pm.status) {
                                        PaymentStatus.COMPLETED -> BlinkGreen
                                        PaymentStatus.PENDING -> Color(0xFFF59E0B)
                                        PaymentStatus.REFUNDED -> BlinkPink
                                    }
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (pm.status) {
                                        PaymentStatus.COMPLETED -> BlinkGreen.copy(alpha = 0.2f)
                                        PaymentStatus.PENDING -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        PaymentStatus.REFUNDED -> BlinkPink.copy(alpha = 0.2f)
                                    }
                                ) {
                                    Text(
                                        text = pm.status.label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (pm.status) {
                                            PaymentStatus.COMPLETED -> BlinkGreen
                                            PaymentStatus.PENDING -> Color(0xFFF59E0B)
                                            PaymentStatus.REFUNDED -> BlinkPink
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Tab 2: Analytics & Breakdown ---
            if (selectedTab == 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Subscription Tiers Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            CustomerTier.values().forEach { tier ->
                                val count = customers.count { it.tier == tier }
                                val pct = if (customers.isNotEmpty()) count.toFloat() / customers.size else 0f

                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "${tier.label} ($${tier.priceMonthly}/mo)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(text = "$count users (${(pct * 100).toInt()}%)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(android.graphics.Color.parseColor(tier.badgeColorHex)),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Customer Health & Retention", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "• Daily Active Users (DAU): ${metrics.activeUsersCount}", fontSize = 13.sp)
                            Text(text = "• Average Revenue Per User (ARPU): $${String.format(Locale.US, "%.2f", metrics.averageRevenuePerUser)}", fontSize = 13.sp)
                            Text(text = "• Paid Subscriptions Ratio: 88.5%", fontSize = 13.sp)
                            Text(text = "• Churn Rate: 1.2% (Industry Low)", fontSize = 13.sp)
                            Text(text = "• Offline Cache Availability: 100% Guaranteed", fontSize = 13.sp, color = BlinkCyanLight)
                        }
                    }
                }
            }
        }

        // --- Record New Payment Dialog ---
        if (showRecordPaymentDialog) {
            var inputCustomerName by remember { mutableStateOf("Maya Lin") }
            var inputCustomerEmail by remember { mutableStateOf("maya@blinkchat.com") }
            var inputPlanName by remember { mutableStateOf("Blink Creator Pass") }
            var inputAmount by remember { mutableStateOf("19.99") }
            var inputMethod by remember { mutableStateOf("Google Pay") }

            AlertDialog(
                onDismissRequest = { showRecordPaymentDialog = false },
                title = { Text("Record Customer Payment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = inputCustomerName,
                            onValueChange = { inputCustomerName = it },
                            label = { Text("Customer Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputCustomerEmail,
                            onValueChange = { inputCustomerEmail = it },
                            label = { Text("Customer Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputPlanName,
                            onValueChange = { inputPlanName = it },
                            label = { Text("Plan / Service") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("Amount ($ USD)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputMethod,
                            onValueChange = { inputMethod = it },
                            label = { Text("Payment Method") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedAmount = inputAmount.toDoubleOrNull() ?: 19.99
                            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
                            onRecordPayment(
                                "cust_" + System.currentTimeMillis().toString().takeLast(6),
                                inputCustomerName,
                                inputCustomerEmail,
                                inputPlanName,
                                parsedAmount,
                                PaymentStatus.COMPLETED,
                                inputMethod
                            )
                            showRecordPaymentDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary)
                    ) {
                        Text("Record")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecordPaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
