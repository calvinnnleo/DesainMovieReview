package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class ForumPost(
    val userName: String,
    val postText: String,
    val rating: Float,
    val profilePicture: Int,
    var isReply: Boolean = false
)

class ForumAdapter(private val posts: MutableList<ForumPost>) :
    RecyclerView.Adapter<ForumAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.user_name)
        val postText: TextView = view.findViewById(R.id.post_text)
        val postRating: RatingBar = view.findViewById(R.id.post_rating)
        val replyButton: Button = view.findViewById(R.id.reply_button)
        val profilePicture: ImageView = view.findViewById(R.id.profile_picture)
        val replyLayout: LinearLayout = view.findViewById(R.id.reply_layout)
        val replyEditText: EditText = view.findViewById(R.id.reply_edit_text)
        val submitReplyButton: Button = view.findViewById(R.id.submit_reply_button)
        val cancelReplyButton: Button = view.findViewById(R.id.cancel_reply_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_forum_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.userName.text = post.userName
        holder.postText.text = post.postText
        holder.profilePicture.setImageResource(post.profilePicture)

        val layoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        val context = holder.itemView.context

        if (post.isReply) {
            holder.postRating.visibility = View.GONE
            holder.replyButton.visibility = View.GONE
            holder.replyLayout.visibility = View.GONE
            // Indent replies
            val marginStartDp = 32
            val marginInPx = (marginStartDp * context.resources.displayMetrics.density).toInt()
            layoutParams.marginStart = marginInPx
        } else {
            holder.postRating.visibility = View.VISIBLE
            holder.replyButton.visibility = View.VISIBLE
            holder.postRating.rating = post.rating
            // Reset indent for original posts
            val marginStartDp = 8
            val marginInPx = (marginStartDp * context.resources.displayMetrics.density).toInt()
            layoutParams.marginStart = marginInPx

            holder.replyButton.setOnClickListener {
                holder.replyLayout.visibility = View.VISIBLE
                holder.replyButton.visibility = View.GONE
            }

            holder.cancelReplyButton.setOnClickListener {
                holder.replyLayout.visibility = View.GONE
                holder.replyButton.visibility = View.VISIBLE
                holder.replyEditText.text.clear()
            }

            holder.submitReplyButton.setOnClickListener {
                val replyText = holder.replyEditText.text.toString()
                if (replyText.isNotEmpty()) {
                    val replyPost = ForumPost(
                        userName = "CurrentUser", // Replace with actual user
                        postText = replyText,
                        rating = 0f, // No rating for replies
                        profilePicture = R.drawable.ic_profile,
                        isReply = true
                    )
                    addReply(replyPost, holder.adapterPosition)
                    holder.replyEditText.text.clear()
                    holder.replyLayout.visibility = View.GONE
                    holder.replyButton.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun getItemCount() = posts.size

    fun addPost(post: ForumPost) {
        posts.add(post)
        notifyItemInserted(posts.size - 1)
    }

    private fun addReply(reply: ForumPost, position: Int) {
        if (position != RecyclerView.NO_POSITION) {
            posts.add(position + 1, reply)
            notifyItemInserted(position + 1)
        }
    }
}
