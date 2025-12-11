package com.example.desainmoviereview2.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.desainmoviereview2.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Image Picking Logic
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            scope.launch {
                val base64 = encodeImageToBase64(context, tempImageUri!!)
                if (base64 != null) {
                    viewModel.onImageSelected(base64)
                } else {
                     snackbarHostState.showSnackbar("Failed to process image")
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
             scope.launch {
                val base64 = encodeImageToBase64(context, uri)
                if (base64 != null) {
                    viewModel.onImageSelected(base64)
                } else {
                     snackbarHostState.showSnackbar("Failed to process image")
                }
            }
        }
    }
    
    var showImageSourceDialog by remember { mutableStateOf(false) }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Choose your profile picture") },
            text = {
                Column {
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        val photoFile = createImageFile(context)
                        if (photoFile != null) {
                             val photoURI = FileProvider.getUriForFile(
                                context,
                                "com.example.desainmoviereview2.fileprovider",
                                photoFile
                            )
                            tempImageUri = photoURI
                            cameraLauncher.launch(photoURI)
                        }
                    }) {
                        Text("Take Photo")
                    }
                    TextButton(onClick = {
                        showImageSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {
                 TextButton(onClick = { showImageSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle side effects (Snackbars)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Gradient Header Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1), // Indigo
                                Color(0xFF8B5CF6)  // Purple
                            )
                        )
                    )
            )

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                
                // Profile Image with Shadow
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Card(
                        modifier = Modifier.size(140.dp),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        ProfileImage(
                            base64String = uiState.pendingAvatarBase64 ?: uiState.user.avatarBase64,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Gray)
                        )
                    }
                    
                    FloatingActionButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier.size(44.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile Picture")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Username Display
                Text(
                    text = uiState.user.username.ifEmpty { "User" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = uiState.user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Movies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reviews",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Posts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // User Info Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Account Settings",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Email (Read-only)
                        OutlinedTextField(
                            value = uiState.user.email,
                            onValueChange = {},
                            label = { Text("Email") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Filled.Email, contentDescription = null)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Username (Editable)
                        OutlinedTextField(
                            value = uiState.user.username,
                            onValueChange = { viewModel.onUsernameChange(it) },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = uiState.user.username.isEmpty(),
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            }
                        )
                        if (uiState.user.username.isEmpty()) {
                             Text(
                                text = "Username cannot be empty",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveUserProfile() },
                    enabled = uiState.isSaveEnabled && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Logout Button
                OutlinedButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ProfileImage(
    base64String: String,
    modifier: Modifier = Modifier
) {
    if (base64String.isNotEmpty()) {
        val imageBytes = try {
             Base64.decode(base64String, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            null
        }

        if (imageBytes != null) {
            GlideImage(
                model = imageBytes,
                contentDescription = "Profile Picture",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        } else {
             Image(
                painter = painterResource(id = R.drawable.ic_anonymous),
                contentDescription = "Profile Picture",
                modifier = modifier,
                contentScale = ContentScale.Crop
            )
        }
    } else {
         Image(
            painter = painterResource(id = R.drawable.ic_anonymous),
            contentDescription = "Profile Picture",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

// Helper functions
private fun createImageFile(context: Context): File? {
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return try {
        File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

private suspend fun encodeImageToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}
