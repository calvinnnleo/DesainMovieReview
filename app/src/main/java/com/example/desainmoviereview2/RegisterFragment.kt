package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.desainmoviereview2.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun registerUser() {
        val fullName = binding.editTextFullName.text.toString().trim()
        val username = binding.editTextUsername.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showErrorSnackbar("Please fill all fields")
            return
        }

        // Show loading state
        binding.buttonRegister.isEnabled = false
        binding.buttonRegister.text = "Registering..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                binding.buttonRegister.isEnabled = true
                binding.buttonRegister.text = "Register"

                if (task.isSuccessful) {
                    val firebaseUser = task.result.user
                    val uid = firebaseUser?.uid
                    if (uid != null) {
                        val user = User(
                            uid = uid,
                            username = username,
                            email = email,
                            joinedDate = System.currentTimeMillis(),
                            fullName = fullName,
                            avatarBase64 = ""
                        )
                        val database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users")
                        database.child(uid).setValue(user).addOnCompleteListener { dbTask ->
                            if (dbTask.isSuccessful) {
                                showSuccessSnackbar("Registration successful! ✓")
                                // Delay navigation to show success message
                                binding.root.postDelayed({
                                    findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                                }, 1000)
                            } else {
                                Log.e("RegisterFragment", "Database error", dbTask.exception)
                                showErrorSnackbar("Database error: ${dbTask.exception?.message}")
                            }
                        }
                    } else {
                        showErrorSnackbar("Registration failed: Could not get user ID")
                    }
                } else {
                    Log.e("RegisterFragment", "Registration failed", task.exception)
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already") == true ->
                            "⚠️ Email already in use"
                        task.exception?.message?.contains("password") == true ->
                            "⚠️ Password should be at least 6 characters"
                        task.exception?.message?.contains("network") == true ->
                            "⚠️ Network error. Check your connection"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "⚠️ Invalid email format"
                        else -> "⚠️ Registration failed: ${task.exception?.message}"
                    }
                    showErrorSnackbar(errorMessage)
                }
            }
    }

    private fun showErrorSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        // Set background color - RED
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))

        // Set text color - WHITE
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        textView.textSize = 14f
        textView.maxLines = 3

        // Add dismiss action
        snackbar.setAction("DISMISS") {
            snackbar.dismiss()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        snackbar.show()
    }

    private fun showSuccessSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view

        // Set background color - GREEN
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))

        // Set text color - WHITE
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        textView.textSize = 14f

        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}