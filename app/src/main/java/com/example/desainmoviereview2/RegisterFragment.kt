package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
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
import java.text.SimpleDateFormat
import java.util.*

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
        database = FirebaseDatabase.getInstance().getReference("users")

        binding.buttonRegister.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotBlank() && password.isNotBlank()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = auth.currentUser
                            val uid = firebaseUser?.uid

                            if (uid != null) {
                                // Create a date formatter for ISO 8601 format
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                                sdf.timeZone = TimeZone.getTimeZone("UTC")
                                val joinedDate = sdf.format(Date())

                                val user = hashMapOf<String, Any>(
                                    "email" to email,
                                    "joinedDate" to joinedDate
                                )

                                database.child(uid).setValue(user)
                                    .addOnSuccessListener {
                                        Log.d("RealtimeDB", "User profile created for $uid")
                                        Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_LONG).show()
                                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("RealtimeDB", "Error adding document", e)
                                        Toast.makeText(requireContext(), "Registration successful (DB error)!", Toast.LENGTH_LONG).show()
                                        findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                    }
                            } else {
                                Toast.makeText(requireContext(), "Registration successful but failed to get user ID.", Toast.LENGTH_LONG).show()
                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            }

                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Authentication failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }

        binding.buttonGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
