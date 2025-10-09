package com.example.desainmoviereview2

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ForumPostAdapter(
    private val posts: List<ForumPost>,
    private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit
) : RecyclerView.Adapter<ForumPostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum_post, parent, false)
        return PostViewHolder(view, onReplySubmit)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val authorAvatar: ImageView = itemView.findViewById(R.id.author_avatar)
        private val authorName: TextView = itemView.findViewById(R.id.author_name)
        private val postContent: TextView = itemView.findViewById(R.id.post_content)
        private val postRating: RatingBar = itemView.findViewById(R.id.post_rating)
        private val postTimestamp: TextView = itemView.findViewById(R.id.post_timestamp)
        private val replyLayout: LinearLayout = itemView.findViewById(R.id.reply_layout)
        private val replyEditText: EditText = itemView.findViewById(R.id.reply_edit_text)
        private val submitReplyButton: Button = itemView.findViewById(R.id.submit_reply_button)
        private val cancelReplyButton: Button = itemView.findViewById(R.id.cancel_reply_button)
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)
        private val replyButton: Button = itemView.findViewById(R.id.reply_button)

        fun bind(post: ForumPost) {
            authorName.text = post.author_username ?: "Anonymous"
            postContent.text = post.content
            postRating.rating = post.user_rating?.toFloat() ?: 0f

            post.created_at?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                postTimestamp.text = sdf.format(Date(it as Long))
            }

            if (post.author_avatar_base64.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_anonymous)
                    .circleCrop()
                    .into(authorAvatar)
            } else {
                try {
                    val imageBytes = Base64.decode(post.author_avatar_base64, Base64.DEFAULT)
                    Glide.with(itemView.context)
                        .load(imageBytes)
                        .circleCrop()
                        .into(authorAvatar)
                } catch (e: IllegalArgumentException) {
                    Glide.with(itemView.context)
                        .load(R.drawable.ic_anonymous)
                        .circleCrop()
                        .into(authorAvatar)
                }
            }

            replyButton.setOnClickListener {
                val isReplyLayoutVisible = replyLayout.visibility == View.VISIBLE
                replyLayout.visibility = if (isReplyLayoutVisible) View.GONE else View.VISIBLE
            }

            cancelReplyButton.setOnClickListener {
                replyLayout.visibility = View.GONE
            }

            submitReplyButton.setOnClickListener {
                val replyContent = replyEditText.text.toString().trim()
                if (replyContent.isNotEmpty()) {
                    onReplySubmit(post, replyContent)
                    replyEditText.text.clear()
                    replyLayout.visibility = View.GONE
                }
            }

            commentsRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
            val repliesList = post.replies.values.toList()

            val sortedReplies = repliesList.sortedBy { it.created_at }

            commentsRecyclerView.adapter = CommentAdapter(sortedReplies)
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
