package com.example.data.model

enum class PaymentStatus(val label: String, val colorHex: String) {
    COMPLETED("Paid", "#10B981"),
    PENDING("Pending", "#F59E0B"),
    REFUNDED("Refunded", "#EF4444")
}

enum class CustomerTier(val label: String, val priceMonthly: Double, val badgeColorHex: String) {
    FREE("Free Tier", 0.0, "#6B7280"),
    PRO("Blink Pro", 9.99, "#8B5CF6"),
    CREATOR("Creator Pass", 19.99, "#EC4899"),
    VIP("VIP Gold", 49.99, "#F59E0B"),
    ENTERPRISE("Enterprise", 199.99, "#06B6D4")
}

data class PaymentRecord(
    val id: String,
    val customerId: String,
    val customerName: String,
    val customerEmail: String,
    val planName: String,
    val amount: Double,
    val currency: String = "USD",
    val status: PaymentStatus = PaymentStatus.COMPLETED,
    val dateFormatted: String,
    val paymentMethod: String = "Visa •••• 4242",
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomerAccount(
    val userId: String,
    val username: String,
    val displayName: String,
    val email: String,
    val tier: CustomerTier = CustomerTier.PRO,
    val isActive: Boolean = true,
    val isOnline: Boolean = true,
    val location: String = "Tokyo, Japan",
    val totalSpent: Double = 29.97,
    val joinedDate: String = "Oct 2026",
    val lastActive: String = "Active now",
    val avatarColorHex: String = "#8B5CF6"
)

data class CrmMetrics(
    val totalCustomers: Int,
    val activeUsersCount: Int,
    val newTodayCount: Int,
    val totalRevenue: Double,
    val monthlyRecurringRevenue: Double,
    val averageRevenuePerUser: Double,
    val paidTransactionsCount: Int
)
