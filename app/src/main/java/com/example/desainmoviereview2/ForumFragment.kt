package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.FragmentForumBinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private var movieItem: MovieItem? = null
    private val posts = mutableListOf<ForumPost>()
    private lateinit var forumAdapter: ForumPostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        movieItem = arguments?.getParcelable("movieItem")

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        loadForumPostsFromJson()
    }

    private fun setupUI() {
        movieItem?.let {
            binding.movieTitle.text = it.title
            binding.movieDescription.text = "Directed by: ${it.director} (${it.releaseYear})"
            Glide.with(this).load(it.posterUrl).into(binding.moviePoster)
        }
    }

    private fun setupRecyclerView() {
        forumAdapter = ForumPostAdapter(posts)
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

    private fun loadForumPostsFromJson() {
        val currentMovieId = movieItem?.id ?: return
        try {
            val jsonString = context?.assets?.open("forum_posts.json")?.bufferedReader().use { it?.readText() }
            if (jsonString != null) {
                val allPosts = Json.decodeFromString<List<ForumPost>>(jsonString)
                posts.clear()
                posts.addAll(allPosts.filter { it.movieId == currentMovieId })
                posts.sortByDescending { it.timestamp }
                forumAdapter.notifyDataSetChanged()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun submitNewPost() {
        val content = binding.newPostEditText.text.toString().trim()
        if (content.isEmpty()) {
            binding.newPostEditText.error = "Post cannot be empty."
            return
        }

        // Since we are using a local JSON, we will just simulate adding the post.
        val newMovieId = movieItem?.id ?: ""
        val newPost = ForumPost(
            id = "post_id_${System.currentTimeMillis()}",
            movieId = newMovieId,
            title = "Post about ${movieItem?.title}",
            content = content,
            authorUid = "local_user",
            authorUsername = "Local User",
            timestamp = System.currentTimeMillis()
        )

        posts.add(0, newPost) // Add to the top of the list
        forumAdapter.notifyItemInserted(0)
        binding.forumRecyclerView.scrollToPosition(0)

        // Reset UI
        binding.newPostEditText.text.clear()
        binding.ratingBar.rating = 0f
        binding.addReviewLayout.visibility = View.GONE
        binding.addReviewFab.visibility = View.VISIBLE

        Toast.makeText(requireContext(), "Post submitted locally!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
