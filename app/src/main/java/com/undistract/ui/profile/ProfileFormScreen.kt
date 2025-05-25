package com.undistract.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    viewModel: ProfileFormViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val profileName by viewModel.profileName.collectAsState()
    val profileIcon by viewModel.profileIcon.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val isEditing = viewModel.isEditing

    var showIconPicker by remember { mutableStateOf(false) }
    var showAppSelection by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "Edit Profile" else "Add Profile") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cancel")
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
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Profile Details Card
            item {
                FormCard(title = "Profile Details") {
                    Text("Profile Name", style = MaterialTheme.typography.bodySmall)
                    
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
                        val context = LocalContext.current
                        val iconResId = context.resources.getIdentifier(
                            profileIcon, "drawable", context.packageName
                        )
                        
                        Icon(
                            painter = painterResource(iconResId),
                            contentDescription = "Profile Icon",
                            modifier = Modifier.size(40.dp)
                        )
                        
                        Text("Choose Icon", modifier = Modifier.padding(start = 16.dp))
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }
            
            // App Configuration Card
            item {
                FormCard(title = "App Configuration") {
                    Button(
                        onClick = { showAppSelection = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configure Blocked Apps")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Blocked Apps:")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${selectedApps.size}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
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
            
            // Delete Button (only for editing mode)
            if (isEditing) {
                item {
                    FormCard {
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
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

    // Dialogs
    if (showIconPicker) {
        IconPickerDialog(
            onIconSelected = { 
                viewModel.updateProfileIcon(it)
                showIconPicker = false 
            },
            onDismiss = { showIconPicker = false }
        )
    }

    if (showAppSelection) {
        AppSelectionDialog(
            selectedApps = selectedApps,
            onAppsSelected = { 
                viewModel.updateSelectedApps(it)
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
                TextButton(onClick = {
                    viewModel.deleteProfile(onDismiss)
                    showDeleteConfirmation = false
                }) {
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

@Composable
private fun FormCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
            val context = LocalContext.current
            LazyColumn {
                items(icons.chunked(4)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { icon ->
                            IconButton(
                                onClick = { onIconSelected(icon) },
                                modifier = Modifier.size(56.dp)
                            ) {
                                val iconResId = context.resources.getIdentifier(
                                    icon, "drawable", context.packageName
                                )
                                Icon(
                                    painter = painterResource(iconResId),
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
