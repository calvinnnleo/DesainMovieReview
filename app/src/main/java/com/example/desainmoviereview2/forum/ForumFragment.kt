package com.example.desainmoviereview2.forum

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RatingBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.desainmoviereview2.R
import com.example.desainmoviereview2.MyAppTheme

class ForumFragment : Fragment() {

    private val args: ForumFragmentArgs by navArgs()
    private val viewModel: ForumViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val movieItem = args.movieItem
                if (movieItem == null) {
                    findNavController().navigateUp()
                    return@setContent
                }

                val posts by viewModel.posts.collectAsState()
                val currentUserId by viewModel.currentUserId.collectAsState()

                MyAppTheme {
                    ForumScreen(
                        movieItem = movieItem,
                        posts = posts,
                        currentUserId = currentUserId,
                        onReplySubmit = { post, replyContent -> viewModel.submitReplyToPost(post, replyContent) },
                        onPostEdit = { post -> showEditPostDialog(post) },
                        onPostDelete = { post -> viewModel.deletePost(post) },
                        onReplyEdit = { post, reply -> showEditReplyDialog(post, reply) },
                        onReplyDelete = { post, reply -> viewModel.deleteReply(post, reply) },
                        onAddNewPost = { content, rating -> viewModel.submitNewPost(content, rating, movieItem) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initialize(args.movieItem)
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
                    viewModel.updatePost(post, newContent, newRating)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                    viewModel.updateReply(post, reply, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
