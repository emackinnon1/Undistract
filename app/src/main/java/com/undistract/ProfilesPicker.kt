package com.undistract

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.collections.addAll
import kotlin.text.clear


@Composable
fun ProfilesPicker(
    profileManager: ProfileManager,
    modifier: Modifier = Modifier
) {
    val profiles by profileManager.profiles.collectAsState()
    val currentProfileId by profileManager.currentProfileId.collectAsState()

    var showAddProfileView by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background) // ProfileSectionBackground color
    ) {
        Text(
            text = "Profiles",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 90.dp),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(profiles) { profile ->
                ProfileCell(
                    profile = profile,
                    isSelected = profile.id == currentProfileId,
                    onClick = { profileManager.setCurrentProfile(profile.id) },
                    onLongClick = { editingProfile = profile }
                )
            }

            item {
                ProfileCellBase(
                    name = "New...",
                    iconResId = R.drawable.baseline_add_24,
                    appsBlocked = null,
                    isSelected = false,
                    isDashed = true,
                    hasDivider = false,
                    onClick = { showAddProfileView = true }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Long press on a profile to edit...",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
    }

    // Add Profile Sheet
    if (showAddProfileView) {
        ProfileFormDialog(
            onDismiss = { showAddProfileView = false },
            onSave = { name, icon, apps ->
                val newProfile = Profile(
                    name = name,
                    appPackageNames = apps,
                    icon = icon
                )
                profileManager.addProfile(newProfile)
                showAddProfileView = false
            }
        )
    }

    // Edit Profile Sheet
    editingProfile?.let { profile ->
        ProfileFormDialog(
            profile = profile,
            onDismiss = { editingProfile = null },
            onSave = { name, icon, apps ->
                profileManager.updateProfile(
                    id = profile.id,
                    name = name,
                    appPackageNames = apps,
                    icon = icon
                )
                editingProfile = null
            },
            onDelete = { profileId ->
                profileManager.deleteProfile(profileId)
            }
        )
    }
}

@Composable
fun ProfileCell(
    profile: Profile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val iconResId = try {
        val resourceField = R.drawable::class.java.getDeclaredField(profile.icon)
        resourceField.getInt(null)
    } catch (e: Exception) {
        R.drawable.baseline_block_24 // Fallback icon
    }

    ProfileCellBase(
        name = profile.name,
        iconResId = iconResId,
        appsBlocked = profile.appPackageNames.size,
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@Composable
fun ProfileCellBase(
    name: String,
    iconResId: Int,
    appsBlocked: Int? = null,
    isSelected: Boolean = false,
    isDashed: Boolean = false,
    hasDivider: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (isDashed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 2.dp,
            color = borderColor
        ),
        color = backgroundColor,
        modifier = Modifier
            .size(90.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )

            if (hasDivider) {
                HorizontalDivider(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth(0.7f)
                )
            }

            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Lighter text
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (appsBlocked != null) {
                Text(
                    text = "Apps: $appsBlocked",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ProfileFormDialog(
    profile: Profile? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, apps: List<String>) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var profileName by remember { mutableStateOf(profile?.name ?: "") }
    var profileIcon by remember { mutableStateOf(profile?.icon ?: "baseline_block_24") }
    var selectedApps by remember { mutableStateOf(profile?.appPackageNames ?: emptyList<String>()) }

    var showIconPicker by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }

    val isEditMode = profile != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(
                        id = if (isEditMode) R.string.edit_profile else R.string.create_profile
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Profile name input
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Icon selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.icon),
                        modifier = Modifier.weight(1f)
                    )

                    // Show current icon
                    val iconResId = try {
                        val resourceField = R.drawable::class.java.getDeclaredField(profileIcon)
                        resourceField.getInt(null)
                    } catch (e: Exception) {
                        R.drawable.baseline_block_24 // Fallback
                    }

                    IconButton(onClick = { showIconPicker = true }) {
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // App selection button
                Button(
                    onClick = { showAppSelection = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_apps_24),
                            contentDescription = null
                        )
                        Text(stringResource(R.string.select_apps_to_block, selectedApps.size))
                    }
                }

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditMode && profile?.isDefault == false) {
                        TextButton(
                            onClick = {
                                profile?.let {
                                    onDelete?.invoke(it.id)  // Call onDelete with profile ID
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onSave(profileName, profileIcon, selectedApps) },
                        enabled = profileName.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }

    // Icon Picker Dialog
    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = profileIcon,
            onIconSelected = {
                profileIcon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }

    // App Selection Dialog
    if (showAppSelection) {
        AppSelectionDialog(
            selectedApps = selectedApps,
            onAppsSelected = {
                selectedApps = it
                showAppSelection = false
            },
            onDismiss = { showAppSelection = false }
        )
    }
}

@Composable
fun IconPickerDialog(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = listOf(
        "baseline_block_24",
        "baseline_check_circle_24",
        "baseline_access_time_24",
        "baseline_favorite_24",
        "baseline_home_24",
        "baseline_person_24",
        "baseline_settings_24",
        "baseline_star_24",
        "baseline_work_24"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_an_icon)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(icons) { iconName ->
                    val iconResId = try {
                        val resourceField = R.drawable::class.java.getDeclaredField(iconName)
                        resourceField.getInt(null)
                    } catch (e: Exception) {
                        R.drawable.baseline_block_24
                    }

                    val isSelected = currentIcon == iconName

                    Surface(
                        shape = CircleShape,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surface,
                        border = if (isSelected)
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable { onIconSelected(iconName) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = iconResId),
                                contentDescription = null,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun AppSelectionDialog(
    selectedApps: List<String>,
    onAppsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val profileManager = UndistractApp.profileManager

    val selectedAppsMutable = remember {
        mutableStateListOf<String>().apply { addAll(selectedApps) }
    }

    val installedApps = remember {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        packageManager.queryIntentActivities(intent, 0)
            .filter { resolveInfo ->
                // Filter out Undistract app
                resolveInfo.activityInfo.applicationInfo.packageName != "com.undistract"
            }
            .map { resolveInfo ->
                val appInfo = resolveInfo.activityInfo.applicationInfo
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    appIcon = appInfo.loadIcon(packageManager)
                )
            }
            .sortedBy { it.appName }
    }

    // Track if all apps are selected
    val allAppsSelected = selectedAppsMutable.size == installedApps.size

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_apps_to_block, selectedAppsMutable.size)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Select All option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (allAppsSelected) {
                                    selectedAppsMutable.clear()
                                } else {
                                    selectedAppsMutable.clear()
                                    selectedAppsMutable.addAll(installedApps.map { it.packageName })
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select All Apps",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        Checkbox(
                            checked = allAppsSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedAppsMutable.clear()
                                    selectedAppsMutable.addAll(installedApps.map { it.packageName })
                                } else {
                                    selectedAppsMutable.clear()
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // App list
                items(installedApps) { appInfo ->
                    val isSelected = selectedAppsMutable.contains(appInfo.packageName)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) {
                                    selectedAppsMutable.remove(appInfo.packageName)
                                } else {
                                    selectedAppsMutable.add(appInfo.packageName)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = appInfo.appIcon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )

                        Text(
                            text = appInfo.appName,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )

                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    selectedAppsMutable.add(appInfo.packageName)
                                } else {
                                    selectedAppsMutable.remove(appInfo.packageName)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAppsSelected(selectedAppsMutable.toList()) }) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable
)

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) return this.bitmap

    val bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(
            this.intrinsicWidth,
            this.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }

    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}
