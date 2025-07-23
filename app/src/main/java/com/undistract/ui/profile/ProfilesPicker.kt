package com.undistract.ui.profile

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.undistract.R
import com.undistract.data.entities.ProfileEntity
import com.undistract.data.models.Profile
import com.undistract.data.models.AppInfo
import com.undistract.managers.ProfileManager

/**
 * A Composable that displays and manages user profiles for app blocking.
 *
 * This component shows a grid of profiles that the user can select, create, edit, or delete.
 * It displays the currently selected profile and provides functionality to:
 * - Select a profile to be used for app blocking
 * - Create new profiles
 * - Edit existing profiles
 * - Delete profiles
 *
 * @param profileManager The manager that handles profile data and operations
 * @param modifier Optional modifier for customizing the component's layout and appearance
 */
@Composable
fun ProfilesPicker(
    profileManager: ProfileManager,
    modifier: Modifier = Modifier
) {
    val profiles by profileManager.profiles.collectAsState()
    val currentProfileId by profileManager.currentProfileId.collectAsState()

    var showAddProfileView by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ProfileEntity?>(null) }

    val isLoading by profileManager.isLoading.collectAsState()
    val errorMessage by profileManager.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            profileManager.clearErrorMessage()
        }
    }


    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
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
                modifier = Modifier.weight(3f).fillMaxHeight()
            ) {
                items(profiles) { profile ->
                    ProfileCell(
                        profile = profile,
                        isSelected = profile.id == currentProfileId,
                        onClick = { profileManager.setCurrentProfile(profile.id) },
                        onLongClick = { editingProfile = profile }
                    )
                }

            }

            OutlinedButton(
                onClick = { showAddProfileView = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_add_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Profile",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Text(
                text = "Long press on a profile to edit or delete it.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp) // Adds bottom spacing between the helper text and the container's edge to improve visual separation and readability
            )

            // Add this right after the Text explaining "Long press on a profile..."
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) { /* Consume all touch events */ },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("loadingIndicator")
                )
            }
        }
    }

    // Add Profile Sheet
    if (showAddProfileView) {
        ProfileFormDialog(
            onDismiss = { showAddProfileView = false },
            onSave = { name, icon, apps ->
                val newProfile = ProfileEntity(
                    id = java.util.UUID.randomUUID().toString(),
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

/**
 * A Composable that displays a single profile as a selectable cell.
 *
 * This component renders a profile with its name, icon, and the number of apps blocked.
 * It supports both tap (for selection) and long press (for editing) interactions.
 *
 * @param profile The profile data to display
 * @param isSelected Whether this profile is currently selected
 * @param onClick Callback invoked when the profile is tapped
 * @param onLongClick Callback invoked when the profile is long pressed
 */
@Composable
fun ProfileCell(
    profile: ProfileEntity,
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

/**
 * Base Composable for profile cells with customizable appearance and behavior.
 *
 * This component provides the visual structure for profile cells with options to customize
 * its appearance and behavior. It's used by ProfileCell and can be used directly for
 * special cases like the "Add New Profile" button.
 *
 * @param name The name to display in the cell
 * @param iconResId Resource ID for the icon to display
 * @param appsBlocked Optional number of apps blocked by this profile (null if not applicable)
 * @param isSelected Whether this cell is currently selected
 * @param isDashed Whether to use a dashed border style
 * @param hasDivider Whether to show a divider between the icon and text
 * @param onClick Callback invoked when the cell is tapped
 * @param onLongClick Callback invoked when the cell is long pressed
 */
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

/**
 * A dialog for creating or editing a profile.
 *
 * This Composable displays a form dialog that allows users to:
 * - Enter a profile name
 * - Select an icon
 * - Choose apps to block
 * - Delete the profile (if in edit mode)
 *
 * @param profile Optional existing profile for editing (null for create mode)
 * @param onDismiss Callback invoked when the dialog is dismissed
 * @param onSave Callback invoked when the profile is saved with the name, icon, and app list
 * @param onDelete Optional callback for deleting a profile, only used in edit mode
 */
@Composable
fun ProfileFormDialog(
    profile: ProfileEntity? = null,
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
                    if (isEditMode) {
                        TextButton(
                            onClick = {
                                profile.let {
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

/**
 * A dialog for selecting an icon for a profile.
 *
 * This Composable displays a grid of available icons that the user can select from.
 * The currently selected icon is highlighted.
 *
 * @param currentIcon The currently selected icon identifier
 * @param onIconSelected Callback invoked when an icon is selected
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
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

/**
 * A dialog for selecting apps to be blocked by a profile.
 *
 * This Composable displays a list of installed apps that the user can select for blocking.
 * It also provides a "Select All" option for convenience.
 *
 * @param selectedApps List of currently selected app package names
 * @param onAppsSelected Callback invoked when app selection is confirmed
 * @param onDismiss Callback invoked when the dialog is dismissed
 */
@Composable
fun AppSelectionDialog(
    selectedApps: List<String>,
    onAppsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

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

/**
 * Extension function to convert a Drawable to a Bitmap.
 *
 * This utility function handles conversion of any Drawable type to a Bitmap format,
 * which is needed for displaying app icons in the Image composable.
 * It properly handles BitmapDrawables and creates appropriate sized bitmaps for other Drawable types.
 *
 * @return A Bitmap representation of the Drawable
 */
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