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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.google.firebase.auth.FirebaseAuth
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
    val userEmail = remember { FirebaseAuth.getInstance().currentUser?.email ?: "" }
    
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("User Profile") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
             if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Profile Image
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    ProfileImage(
                        base64String = uiState.pendingAvatarBase64 ?: uiState.user.avatarBase64,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                    
                    SmallFloatingActionButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier.size(40.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile Picture")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Email (Read-only)
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    label = { Text("Email") },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Username (Editable)
                OutlinedTextField(
                    value = uiState.user.username,
                    onValueChange = { viewModel.onUsernameChange(it) },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.user.username.isEmpty()
                )
                if (uiState.user.username.isEmpty()) {
                     Text(
                        text = "Username cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                Button(
                    onClick = { viewModel.saveUserProfile() },
                    enabled = uiState.isSaveEnabled && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Changes")
                }

                Spacer(modifier = Modifier.height(16.dp))

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
