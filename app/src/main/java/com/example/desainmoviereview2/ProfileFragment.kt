package com.example.desainmoviereview2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var originalUsername: String? = null

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
        // Point the database reference to the "users" node in the correct database instance
        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users")

        loadUserProfile()

        binding.editTextUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.buttonSaveProfile.isEnabled = s.toString() != originalUsername && binding.progressBar.visibility == View.GONE
            }
        })

        binding.buttonSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        binding.buttonLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireActivity(), AuthActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadUserProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e("ProfileFragment", "User is not logged in.")
            return
        }

        val userId = firebaseUser.uid
        binding.textViewEmail.text = firebaseUser.email

        database.child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // Deserialize the entire User object
                val userProfile = dataSnapshot.getValue(User::class.java)
                
                userProfile?.let {
                    originalUsername = it.username
                    binding.editTextUsername.setText(originalUsername)
                    
                    // Load profile picture using Glide
                    it.profilePictureUrl?.let { url ->
                        if (url.isNotEmpty()) {
                            Glide.with(this@ProfileFragment)
                                .load(url)
                                .placeholder(R.drawable.ic_profile) // Optional placeholder
                                .into(binding.imageViewProfilePic)
                        }
                    }
                }
                binding.buttonSaveProfile.isEnabled = false // Disable button initially
            } else {
                Log.d("ProfileFragment", "No profile data found in Realtime DB for user $userId")
            }
        }.addOnFailureListener{
            Log.e("ProfileFragment", "Error getting data", it)
        }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val username = binding.editTextUsername.text.toString().trim()
        if (username.isEmpty()) {
            binding.textFieldUsername.error = "Username cannot be empty"
            return
        }

        binding.textFieldUsername.error = null

        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSaveProfile.isEnabled = false

        // Create a map to update the specific field
        val userUpdates = mapOf<String, Any>(
            "username" to username
        )

        database.child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                originalUsername = username
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.buttonSaveProfile.isEnabled = true
                Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.w("ProfileFragment", "Error updating document", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
