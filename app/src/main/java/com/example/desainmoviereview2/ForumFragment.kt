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
import com.bumptech.glide.request.RequestOptions
import com.example.desainmoviereview2.databinding.FragmentForumBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue
import jp.wasabeef.glide.transformations.BlurTransformation

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var forumPostsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    private var movieItem: MovieItem? = null
    private val posts = mutableListOf<ForumPost>()
    private lateinit var forumAdapter: ForumPostAdapter

    private var valueEventListener: ValueEventListener? = null
    private var query: Query? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movieItem = arguments?.getParcelable("movieItem")

        if (movieItem == null || movieItem?.movie_id.isNullOrBlank()) {
            Toast.makeText(context, "Error: Movie data is missing.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        auth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app")
        forumPostsRef = db.getReference("forum_posts")
        usersRef = db.getReference("users")

        binding.addReviewFab.visibility = View.GONE
        binding.addReviewLayout.visibility = View.GONE

        setupAuthStateListener()
        setupUI()
        setupRecyclerView()
        setupClickListeners()

        fetchForumPosts()
    }

    private fun setupAuthStateListener() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("AuthState", "Listener: User is signed in with UID: ${user.uid}")
                binding.addReviewFab.visibility = View.VISIBLE
            } else {
                Log.d("AuthState", "Listener: User is signed out.")
                binding.addReviewFab.visibility = View.GONE
                binding.addReviewLayout.visibility = View.GONE
            }
        }
    }

    private fun setupUI() {
        movieItem?.let {
            binding.movieTitle.text = it.title
            binding.movieDescription.text = it.overview
            binding.movieRatingBar.rating = it.rating?.toFloat() ?: 0f
            Glide.with(this)
                .load(it.primary_image_url)
                .placeholder(R.drawable.ic_movie_list)
                .fitCenter()
                .into(binding.moviePoster)
            Glide.with(this)
                .load(it.primary_image_url)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3))) // 25 is radius, 3 is sampling
                .into(binding.bgMoviePoster)
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

        binding.addReviewFab.setOnClickListener {
            binding.addReviewLayout.visibility = View.VISIBLE
            binding.addReviewFab.visibility = View.GONE
        }

        binding.submitPostButton.setOnClickListener {
            submitNewPost()
        }
    }

    private fun fetchForumPosts() {
        val currentMovieId = movieItem?.movie_id ?: return
        query = forumPostsRef.orderByChild("movie_id").equalTo(currentMovieId)

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                posts.clear()
                if (snapshot.exists()) {
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(ForumPost::class.java)
                        if (post != null) {
                            posts.add(post)
                        }
                    }
                }
                posts.sortByDescending { it.created_at as? Long ?: 0 }
                forumAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ForumFragment", "loadPosts:onCancelled", error.toException())
            }
        }
        query?.addValueEventListener(valueEventListener!!)
    }

    private fun submitNewPost() {
        val user = auth.currentUser ?: return

        val currentMovieId = movieItem?.movie_id
        val content = binding.newPostEditText.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(context, "Content cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        usersRef.child(user.uid).get().addOnSuccessListener { dataSnapshot ->
            val userProfile = dataSnapshot.getValue(User::class.java)
            val username = userProfile?.username ?: "Anonymous"
            val avatarBase64 = userProfile?.avatarBase64 ?: ""

            val newPostRef = forumPostsRef.push()
            val postId = newPostRef.key

            val postMap = mapOf(
                "post_id" to postId,
                "movie_id" to currentMovieId,
                "content" to content,
                "author_uid" to user.uid,
                "author_username" to username,
                "author_avatar_base64" to avatarBase64,
                "user_rating" to binding.ratingBar.rating.toInt(),
                "created_at" to ServerValue.TIMESTAMP
            )

            newPostRef.setValue(postMap).addOnSuccessListener {
                Toast.makeText(requireContext(), "Post submitted!", Toast.LENGTH_SHORT).show()
                binding.newPostEditText.text.clear()
                binding.ratingBar.rating = 0f
                binding.addReviewLayout.visibility = View.GONE
                binding.addReviewFab.visibility = View.VISIBLE
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to submit post: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Could not get user data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun submitReplyToPost(post: ForumPost, replyContent: String) {
        val user = auth.currentUser ?: return
        val parentPostId = post.post_id ?: return

        if (replyContent.isEmpty()) {
            Toast.makeText(requireContext(), "Reply cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        usersRef.child(user.uid).get().addOnSuccessListener { dataSnapshot ->
            val userProfile = dataSnapshot.getValue(User::class.java)
            val username = userProfile?.username ?: "Anonymous"
            val avatarBase64 = userProfile?.avatarBase64 ?: ""

            val repliesRef = forumPostsRef.child(parentPostId).child("replies")

            val newReplyRef = repliesRef.push()
            val replyId = newReplyRef.key

            val replyMap = mapOf(
                "post_id" to replyId,
                "author_uid" to user.uid,
                "author_username" to username,
                "author_avatar_base64" to avatarBase64,
                "content" to replyContent,
                "created_at" to ServerValue.TIMESTAMP
            )

            newReplyRef.setValue(replyMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Reply submitted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to submit reply: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Could not get user data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        valueEventListener?.let { listener ->
            query?.removeEventListener(listener)
        }
        _binding = null
    }
}
