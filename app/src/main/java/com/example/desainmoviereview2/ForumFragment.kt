package com.example.desainmoviereview2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentForumBinding

class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val posts = mutableListOf(
        ForumPost("User1", "This is the first comment.", 4.5f, R.drawable.ic_profile),
        ForumPost("User2", "I really liked this movie!", 5f, R.drawable.ic_profile),
        ForumPost("User3", "I think the book was better.", 3f, R.drawable.ic_profile),
        ForumPost("User3", "I think the book was better.", 3f, R.drawable.ic_profile),
        ForumPost("User3", "I think the book was better.", 3f, R.drawable.ic_profile),
        ForumPost("User3", "I think the book was better.", 3f, R.drawable.ic_profile)
    )
    private lateinit var forumAdapter: ForumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieItem = arguments?.getParcelable<MovieItem>("movieItem")

        movieItem?.let {
            binding.movieTitle.text = it.title
            binding.movieDescription.text = it.desc
            binding.moviePoster.setImageResource(it.imageRes)
            binding.movieRatingBar.rating = 4.2f // Dummy rating
        }

        forumAdapter = ForumAdapter(posts)
        binding.forumRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.forumRecyclerView.adapter = forumAdapter

        binding.addReviewFab.setOnClickListener {
            binding.addReviewLayout.visibility = View.VISIBLE
            binding.addReviewFab.visibility = View.GONE
        }

        binding.submitPostButton.setOnClickListener {
            val newPostText = binding.newPostEditText.text.toString()
            val rating = binding.ratingBar.rating
            if (newPostText.isNotEmpty()) {
                val newPost = ForumPost("CurrentUser", newPostText, rating, R.drawable.ic_profile)
                forumAdapter.addPost(newPost)
                binding.newPostEditText.text.clear()
                binding.ratingBar.rating = 0f
                binding.addReviewLayout.visibility = View.GONE
                binding.addReviewFab.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
