package com.example.desainmoviereview2

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var originalUsername: String? = null
    private var imageUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            imageUri?.let { uri ->
                binding.imageViewProfilePic.setImageURI(uri)
                binding.buttonSaveProfile.isEnabled = true
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                imageUri = uri
                binding.imageViewProfilePic.setImageURI(uri)
                binding.buttonSaveProfile.isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        loadUserProfile()

        binding.editProfileImageButton.setOnClickListener {
            showImageSourceDialog()
        }

        binding.editTextUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.buttonSaveProfile.isEnabled = s.toString() != originalUsername || imageUri != null
            }
        })

        binding.buttonSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireActivity(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Choose your profile picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        createImageFile()?.let {
                            val photoURI: Uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.example.desainmoviereview2.fileprovider",
                                it
                            )
                            imageUri = photoURI
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            cameraLauncher.launch(intent)
                        }
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryLauncher.launch(intent)
                    }
                    2 -> dialog.dismiss()
                }
            }.show()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            imageUri = Uri.fromFile(this)
        }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        val newUsername = binding.editTextUsername.text.toString().trim()
        if (newUsername.isEmpty()) {
            binding.editTextUsername.error = "Username cannot be empty"
            return
        }

        binding.editTextUsername.error = null
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSaveProfile.isEnabled = false

        val updates = mutableMapOf<String, Any>()
        if (newUsername != originalUsername) {
            updates["username"] = newUsername
        }

        if (imageUri != null) {
            val imageBase64 = encodeImageToBase64(imageUri!!)
            if (imageBase64 != null) {
                updates["avatarBase64"] = imageBase64
                updateUserInDatabase(userId, updates)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.buttonSaveProfile.isEnabled = true
                Toast.makeText(requireContext(), "Failed to encode image.", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (updates.isNotEmpty()) {
                updateUserInDatabase(userId, updates)
            } else {
                binding.progressBar.visibility = View.GONE
                binding.buttonSaveProfile.isEnabled = false
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
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

    private fun updateUserInDatabase(userId: String, updates: Map<String, Any>) {
        database.child("users").child(userId).updateChildren(updates).addOnSuccessListener {
            if (_binding == null) return@addOnSuccessListener

            if (updates.containsKey("username")) {
                originalUsername = updates["username"] as String
            }
            imageUri = null

            if (updates.isNotEmpty()) {
                updateUserContributions(userId, updates) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "Profile and all contributions updated successfully", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.buttonSaveProfile.isEnabled = false
                        loadUserProfile() // Reload to show new PFP
                    }
                }
            } else {
                // This case should not happen if we get here, but as a fallback:
                binding.progressBar.visibility = View.GONE
                binding.buttonSaveProfile.isEnabled = false
            }

        }.addOnFailureListener { e ->
            if (_binding == null) return@addOnFailureListener
            binding.progressBar.visibility = View.GONE
            binding.buttonSaveProfile.isEnabled = true
            Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.w("ProfileFragment", "Error updating document", e)
        }
    }

    private fun updateUserContributions(userId: String, updates: Map<String, Any>, onComplete: () -> Unit) {
        val forumPostsRef = database.child("forum_posts")
        val contributionUpdates = mutableMapOf<String, Any?>()

        forumPostsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (postSnapshot in snapshot.children) {
                    if (postSnapshot.child("author_uid").value == userId) {
                        if (updates.containsKey("username")) {
                            contributionUpdates["${postSnapshot.key}/author_username"] = updates["username"]
                        }
                        if (updates.containsKey("avatarBase64")) {
                            contributionUpdates["${postSnapshot.key}/author_avatar_base64"] = updates["avatarBase64"]
                        }
                    }

                    for (replySnapshot in postSnapshot.child("replies").children) {
                        if (replySnapshot.child("author_uid").value == userId) {
                            if (updates.containsKey("username")) {
                                contributionUpdates["${postSnapshot.key}/replies/${replySnapshot.key}/author_username"] = updates["username"]
                            }
                            if (updates.containsKey("avatarBase64")) {
                                contributionUpdates["${postSnapshot.key}/replies/${replySnapshot.key}/author_avatar_base64"] = updates["avatarBase64"]
                            }
                        }
                    }
                }

                if (contributionUpdates.isNotEmpty()) {
                    forumPostsRef.updateChildren(contributionUpdates).addOnCompleteListener {
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonSaveProfile.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to update contributions: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }


    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        binding.textViewEmail.text = auth.currentUser?.email

        database.child("users").child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (_binding == null || !dataSnapshot.exists()) return@addOnSuccessListener

            val userProfile = dataSnapshot.getValue(User::class.java)
            userProfile?.let {
                originalUsername = it.username
                binding.editTextUsername.setText(originalUsername)

                if (it.avatarBase64.isNotBlank()) {
                    try {
                        val imageBytes = Base64.decode(it.avatarBase64, Base64.DEFAULT)
                        Glide.with(this)
                            .load(imageBytes)
                            .circleCrop()
                            .into(binding.imageViewProfilePic)
                    } catch (e: IllegalArgumentException) {
                        Log.e("ProfileFragment", "Failed to decode Base64 string", e)
                        binding.imageViewProfilePic.setImageResource(R.drawable.ic_anonymous)
                    }
                } else {
                    binding.imageViewProfilePic.setImageResource(R.drawable.ic_anonymous)
                }
            }
            binding.buttonSaveProfile.isEnabled = false
        }.addOnFailureListener{
            Log.e("ProfileFragment", "Error getting data", it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}