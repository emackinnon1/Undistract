package com.undistract

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.CenterAlignedTopAppBar
import com.undistract.data.repositories.ProfileRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    viewModel: ProfileRepository = viewModel(),
    onDismiss: () -> Unit
) {
    val profileName by viewModel.profileName.collectAsState()
    val profileIcon by viewModel.profileIcon.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()

    var showIconPicker by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isEditing = viewModel.isEditing

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "Edit Profile" else "Add Profile") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile(onDismiss) },
                        enabled = viewModel.isFormValid()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            "Profile Details",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Profile Name",
                            style = MaterialTheme.typography.bodySmall
                        )

                        TextField(
                            value = profileName,
                            onValueChange = viewModel::updateProfileName,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter profile name") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIconPicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = LocalContext.current.resources.getIdentifier(
                                        profileIcon, "drawable", LocalContext.current.packageName
                                    )
                                ),
                                contentDescription = "Profile Icon",
                                modifier = Modifier.size(40.dp)
                            )

                            Text(
                                "Choose Icon",
                                modifier = Modifier.padding(start = 16.dp)
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Text(
                            "App Configuration",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showAppSelection = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Configure Blocked Apps")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Blocked Apps:")
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "${selectedApps.size}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Undistract doesn't show the names of the selected apps for privacy reasons.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isEditing) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete Profile")
                        }
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            onIconSelected = { icon ->
                viewModel.updateProfileIcon(icon)
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }

    if (showAppSelection) {
        AppSelectionDialog(
            selectedApps = selectedApps,
            onAppsSelected = { apps: List<String> ->
                viewModel.updateSelectedApps(apps)
                showAppSelection = false
            },
            onDismiss = { showAppSelection = false }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete this profile?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(onDismiss)
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // This is a simple implementation - you'd want to add more icons
    val icons = listOf(
        "baseline_block_24",
        "baseline_do_not_disturb_24",
        "baseline_warning_24",
        "baseline_lock_24",
        "baseline_access_time_24",
        "baseline_work_24",
        "baseline_school_24",
        "baseline_home_24"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an Icon") },
        text = {
            LazyColumn {
                items(icons.chunked(4)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (icon in row) {
                            IconButton(
                                onClick = { onIconSelected(icon) },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(
                                        id = LocalContext.current.resources.getIdentifier(
                                            icon, "drawable", LocalContext.current.packageName
                                        )
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
