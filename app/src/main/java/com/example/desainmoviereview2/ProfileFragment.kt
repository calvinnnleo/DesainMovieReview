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

import com.example.desainmoviereview2.User

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private var originalUsername: String? = null // Renamed from originalNickname

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
                val userProfile = dataSnapshot.getValue(User::class.java)

                userProfile?.let {
                    originalUsername = it.username // Changed from nickname
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

        val username = binding.editTextUsername.text.toString().trim() // Changed from nickname
        if (username.isEmpty()) {
            binding.editTextUsername.error = "Username cannot be empty" // Changed from nickname
            return
        }

        binding.editTextUsername.error = null

        binding.progressBar.visibility = View.VISIBLE
        binding.buttonSaveProfile.isEnabled = false

        val userUpdates = mapOf<String, Any>(
            "username" to username // Changed from nickname
        )

        database.child(userId).updateChildren(userUpdates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                originalUsername = username // Changed from nickname
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
