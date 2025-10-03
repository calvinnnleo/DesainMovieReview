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
        database = FirebaseDatabase.getInstance().getReference("users")

        loadUserProfile()

        binding.editTextUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.buttonSaveProfile.isEnabled = s.toString() != originalUsername
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
        val user = auth.currentUser
        if (user == null) {
            Log.e("ProfileFragment", "User is not logged in.")
            // Optionally, navigate to the login screen
            return
        }

        val userId = user.uid
        binding.textViewEmail.text = user.email

        database.child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                originalUsername = it.child("username").getValue(String::class.java)
                binding.editTextUsername.setText(originalUsername)
                binding.buttonSaveProfile.isEnabled = false // Disable button initially
            } else {
                Log.d("ProfileFragment", "No profile data found in Realtime DB")
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

        // Clear any previous errors
        binding.textFieldUsername.error = null

        val userUpdates = mapOf<String, Any>(
            "username" to username
        )

        database.child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                originalUsername = username // Update the original username
                binding.buttonSaveProfile.isEnabled = false // Disable after saving
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.w("ProfileFragment", "Error updating document", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
