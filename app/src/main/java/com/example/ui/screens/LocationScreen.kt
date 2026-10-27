package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    currentUser: User?,
    friends: List<User>,
    onUpdateLocation: (locationName: String, coordinates: String) -> Unit,
    onToggleGhostMode: (Boolean) -> Unit,
    onStartChat: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf("All") }
    var showCityPicker by remember { mutableStateOf(false) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Major global cities database with real lat/long coordinates
    val globalCities = remember {
        listOf(
            Triple("Tokyo, Japan", 35.6762, 139.6503),
            Triple("San Francisco, USA", 37.7749, -122.4194),
            Triple("Berlin, Germany", 52.5200, 13.4050),
            Triple("London, UK", 51.5074, -0.1278),
            Triple("Rio de Janeiro, Brazil", -22.9068, -43.1729),
            Triple("Paris, France", 48.8566, 2.3522),
            Triple("New York, USA", 40.7128, -74.0060),
            Triple("Seoul, South Korea", 37.5665, 126.9780),
            Triple("Singapore", 1.3521, 103.8198),
            Triple("Sydney, Australia", -33.8688, 151.2093),
            Triple("Dubai, UAE", 25.2048, 55.2708),
            Triple("Nairobi, Kenya", -1.2921, 36.8219),
            Triple("Toronto, Canada", 43.6532, -79.3832),
            Triple("Mumbai, India", 19.0760, 72.8777)
        )
    }

    // Helper to calculate distance in km using Haversine formula
    fun calculateDistance(userCoords: String?, friendCoords: String?): String {
        try {
            if (userCoords.isNullOrBlank() || friendCoords.isNullOrBlank()) return "Worldwide"
            val lat1 = userCoords.substringBefore("°").trim().toDoubleOrNull() ?: 35.67
            val lon1 = userCoords.substringAfter(",").substringBefore("°").trim().toDoubleOrNull() ?: 139.65
            val lat2 = friendCoords.substringBefore("°").trim().toDoubleOrNull() ?: 0.0
            val lon2 = friendCoords.substringAfter(",").substringBefore("°").trim().toDoubleOrNull() ?: 0.0

            val r = 6371.0 // Radius of earth in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val d = (r * c).roundToInt()
            return if (d < 5) "Nearby (<5 km)" else "$d km away"
        } catch (e: Exception) {
            return "Global"
        }
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                isDetectingGps = true
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (loc != null) {
                        val latStr = String.format(Locale.US, "%.4f° %s", abs(loc.latitude), if (loc.latitude >= 0) "N" else "S")
                        val lonStr = String.format(Locale.US, "%.4f° %s", abs(loc.longitude), if (loc.longitude >= 0) "E" else "W")
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        val cityName = addresses?.firstOrNull()?.let {
                            val city = it.locality ?: it.subAdminArea ?: it.adminArea
                            val country = it.countryName ?: ""
                            if (city != null) "$city, $country" else country
                        } ?: "Current Location"
                        onUpdateLocation(cityName, "$latStr, $lonStr")
                    } else {
                        onUpdateLocation("Tokyo, Japan", "35.6762° N, 139.6503° E")
                    }
                } catch (e: Exception) {
                    onUpdateLocation("Tokyo, Japan", "35.6762° N, 139.6503° E")
                } finally {
                    isDetectingGps = false
                    showCityPicker = false
                }
            }
        }
    }

    val filteredFriends = remember(friends, selectedFilter, searchQuery) {
        friends.filter { friend ->
            val matchesSearch = friend.displayName.contains(searchQuery, ignoreCase = true) ||
                    friend.username.contains(searchQuery, ignoreCase = true) ||
                    friend.location.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Online" -> friend.isOnline
                "Nearby" -> !friend.isGhostMode
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("location_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header: Location Feature Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = BlinkCyanLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Worldwide Location Radar",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Text(
                            text = "Find friends across global cities & share your location",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Ghost Mode quick toggle pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentUser?.isGhostMode == true) BlinkPink.copy(alpha = 0.2f) else BlinkCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (currentUser?.isGhostMode == true) BlinkPink else BlinkCyanLight
                        ),
                        modifier = Modifier
                            .clickable {
                                onToggleGhostMode(!(currentUser?.isGhostMode ?: false))
                            }
                            .testTag("ghost_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (currentUser?.isGhostMode == true) "👻 Ghost ON" else "📍 Public",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser?.isGhostMode == true) BlinkPinkLight else BlinkCyanLight
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Current User Location Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_user_location_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BlinkPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(listOf(BlinkPrimary, BlinkCyan, BlinkPink, BlinkPrimary))
                                    )
                                    .padding(2.5.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                AvatarBadge(
                                    name = currentUser?.displayName ?: "Me",
                                    colorHex = currentUser?.avatarColorHex ?: "#8B5CF6",
                                    size = 46.dp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Your Current Location",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currentUser?.location ?: "Tokyo, Japan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentUser?.coordinates ?: "35.6762° N, 139.6503° E",
                                    fontSize = 11.sp,
                                    color = BlinkCyanLight,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Button(
                            onClick = { showCityPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("change_location_button")
                        ) {
                            Icon(Icons.Default.EditLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search friends or cities...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_search_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BlinkCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Online", "Nearby").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlinkCyan.copy(alpha = 0.2f),
                                selectedLabelColor = BlinkCyanLight
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Section Title: Friends on the Radar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Global Friends Radar (${filteredFriends.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "⚡ Real-time telemetry",
                        fontSize = 11.sp,
                        color = BlinkCyanLight
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Friends List with Distance & Location
            if (filteredFriends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No friends match your location filter.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredFriends, key = { it.id }) { friend ->
                    val distanceText = calculateDistance(currentUser?.coordinates, friend.coordinates)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .testTag("friend_location_card_${friend.username}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarBadge(
                                    name = friend.displayName,
                                    colorHex = friend.avatarColorHex,
                                    size = 46.dp,
                                    showOnline = true,
                                    isOnline = friend.isOnline
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = friend.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (friend.isGhostMode) {
                                            Text(text = "👻", fontSize = 12.sp)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = BlinkPinkLight,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = friend.location,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "📏 $distanceText • ${friend.statusText}",
                                        fontSize = 11.sp,
                                        color = BlinkCyanLight,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onStartChat(friend) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BlinkPrimary.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = "Chat",
                                        tint = BlinkPrimaryLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Worldwide City Picker Modal ---
        if (showCityPicker) {
            AlertDialog(
                onDismissRequest = { showCityPicker = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TravelExplore, contentDescription = null, tint = BlinkCyanLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Worldwide City", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // GPS Auto-detect button
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("gps_auto_detect_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BlinkCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isDetectingGps) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detecting GPS...")
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Detect GPS Location", fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "Or choose a global city:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            items(globalCities) { (city, lat, lon) ->
                                val latStr = String.format(Locale.US, "%.4f° %s", abs(lat), if (lat >= 0) "N" else "S")
                                val lonStr = String.format(Locale.US, "%.4f° %s", abs(lon), if (lon >= 0) "E" else "W")
                                val coordsStr = "$latStr, $lonStr"

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onUpdateLocation(city, coordsStr)
                                            showCityPicker = false
                                        }
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (currentUser?.location == city) BlinkPrimary.copy(alpha = 0.15f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = city, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(text = coordsStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (currentUser?.location == city) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BlinkPrimaryLight, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCityPicker = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
