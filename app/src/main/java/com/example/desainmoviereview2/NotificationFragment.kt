// com.example.desainmoviereview2.NotificationFragment.kt

package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.core.os.bundleOf

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var notificationsRef: DatabaseReference
    private val notifications = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return

        notificationsRef = FirebaseDatabase.getInstance()
            .getReference("notifications")
            .child(user.uid)

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notifications) { notification ->
            // Navigasi ke ForumFragment dengan movieItem
            notification.movieId?.let { movieId ->
                val movieItem = MovieItem(
                    movie_id = movieId,
                    title = "Movie",
                    overview = "",
                    primary_image_url = "",
                    rating = 0.0,
                    year = 0,
                    genres = "Unknown",
                    num_votes = 0
                )
                val bundle = bundleOf("movieItem" to movieItem)
                findNavController().navigate(R.id.action_global_forumFragment, bundle)
            }
        }
        binding.notificationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notificationsRecyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notifications.clear()
                for (child in snapshot.children) {
                    val notif = child.getValue(NotificationItem::class.java)
                    if (notif != null) {
                        notifications.add(notif)
                    }
                }
                notifications.sortByDescending { it.createdAt ?: 0 }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // ignore
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class NotificationItem(
    val type: String? = null,
    val message: String? = null,
    val fromUsername: String? = null,
    val movieId: String? = null,
    val postId: String? = null,
    val createdAt: Long? = null,
    val read: Boolean = false
)