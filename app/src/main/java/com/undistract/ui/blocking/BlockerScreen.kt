package com.undistract.ui.blocking

import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.undistract.R
import com.undistract.UndistractApp
import com.undistract.data.models.NfcTag
import com.undistract.nfc.NfcHelper
import com.undistract.ui.profile.ProfilesPicker
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable


@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BlockerScreen(
    nfcHelper: NfcHelper,
    newIntentFlow: StateFlow<Intent?>,
    viewModel: BlockerViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect states
    val isBlocking by viewModel.isBlocking.collectAsState(initial = false)
    val isWritingTag by viewModel.isWritingTag.collectAsState(initial = false)
    val showWrongTagAlert by viewModel.showWrongTagAlert.collectAsState(initial = false)
    val showCreateTagAlert by viewModel.showCreateTagAlert.collectAsState(initial = false)
    val nfcWriteSuccess by viewModel.nfcWriteSuccess.collectAsState(initial = false)
    val showScanTagAlert by viewModel.showScanTagAlert.collectAsState(initial = false)
    val writtenTags by viewModel.writtenTags.collectAsState()
    val showTagsList = remember { mutableStateOf(false) }

    val profileManager = UndistractApp.profileManager
    val errorMessage by profileManager.errorMessage.collectAsState()

    // Control NFC scanning and writing based on dialog visibility
    LaunchedEffect(showScanTagAlert, isWritingTag) {
        if (showScanTagAlert || isWritingTag) {
            if (showScanTagAlert) {
                nfcHelper.startScan { payload -> viewModel.scanTag(payload) }
            }
            nfcHelper.enableForegroundDispatch()
        } else {
            nfcHelper.disableForegroundDispatch()
        }
    }

    // Handle new intents for both scanning and writing
    LaunchedEffect(Unit) {
        newIntentFlow.collect { intent ->
            if (intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                intent?.action == NfcAdapter.ACTION_TAG_DISCOVERED) {

                if (showScanTagAlert) {
                    nfcHelper.handleIntent(intent)
                } else if (isWritingTag) {
                    nfcHelper.handleIntent(intent)
                }
            }
        }
    }

    // Clean up NFC when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            nfcHelper.disableForegroundDispatch()
        }
    }

    // Error dialog
    errorMessage?.let {
        AlertDialogWithGlow(
            title = "Error",
            text = it,
            onDismiss = { profileManager.clearErrorMessage() },
            confirmButtonText = "OK"
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

                    IconButton(onClick = { showTagsList.value = true }) {
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
                .background(if (isBlocking)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
                )
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
                                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                }
                            },
                            modifier = Modifier.size(120.dp)
                        ) {
                            PulsingGlowEffect {
                                Icon(
                                    painter = painterResource(id = R.drawable.undistract_plain),
                                    contentDescription = if (blocking) "Unblock Apps" else "Block Apps",
                                    modifier = Modifier.size(100.dp),
                                    tint = if (blocking) Color.Red else MaterialTheme.colorScheme.tertiary
                                )
                            }
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

    // Alert dialogs
    if (showScanTagAlert) {
        AlertDialogWithProgress(
            title = if (isBlocking) "Scan to Unblock" else "Scan to Block",
            text = if (isBlocking)
                "Please hold your Undistract NFC tag against your device to unblock apps"
            else
                "Please hold your Undistract NFC tag against your device to block distractions",
            onDismiss = { viewModel.dismissScanTagAlert() }
        )
    }

    if (showWrongTagAlert) {
        AlertDialogWithMessage(
            title = "Not an Undistract Tag",
            text = "You can create a new Undistract tag using the + button",
            onConfirm = { viewModel.dismissWrongTagAlert() }
        )
    }

    if (showCreateTagAlert) {
        AlertDialogWithConfirmation(
            title = "Create Undistract Tag",
            text = "Do you want to create a new Undistract tag?",
            onConfirm = {
                viewModel.onCreateTagConfirmed()
                viewModel.setWritingTag(true)
                val uniquePayload = viewModel.generateUniqueTagPayload()
                nfcHelper.startWrite(uniquePayload) { success ->
                    viewModel.setWritingTag(false)
                    if (success) {
                        viewModel.saveTag(uniquePayload)
                    }
                    viewModel.onTagWriteResult(success)
                    viewModel.setWritingTag(false)
                }
            },
            onDismiss = {
                viewModel.hideCreateTagAlert()
                viewModel.setWritingTag(false)
            }
        )
    }

    if (isWritingTag) {
        AlertDialogWithProgress(
            title = "Writing NFC Tag",
            text = "Please hold your NFC tag against the back of your device.",
            onDismiss = {
                viewModel.setWritingTag(false)
            }
        )
    }

    if (nfcWriteSuccess) {
        AlertDialogWithMessage(
            title = "Tag Creation",
            text = "Undistract tag created successfully!",
            onConfirm = { viewModel.dismissNfcWriteSuccessAlert() }
        )
    }

    if (showTagsList.value) {
        TagsList(
            tags = writtenTags,
            onClose = { showTagsList.value = false },
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagsList(
        tags: List<NfcTag>,
        onClose: () -> Unit,
        viewModel: BlockerViewModel
) {
    var tagToDelete by remember { mutableStateOf<NfcTag?>(null) }

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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { tagToDelete = tag }
                                    )
                            ) {
                                Text(tag.payload)
                                Text(
                                    "Created: ${formatDate(tag.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Divider()
                            }
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

    // Confirmation dialog for deletion
    tagToDelete?.let { tag ->
        AlertDialogWithConfirmation(
            title = "Delete Tag",
            text = "Are you sure you want to delete this tag? This action cannot be undone.",
            onConfirm = {
                viewModel.deleteTag(tag)
                tagToDelete = null
            },
            onDismiss = {
                tagToDelete = null
            }
        )
    }
}

@Composable
fun PulsingGlowEffect(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 24.dp,
                    spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = glowAlpha),
                    shape = CircleShape
                )
        )
        content()
    }
}

@Composable
fun GlowingBorder(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .drawBehind {
                val strokeWidth = with(density) { 2.dp.toPx() }
                val cornerRadius = with(density) { 8.dp.toPx() }
                val borderColor = Color(0xFFA346FF)

                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = strokeWidth),
                    cornerRadius = CornerRadius(cornerRadius),
                    alpha = 0.7f
                )
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = strokeWidth * 2),
                    cornerRadius = CornerRadius(cornerRadius),
                    alpha = 0.4f
                )
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = strokeWidth * 3),
                    cornerRadius = CornerRadius(cornerRadius),
                    alpha = 0.2f
                )
            }
    ) {
        content()
    }
}

// Reusable Alert Dialog Components
@Composable
fun AlertDialogWithGlow(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    confirmButtonText: String = "OK"
) {
    Dialog(onDismissRequest = onDismiss) {
        GlowingBorder {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(confirmButtonText, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertDialogWithProgress(
    title: String,
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text)
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AlertDialogWithMessage(
    title: String,
    text: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        }
    )
}

@Composable
fun AlertDialogWithConfirmation(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
