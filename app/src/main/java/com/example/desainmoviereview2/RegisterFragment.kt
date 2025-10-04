package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.desainmoviereview2.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

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
        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }

        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun registerUser() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        val username = binding.editTextUsername.text.toString().trim() // Changed from editTextNickname

        if (email.isEmpty() || password.isEmpty() || username.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val uid = task.result.user?.uid

                    if (uid != null) {
                        val user = User(
                            uid = uid,
                            username = username, // Changed from nickname
                            email = email,
                            joinedDate = System.currentTimeMillis()
                        )

                        database.child("users").child(uid).setValue(user)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                // Navigate to home after successful registration
                                findNavController().navigate(R.id.action_registerFragment_to_homeFragment) 
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Registration failed: Could not get user ID.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
