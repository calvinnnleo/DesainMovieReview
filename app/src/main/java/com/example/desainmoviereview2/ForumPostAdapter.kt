package com.example.desainmoviereview2

import android.app.AlertDialog
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ForumPostAdapter(
    private val posts: List<ForumPost>,
    private val currentUserId: String?,
    private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit,
    private val onPostEdit: (post: ForumPost) -> Unit,
    private val onPostDelete: (post: ForumPost) -> Unit,
    private val onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
    private val onReplyDelete: (post: ForumPost, reply: Reply) -> Unit
) : RecyclerView.Adapter<ForumPostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forum_post, parent, false)
        return PostViewHolder(view, onReplySubmit, onPostEdit, onPostDelete, onReplyEdit, onReplyDelete, currentUserId)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val onReplySubmit: (post: ForumPost, replyContent: String) -> Unit,
        private val onPostEdit: (post: ForumPost) -> Unit,
        private val onPostDelete: (post: ForumPost) -> Unit,
        private val onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
        private val onReplyDelete: (post: ForumPost, reply: Reply) -> Unit,
        private val currentUserId: String?
    ) : RecyclerView.ViewHolder(itemView) {

        private val authorAvatar: ImageView = itemView.findViewById(R.id.author_avatar)
        private val authorName: TextView = itemView.findViewById(R.id.author_name)
        private val postContent: TextView = itemView.findViewById(R.id.post_content)
        private val postRating: RatingBar = itemView.findViewById(R.id.post_rating)
        private val postTimestamp: TextView = itemView.findViewById(R.id.post_timestamp)
        private val editedLabel: TextView = itemView.findViewById(R.id.edited_label)
        private val replyLayout: LinearLayout = itemView.findViewById(R.id.reply_layout)
        private val replyEditText: EditText = itemView.findViewById(R.id.reply_edit_text)
        private val submitReplyButton: Button = itemView.findViewById(R.id.submit_reply_button)
        private val cancelReplyButton: Button = itemView.findViewById(R.id.cancel_reply_button)
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)
        private val replyButton: Button = itemView.findViewById(R.id.reply_button)
        private val editPostButton: ImageView = itemView.findViewById(R.id.edit_post_button)

        fun bind(post: ForumPost) {
            authorName.text = post.author_username ?: "Anonymous"
            postContent.text = post.content
            postRating.rating = post.user_rating?.toFloat() ?: 0f

            post.created_at?.let {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                postTimestamp.text = sdf.format(Date(it as Long))
            }

            if (post.isEdited) {
                editedLabel.visibility = View.VISIBLE
            } else {
                editedLabel.visibility = View.GONE
            }

            if (post.author_avatar_base64.isNullOrBlank()) {
                Glide.with(itemView.context).load(R.drawable.ic_anonymous).circleCrop().into(authorAvatar)
            } else {
                try {
                    val imageBytes = Base64.decode(post.author_avatar_base64, Base64.DEFAULT)
                    Glide.with(itemView.context).load(imageBytes).circleCrop().into(authorAvatar)
                } catch (e: IllegalArgumentException) {
                    Glide.with(itemView.context).load(R.drawable.ic_anonymous).circleCrop().into(authorAvatar)
                }
            }

            if (currentUserId == post.author_uid) {
                editPostButton.visibility = View.VISIBLE
                editPostButton.setOnClickListener {
                    showPostPopupMenu(it, post)
                }
            } else {
                editPostButton.visibility = View.GONE
            }

            replyButton.setOnClickListener {
                replyLayout.visibility = if (replyLayout.visibility == View.VISIBLE) View.GONE else View.VISIBLE
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
            commentsRecyclerView.adapter = CommentAdapter(post, sortedReplies, currentUserId, onReplyEdit, onReplyDelete)
        }

        private fun showPostPopupMenu(view: View, post: ForumPost) {
            val popup = PopupMenu(itemView.context, view)
            popup.menuInflater.inflate(R.menu.menu_edit_delete, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit -> {
                        onPostEdit(post)
                        true
                    }
                    R.id.menu_delete -> {
                        showDeleteConfirmationDialog(post)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showDeleteConfirmationDialog(post: ForumPost) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Delete") { _, _ -> onPostDelete(post) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}

class CommentAdapter(
    private val post: ForumPost,
    private val comments: List<Reply>,
    private val currentUserId: String?,
    private val onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
    private val onReplyDelete: (post: ForumPost, reply: Reply) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view, onReplyEdit, onReplyDelete, currentUserId)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(post, comments[position])
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(
        itemView: View,
        private val onReplyEdit: (post: ForumPost, reply: Reply) -> Unit,
        private val onReplyDelete: (post: ForumPost, reply: Reply) -> Unit,
        private val currentUserId: String?
    ) : RecyclerView.ViewHolder(itemView) {
        private val textViewAuthor: TextView = itemView.findViewById(R.id.textViewAuthor)
        private val textViewComment: TextView = itemView.findViewById(R.id.textViewComment)
        private val profilePicture: ImageView = itemView.findViewById(R.id.profile_picture)
        private val editCommentButton: ImageView = itemView.findViewById(R.id.edit_comment_button)
        private val editedLabel: TextView = itemView.findViewById(R.id.edited_label)

        fun bind(post: ForumPost, comment: Reply) {
            textViewAuthor.text = comment.author_username ?: "Anonymous"
            textViewComment.text = comment.content

            if (comment.isEdited) {
                editedLabel.visibility = View.VISIBLE
            } else {
                editedLabel.visibility = View.GONE
            }

            if (comment.author_avatar_base64.isNullOrBlank()) {
                Glide.with(itemView.context).load(R.drawable.ic_anonymous).circleCrop().into(profilePicture)
            } else {
                try {
                    val imageBytes = Base64.decode(comment.author_avatar_base64, Base64.DEFAULT)
                    Glide.with(itemView.context).load(imageBytes).circleCrop().into(profilePicture)
                } catch (e: IllegalArgumentException) {
                    Glide.with(itemView.context).load(R.drawable.ic_anonymous).circleCrop().into(profilePicture)
                }
            }

            if (currentUserId == comment.author_uid) {
                editCommentButton.visibility = View.VISIBLE
                editCommentButton.setOnClickListener {
                    showCommentPopupMenu(it, post, comment)
                }
            } else {
                editCommentButton.visibility = View.GONE
            }
        }

        private fun showCommentPopupMenu(view: View, post: ForumPost, comment: Reply) {
            val popup = PopupMenu(itemView.context, view)
            popup.menuInflater.inflate(R.menu.menu_edit_delete, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_edit -> {
                        onReplyEdit(post, comment)
                        true
                    }
                    R.id.menu_delete -> {
                        showDeleteConfirmationDialog(post, comment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showDeleteConfirmationDialog(post: ForumPost, comment: Reply) {
            AlertDialog.Builder(itemView.context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete") { _, _ -> onReplyDelete(post, comment) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
