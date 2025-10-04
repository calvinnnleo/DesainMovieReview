package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.FragmentForumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var forumPostsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var commentsRef: DatabaseReference

    private var movieItem: MovieItem? = null
    private val posts = mutableListOf<ForumPost>()
    private lateinit var forumAdapter: ForumPostAdapter

    private lateinit var valueEventListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")
        forumPostsRef = db.getReference("forum_posts")
        usersRef = db.getReference("users")
        commentsRef = db.getReference("post_comments")

        movieItem = arguments?.getParcelable("movieItem")

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        fetchForumPosts()
    }

    private fun setupUI() {
        movieItem?.let {
            binding.movieTitle.text = it.title
            binding.movieDescription.text = "Directed by: ${it.director} (${it.releaseYear})"
            Glide.with(this).load(it.posterUrl).into(binding.moviePoster)
        }
    }

    private fun setupRecyclerView() {
        forumAdapter = ForumPostAdapter(posts) { post, replyContent ->
            submitReplyToPost(post, replyContent)
        }
        binding.forumRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.forumRecyclerView.adapter = forumAdapter
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addReviewFab.setOnClickListener {
            binding.addReviewLayout.visibility = View.VISIBLE
            binding.addReviewFab.visibility = View.GONE
        }

        binding.submitPostButton.setOnClickListener {
            submitNewPost()
        }
    }

    private fun fetchForumPosts() {
        val currentMovieId = movieItem?.id ?: return

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                posts.clear()
                var totalRating = 0.0
                var ratingCount = 0
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(ForumPost::class.java)
                    if (post != null) {
                        post.id = postSnapshot.key
                        posts.add(post)
                        post.ratingsSummary?.averageRating?.let {
                            totalRating += it
                            ratingCount++
                        }
                    }
                }
                posts.sortByDescending { it.timestamp } // Show newest posts first
                forumAdapter.notifyDataSetChanged()

                if (ratingCount > 0) {
                    binding.movieRatingBar.rating = (totalRating / ratingCount).toFloat()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ForumFragment", "loadPosts:onCancelled", error.toException())
            }
        }

        forumPostsRef.orderByChild("movieId").equalTo(currentMovieId).addValueEventListener(valueEventListener)
    }

    private fun submitNewPost() {
        val user = auth.currentUser
        val movie = movieItem

        if (user == null || movie == null) {
            Toast.makeText(requireContext(), "You must be logged in to post.", Toast.LENGTH_SHORT).show()
            return
        }

        val content = binding.newPostEditText.text.toString().trim()
        if (content.isEmpty()) {
            binding.newPostEditText.error = "Post cannot be empty."
            return
        }

        // First, get the current user's username from the database
        usersRef.child(user.uid).child("username").get().addOnSuccessListener {
            val username = it.getValue(String::class.java) ?: "Anonymous"
            val postId = forumPostsRef.push().key ?: return@addOnSuccessListener

            val newPost = ForumPost(
                id = postId,
                movieId = movie.id,
                title = "Post about ${movie.title}", // You can create a more sophisticated title later
                content = content,
                authorUid = user.uid,
                authorUsername = username,
                timestamp = System.currentTimeMillis(),
                ratingsSummary = RatingsSummary(binding.ratingBar.rating.toDouble(), 1)
            )

            // Now, save the new post to the database
            forumPostsRef.child(postId).setValue(newPost).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Post submitted!", Toast.LENGTH_SHORT).show()
                    // Reset UI
                    binding.newPostEditText.text.clear()
                    binding.ratingBar.rating = 0f
                    binding.addReviewLayout.visibility = View.GONE
                    binding.addReviewFab.visibility = View.VISIBLE
                } else {
                    Toast.makeText(requireContext(), "Failed to submit post: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener{
            Toast.makeText(requireContext(), "Could not verify user data to post.", Toast.LENGTH_SHORT).show()
        }
    }

    fun submitReplyToPost(post: ForumPost, replyContent: String) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "You must be logged in to reply.", Toast.LENGTH_SHORT).show()
            return
        }

        usersRef.child(user.uid).child("username").get().addOnSuccessListener {
            val username = it.getValue(String::class.java) ?: "Anonymous"
            val commentId = commentsRef.child(post.id!!).push().key ?: return@addOnSuccessListener

            val newComment = PostComment(
                id = commentId,
                authorUid = user.uid,
                authorUsername = username,
                content = replyContent,
                timestamp = System.currentTimeMillis()
            )

            commentsRef.child(post.id!!).child(commentId).setValue(newComment).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Reply submitted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to submit reply: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Could not verify user data to post.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::valueEventListener.isInitialized) {
            forumPostsRef.removeEventListener(valueEventListener)
        }
        _binding = null
    }
}
