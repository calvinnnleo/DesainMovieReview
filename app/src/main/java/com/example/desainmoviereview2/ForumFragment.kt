package com.example.desainmoviereview2

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.desainmoviereview2.databinding.FragmentForumBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue
import jp.wasabeef.glide.transformations.BlurTransformation
import androidx.navigation.fragment.navArgs

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val args: ForumFragmentArgs by navArgs()

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

        movieItem = args.movie

        if (movieItem == null || movieItem?.movie_id.isNullOrBlank()) {
            showSnackbar("Error: Movie data is missing.", Snackbar.LENGTH_LONG)
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
            binding.movieRatingBar.rating = (it.rating?.toFloat() ?: 0f) / 2f

            Glide.with(this).load(it.primary_image_url).placeholder(R.drawable.ic_movie_list).fitCenter().into(binding.moviePoster)
            Glide.with(this).load(it.primary_image_url).apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3))).into(binding.bgMoviePoster)
        }
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid
        forumAdapter = ForumPostAdapter(
            posts,
            currentUserId,
            onReplySubmit = { post, replyContent -> submitReplyToPost(post, replyContent) },
            onPostEdit = { post -> showEditPostDialog(post) },
            onPostDelete = { post -> deletePost(post) },
            onReplyEdit = { post, reply -> showEditReplyDialog(post, reply) },
            onReplyDelete = { post, reply -> deleteReply(post, reply) })

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
                        val post = parseForumPost(postSnapshot)
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

    private fun parseForumPost(postSnapshot: DataSnapshot): ForumPost? {
        val postMap = postSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (postMap != null) {
            val userRating = (postMap["user_rating"] as? Number)?.toInt()
            val createdAt = (postMap["created_at"] as? Number)?.toLong()
            val isEdited = postMap["isEdited"] as? Boolean ?: false

            val repliesMap = postMap["replies"] as? Map<String, Any>
            val replies = mutableMapOf<String, Reply>()
            if (repliesMap != null) {
                for ((replyId, replyData) in repliesMap) {
                    val replyMap = replyData as? Map<String, Any?>
                    if (replyMap != null) {
                        val reply = parseReply(replyMap)
                        if (reply != null) {
                            replies[replyId] = reply
                        }
                    }
                }
            }

            ForumPost(
                post_id = postSnapshot.key,
                movie_id = postMap["movie_id"] as? String,
                title = postMap["title"] as? String,
                content = postMap["content"] as? String,
                author_uid = postMap["author_uid"] as? String,
                author_username = postMap["author_username"] as? String,
                author_avatar_base64 = postMap["author_avatar_base64"] as? String ?: "",
                user_rating = userRating,
                created_at = createdAt,
                replies = replies,
                isEdited = isEdited
            )
        } else {
            null
        }
    }

    private fun parseReply(replyMap: Map<String, Any?>): Reply? {
        val createdAt = (replyMap["created_at"] as? Number)?.toLong()
        val isEdited = replyMap["isEdited"] as? Boolean ?: false

        return Reply(
            post_id = replyMap["post_id"] as? String,
            author_uid = replyMap["author_uid"] as? String,
            author_username = replyMap["author_username"] as? String,
            author_avatar_base64 = replyMap["author_avatar_base64"] as? String ?: "",
            content = replyMap["content"] as? String,
            created_at = createdAt,
            isEdited = isEdited
        )
    }

    private fun submitNewPost() {
        val user = auth.currentUser ?: return
        val currentMovieId = movieItem?.movie_id
        val content = binding.newPostEditText.text.toString().trim()

        if (content.isEmpty()) {
            showSnackbar("Content cannot be empty")
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
                showSnackbar("Post submitted!")
                binding.newPostEditText.text.clear()
                binding.ratingBar.rating = 0f
                binding.addReviewLayout.visibility = View.GONE
                binding.addReviewFab.visibility = View.VISIBLE
            }.addOnFailureListener { e ->
                showSnackbar("Failed to submit post: ${e.message}")
            }
        }.addOnFailureListener { e ->
            showSnackbar("Could not get user data: ${e.message}")
        }
    }

    private fun submitReplyToPost(post: ForumPost, replyContent: String) {
        val user = auth.currentUser ?: return
        val parentPostId = post.post_id ?: return

        if (replyContent.isEmpty()) {
            showSnackbar("Reply cannot be empty.")
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

            newReplyRef.setValue(replyMap).addOnSuccessListener {
                showSnackbar("Reply submitted!")
            }.addOnFailureListener { e ->
                showSnackbar("Failed to submit reply: ${e.message}")
            }
        }.addOnFailureListener { e ->
            showSnackbar("Could not get user data: ${e.message}")
        }
    }

    private fun showEditPostDialog(post: ForumPost) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_post, null)
        val editText = dialogView.findViewById<EditText>(R.id.edit_post_content)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.edit_post_rating)

        editText.setText(post.content)
        ratingBar.rating = post.user_rating?.toFloat() ?: 0f

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                val newRating = ratingBar.rating.toInt()
                if (newContent.isNotEmpty()) {
                    updatePost(post, newContent, newRating)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePost(post: ForumPost, newContent: String, newRating: Int) {
        val postRef = forumPostsRef.child(post.post_id!!)
        val updates = mapOf(
            "content" to newContent,
            "user_rating" to newRating,
            "isEdited" to true
        )
        postRef.updateChildren(updates).addOnSuccessListener {
            showSnackbar("Post updated!")
        }.addOnFailureListener { e ->
            showSnackbar("Failed to update post: ${e.message}")
        }
    }

    private fun deletePost(post: ForumPost) {
        forumPostsRef.child(post.post_id!!).removeValue().addOnSuccessListener {
            showSnackbar("Post deleted!")
        }.addOnFailureListener { e ->
            showSnackbar("Failed to delete post: ${e.message}")
        }
    }

    private fun showEditReplyDialog(post: ForumPost, reply: Reply) {
        val editText = EditText(requireContext()).apply {
            setText(reply.content)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Reply")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newContent = editText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    updateReply(post, reply, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateReply(post: ForumPost, reply: Reply, newContent: String) {
        val replyRef = forumPostsRef.child(post.post_id!!).child("replies").child(reply.post_id!!)
        val updates = mapOf(
            "content" to newContent,
            "isEdited" to true
        )
        replyRef.updateChildren(updates).addOnSuccessListener {
            showSnackbar("Reply updated!")
        }.addOnFailureListener { e ->
            showSnackbar("Failed to update reply: ${e.message}")
        }
    }

    private fun deleteReply(post: ForumPost, reply: Reply) {
        forumPostsRef.child(post.post_id!!).child("replies").child(reply.post_id!!).removeValue().addOnSuccessListener {
            showSnackbar("Reply deleted!")
        }.addOnFailureListener { e ->
            showSnackbar("Failed to delete reply: ${e.message}")
        }
    }

    private fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        view?.let {
            val snackbar = Snackbar.make(it, message, duration)
            snackbar.view.setBackgroundColor(it.context.getColor(android.R.color.white))
            val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(it.context.getColor(android.R.color.black))
            snackbar.show()
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
