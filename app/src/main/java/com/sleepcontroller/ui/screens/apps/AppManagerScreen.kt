package com.sleepcontroller.ui.screens.apps

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepcontroller.data.db.entity.BlockedApp
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.theme.*

@Composable
fun AppManagerScreen(
    viewModel: AppManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Text(
            text = "App Manager",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Choose which apps to block during sleep",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearch(it) },
            placeholder = { Text("Search apps...", color = TextTertiary) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextTertiary)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftPurple,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = SoftPurple,
                focusedContainerColor = DeepNight,
                unfocusedContainerColor = DeepNight
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip("Block Social", SleepRed) { viewModel.blockAllSocial() }
            PresetChip("Block Games", SleepRed) { viewModel.blockAllGames() }
            PresetChip("Block All", SleepRed) { viewModel.blockAll() }
            PresetChip("Unblock All", SleepGreen) { viewModel.unblockAll() }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "All", "blocked" to "Blocked", "whitelisted" to "Allowed").forEach { (key, label) ->
                val selected = uiState.selectedFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setFilter(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SoftPurple.copy(alpha = 0.2f),
                        selectedLabelColor = SoftPurple,
                        containerColor = GlassWhite,
                        labelColor = TextTertiary
                    )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${filteredApps.size} apps",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = SoftPurple,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        onToggle = { viewModel.toggleAppBlocked(app) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: BlockedApp,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (app.isPermanentlyWhitelisted) SleepGreen.copy(alpha = 0.05f)
        else if (app.isBlocked) SleepRed.copy(alpha = 0.05f)
        else GlassWhite.copy(alpha = 0.3f),
        label = "appBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(enabled = !app.isPermanentlyWhitelisted) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DuskPurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = SoftPurple,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (app.isPermanentlyWhitelisted) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = SleepGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Always allowed",
                        style = MaterialTheme.typography.labelSmall,
                        color = SleepGreen
                    )
                } else {
                    Text(
                        text = app.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }

        if (!app.isPermanentlyWhitelisted) {
            Switch(
                checked = app.isBlocked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SleepRed,
                    checkedTrackColor = SleepRed.copy(alpha = 0.3f),
                    uncheckedThumbColor = SleepGreen,
                    uncheckedTrackColor = SleepGreen.copy(alpha = 0.3f)
                )
            )
        } else {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = "Protected",
                tint = SleepGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}
