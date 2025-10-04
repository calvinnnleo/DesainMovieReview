package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desainmoviereview2.databinding.ItemCommentBinding
import com.example.desainmoviereview2.databinding.ItemForumPostBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        private val comments = mutableListOf<PostComment>()
        private val commentAdapter = CommentAdapter(comments)

        fun bind(post: ForumPost) {
            binding.textViewAuthor.text = post.authorUsername ?: "Anonymous"
            binding.textViewPostContent.text = post.content

            val averageRating = post.ratingsSummary?.averageRating?.toFloat() ?: 0f
            binding.postRating.rating = averageRating

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

            // Setup comments RecyclerView
            binding.commentsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context)
            binding.commentsRecyclerView.adapter = commentAdapter

            // Fetch comments for the post
            val commentsRef = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("post_comments").child(post.id!!)
            commentsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()
                    for (commentSnapshot in snapshot.children) {
                        val comment = commentSnapshot.getValue(PostComment::class.java)
                        if (comment != null) {
                            comments.add(comment)
                        }
                    }
                    commentAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }
}

class CommentAdapter(private val comments: List<PostComment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: PostComment) {
            binding.textViewAuthor.text = comment.authorUsername ?: "Anonymous"
            binding.textViewComment.text = comment.content
        }
    }
}