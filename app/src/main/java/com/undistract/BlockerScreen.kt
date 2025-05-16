package com.undistract

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.undistract.ProfileManager
import com.undistract.R
import com.undistract.UndistractApp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.Dialog
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Divider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BlockerScreen(
    nfcHelper: NfcHelper,
    newIntentFlow: StateFlow<Intent?>,
    viewModel: BlockerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as Activity

    val isBlocking by viewModel.isBlocking.collectAsState(initial = false)
    val showWrongTagAlert by viewModel.showWrongTagAlert.collectAsState(initial = false)
    val showCreateTagAlert by viewModel.showCreateTagAlert.collectAsState(initial = false)
    val nfcWriteSuccess by viewModel.nfcWriteSuccess.collectAsState(initial = false)
    val nfcWriteDialogShown by viewModel.nfcWriteDialogShown.collectAsState(initial = false)
    val showScanTagAlert by viewModel.showScanTagAlert.collectAsState(initial = false)
    val showTagsList = remember { mutableStateOf(false) }
    val writtenTags by viewModel.writtenTags.collectAsState()


    // Configure NFC reading
    LaunchedEffect(Unit) {
        nfcHelper.startScan { payload ->
            viewModel.scanTag(payload)
        }
    }

    val backgroundColor = if (isBlocking) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    // Handle NFC scanning
    DisposableEffect(nfcHelper) {
        // Lifecycle observer to enable/disable NFC dispatch
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> nfcHelper.enableForegroundDispatch()
                Lifecycle.Event.ON_PAUSE -> nfcHelper.disableForegroundDispatch()
                else -> {}
            }
        }

        val lifecycle = (activity as LifecycleOwner).lifecycle
        lifecycle.addObserver(lifecycleObserver)

        // Intent listener for NFC
        val intent = activity.intent
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            nfcHelper.handleIntent(intent)
        }

        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    // Handle new intents
    LaunchedEffect(Unit) {
        newIntentFlow.collect { intent ->
            if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                nfcHelper.handleIntent(intent)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Undistract") },
                actions = {
                    IconButton(
                        onClick = { viewModel.showCreateTagAlert() },
                        enabled = nfcHelper.isNfcAvailable
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_add_24),
                            contentDescription = "Create Tag"
                        )
                    }

                    IconButton(
                        onClick = { showTagsList.value = true }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stack_hexagon_24),
                            contentDescription = "Show Tags"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Block/Unblock Button
                AnimatedContent(
                    targetState = isBlocking,
                    transitionSpec = {
                        slideInVertically { height -> height } + fadeIn() with
                        slideOutVertically { height -> -height } + fadeOut()
                    }
                ) { blocking ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (blocking) "Tap to unblock" else "Tap to block",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        IconButton(
                            onClick = {
                                if (nfcHelper.isNfcEnabled) {
                                    viewModel.showScanTagAlert()
                                } else {
                                    // Prompt to enable NFC
                                    val intent = Intent(android.provider.Settings.ACTION_NFC_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.size(120.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (blocking) R.drawable.baseline_block_24 else R.drawable.baseline_check_circle_24
                                ),
                                contentDescription = if (blocking) "Unblock" else "Block",
                                modifier = Modifier.size(100.dp),
                                tint = if (blocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Only show profile picker when not blocking
                AnimatedVisibility(
                    visible = !isBlocking,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    ProfilesPicker(profileManager = UndistractApp.profileManager)
                }
            }
        }
    }

    // Alerts
    if (showScanTagAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissScanTagAlert() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Scan Your Tag") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Please hold your Undistract NFC tag against the back of your device")
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissScanTagAlert() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWrongTagAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWrongTagAlert() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Not an Undistract Tag") },
            text = { Text("You can create a new Undistract tag using the + button") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissWrongTagAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showCreateTagAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateTagAlert() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Create Undistract Tag") },
            text = { Text("Do you want to create a new Undistract tag?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onCreateTagConfirmed()
                    nfcHelper.startWrite("UNDISTRACT-IS-GREAT") { success ->
                        if (success) {
                            viewModel.saveTag("UNDISTRACT-IS-GREAT") // Record successful write
                        }
                        viewModel.onTagWriteResult(success)
                    }
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideCreateTagAlert() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (nfcWriteSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNfcWriteSuccessAlert() },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Tag Creation") },
            text = { Text("Undistract tag created successfully!") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissNfcWriteSuccessAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showTagsList.value) {
        TagsList(
            tags = writtenTags,
            onClose = { showTagsList.value = false }
        )
    }
}


@Composable
fun TagsList(tags: List<NfcTag>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Your NFC Tags",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (tags.isEmpty()) {
                    Text("No tags have been written yet")
                } else {
                    LazyColumn {
                        items(tags) { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column {
                                    Text(tag.payload)
                                    Text(
                                        "Created: ${formatDate(tag.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Divider()
                        }
                    }
                }

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}


