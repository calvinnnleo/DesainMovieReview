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
import com.example.desainmoviereview2.databinding.FragmentLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Fragment for user login.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener {
            loginUser()
        }

        binding.buttonGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    /**
     * Logs in the user with the provided email and password.
     */
    private fun loginUser() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showErrorSnackbar("Please enter both email and password")
            return
        }

        // Show loading state
        binding.buttonLogin.isEnabled = false
        binding.buttonLogin.text = "Logging in..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                // Restore button state
                binding.buttonLogin.isEnabled = true
                binding.buttonLogin.text = "Login"

                if (task.isSuccessful) {
                    val firebaseUser = task.result.user
                    val uid = firebaseUser?.uid
                    if (uid != null) {
                        val userRef = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("users").child(uid)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val user = snapshot.getValue(User::class.java)
                                if (user != null && user.fcmToken.isEmpty()) {
                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                                        if (tokenTask.isSuccessful) {
                                            val fcmToken = tokenTask.result
                                            userRef.child("fcmToken").setValue(fcmToken)
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("LoginFragment", "Database error", error.toException())
                            }
                        })
                    }

                    // Navigate to home
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("There is no user record") == true ->
                            "⚠️ No account found with this email"
                        task.exception?.message?.contains("The password is invalid") == true ->
                            "⚠️ Incorrect password"
                        task.exception?.message?.contains("network") == true ->
                            "⚠️ Network error. Check your connection"
                        task.exception?.message?.contains("badly formatted") == true ->
                            "⚠️ Invalid email format"
                        else ->
                            "⚠️ Login failed: ${task.exception?.message}"
                    }
                    showErrorSnackbar(errorMessage)
                }
            }
    }

    /**
     * Shows an error message in a Snackbar.
     */
    private fun showErrorSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        // Background: red
        snackbarView.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))

        // Text: white
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        textView.textSize = 14f
        textView.maxLines = 3

        // Dismiss action
        snackbar.setAction("DISMISS") {
            snackbar.dismiss()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))

        snackbar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}