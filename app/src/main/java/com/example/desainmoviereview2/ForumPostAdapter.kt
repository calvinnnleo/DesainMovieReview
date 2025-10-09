package com.example.desainmoviereview2

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.ItemForumPostBinding

class ForumPostAdapter(
    private val posts: List<ForumPost>,
    private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit
) : RecyclerView.Adapter<ForumPostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemForumPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onReplySubmit)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        private val binding: ItemForumPostBinding,
        private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: ForumPost) {
            binding.textViewAuthor.text = post.author_username ?: "Anonymous"
            binding.textViewPostContent.text = post.content

            binding.postRating.rating = post.user_rating?.toFloat() ?: 0f

            if (post.author_avatar_base64.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_anonymous)
                    .circleCrop()
                    .into(binding.profilePicture)
            } else {
                try {
                    val imageBytes = Base64.decode(post.author_avatar_base64, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .load(imageBytes)
                        .circleCrop()
                        .into(binding.profilePicture)
                } catch (e: IllegalArgumentException) {
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_anonymous)
                        .circleCrop()
                        .into(binding.profilePicture)
                }
            }

            binding.replyButton.setOnClickListener {
                val isReplyLayoutVisible = binding.replyLayout.visibility == View.VISIBLE
                binding.replyLayout.visibility = if (isReplyLayoutVisible) View.GONE else View.VISIBLE
            }

            binding.cancelReplyButton.setOnClickListener {
                binding.replyLayout.visibility = View.GONE
            }

            binding.submitReplyButton.setOnClickListener {
                val replyContent = binding.replyEditText.text.toString().trim()
                if (replyContent.isNotEmpty()) {
                    onReplySubmit(post, replyContent)
                    binding.replyEditText.text.clear()
                    binding.replyLayout.visibility = View.GONE
                }
            }

            binding.commentsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            val repliesList = post.replies.values.toList()

            val sortedReplies = repliesList.sortedBy { it.created_at }

            binding.commentsRecyclerView.adapter = CommentAdapter(sortedReplies)
        }
    }
}

class CommentAdapter(private val comments: List<Reply>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewAuthor: TextView = itemView.findViewById(R.id.textViewAuthor)
        private val textViewComment: TextView = itemView.findViewById(R.id.textViewComment)
        private val profilePicture: ImageView = itemView.findViewById(R.id.profile_picture)

        fun bind(comment: Reply) {
            textViewAuthor.text = comment.author_username ?: "Anonymous"
            textViewComment.text = comment.content

            if (comment.author_avatar_base64.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_anonymous)
                    .circleCrop()
                    .into(profilePicture)
            } else {
                try {
                    val imageBytes = Base64.decode(comment.author_avatar_base64, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .load(imageBytes)
                        .circleCrop()
                        .into(profilePicture)
                } catch (e: IllegalArgumentException) {
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_anonymous)
                        .circleCrop()
                        .into(profilePicture)
                }
            }
        }
    }
}
