package com.sleepcontroller.ui.screens.tasks

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sleepcontroller.data.db.entity.MorningTask
import com.sleepcontroller.data.db.entity.TaskType
import com.sleepcontroller.ui.components.GlassCard
import com.sleepcontroller.ui.theme.*
import java.io.File

/**
 * Configuration-mode task card. Shows task details, toggle switch,
 * edit click, and delete button.
 */
@Composable
fun TaskCard(
    task: MorningTask,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val taskIcon = when (task.type) {
        TaskType.CHECKBOX -> "✅"
        TaskType.TIMED -> "⏱️"
        TaskType.PHOTO -> "📸"
    }

    val taskTypeLabel = when (task.type) {
        TaskType.CHECKBOX -> "Check off"
        TaskType.TIMED -> "${task.timedDurationSeconds / 60} min"
        TaskType.PHOTO -> "Photo proof"
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(taskIcon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (task.isEnabled) TextPrimary else TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SoftPurple.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = taskTypeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftPurple,
                            fontSize = 10.sp
                        )
                    }
                    if (task.type == TaskType.PHOTO) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SunriseYellow.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EXIF verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = SunriseYellow,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Toggle
            Switch(
                checked = task.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SoftPurple,
                    checkedTrackColor = SoftPurple.copy(alpha = 0.3f),
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = SurfaceBorder
                )
            )

            // Delete
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Delete",
                    tint = SleepRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Camera capture button with permission handling.
 * Uses ActivityResult API to launch camera and return the photo URI.
 */
@Composable
fun PhotoCaptureButton(onPhotoSubmit: (Uri) -> Unit) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onPhotoSubmit(photoUri!!)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoDir = File(context.filesDir, "task_photos").apply { mkdirs() }
            val file = File(photoDir, "morning_task_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Button(
        onClick = {
            val hasCameraPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCameraPermission) {
                val photoDir = File(context.filesDir, "task_photos").apply { mkdirs() }
                val file = File(photoDir, "morning_task_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                photoUri = uri
                cameraLauncher.launch(uri)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        modifier = Modifier.height(36.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SunriseYellow),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            Icons.Rounded.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = NightBlack
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Photo", style = MaterialTheme.typography.labelMedium, color = NightBlack)
    }
}

/**
 * Add/Edit task dialog with task type selection and timed duration picker.
 */
@Composable
fun AddTaskDialog(
    editingTask: MorningTask?,
    onDismiss: () -> Unit,
    onSave: (String, TaskType, Int) -> Unit
) {
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var selectedType by remember { mutableStateOf(editingTask?.type ?: TaskType.CHECKBOX) }
    var timedMinutes by remember { mutableStateOf((editingTask?.timedDurationSeconds ?: 300) / 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title, selectedType, timedMinutes * 60)
                    }
                }
            ) {
                Text(
                    if (editingTask != null) "Update" else "Add",
                    color = SoftPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextTertiary)
            }
        },
        title = {
            Text(
                if (editingTask != null) "Edit Task" else "Add Morning Task",
                color = TextPrimary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SoftPurple,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = SoftPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Task Type", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                TaskType.entries.forEach { type ->
                    val icon = when (type) {
                        TaskType.CHECKBOX -> "✅"
                        TaskType.TIMED -> "⏱️"
                        TaskType.PHOTO -> "📸"
                    }
                    val label = when (type) {
                        TaskType.CHECKBOX -> "Simple check-off"
                        TaskType.TIMED -> "Timed activity"
                        TaskType.PHOTO -> "Photo with EXIF proof"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedType = type }
                            .background(
                                if (selectedType == type) SoftPurple.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SoftPurple,
                                unselectedColor = TextTertiary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(icon, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }

                // Timed duration picker
                if (selectedType == TaskType.TIMED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Duration: $timedMinutes minutes",
                        style = MaterialTheme.typography.labelLarge,
                        color = SunriseYellow
                    )
                    Slider(
                        value = timedMinutes.toFloat(),
                        onValueChange = { timedMinutes = it.toInt() },
                        valueRange = 1f..30f,
                        colors = SliderDefaults.colors(
                            thumbColor = SunriseYellow,
                            activeTrackColor = SunriseYellow,
                            inactiveTrackColor = SurfaceBorder
                        )
                    )
                }
            }
        },
        containerColor = DuskPurple,
        shape = RoundedCornerShape(24.dp)
    )
}
