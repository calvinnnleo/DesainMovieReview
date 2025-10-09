package com.example.desainmoviereview2

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    private fun loadUserProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            Log.e("ProfileFragment", "User is not logged in.")
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            return
        }

        val userId = firebaseUser.uid
        binding.textViewEmail.text = firebaseUser.email

        database.child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (_binding == null) return@addOnSuccessListener // View is destroyed
            if (dataSnapshot.exists()) {
                val userProfile = dataSnapshot.getValue(User::class.java)

                userProfile?.let {
                    originalUsername = it.username
                    binding.editTextUsername.setText(originalUsername)
                }
                binding.imageViewProfilePic.setImageResource(R.drawable.ic_profile)
                binding.buttonSaveProfile.isEnabled = false
            } else {
                Log.d("ProfileFragment", "No profile data found for user $userId")
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

        val newUsername = binding.editTextUsername.text.toString().trim()
        if (newUsername.isEmpty()) {
            binding.editTextUsername.error = "Username cannot be empty"
            return
        }

        binding.editTextUsername.error = null
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSaveProfile.isEnabled = false

        // First, read the existing user data to get the complete object.
        database.child(userId).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val currentUser = dataSnapshot.getValue(User::class.java)
                if (currentUser != null) {
                    // Create a new user object with the updated username.
                    val updatedUser = currentUser.copy(username = newUsername)

                    // Write the entire updated object back to the database.
                    database.child(userId).setValue(updatedUser)
                        .addOnSuccessListener {
                            if (_binding == null) return@addOnSuccessListener
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            originalUsername = newUsername
                            binding.buttonSaveProfile.isEnabled = false
                        }
                        .addOnFailureListener { e ->
                            if (_binding == null) return@addOnFailureListener
                            binding.progressBar.visibility = View.GONE
                            binding.buttonSaveProfile.isEnabled = true
                            Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.w("ProfileFragment", "Error updating document", e)
                        }
                }
            } else {
                 if (_binding == null) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                binding.buttonSaveProfile.isEnabled = true
                Toast.makeText(requireContext(), "Could not find user data to update.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
             if (_binding == null) return@addOnFailureListener
            binding.progressBar.visibility = View.GONE
            binding.buttonSaveProfile.isEnabled = true
            Toast.makeText(requireContext(), "Failed to read user data: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
