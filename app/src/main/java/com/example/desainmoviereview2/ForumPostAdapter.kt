package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desainmoviereview2.databinding.ItemForumPostBinding

class ForumPostAdapter(private val posts: List<ForumPost>) : RecyclerView.Adapter<ForumPostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemForumPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(private val binding: ItemForumPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: ForumPost) {
            // Corrected to use the IDs from the XML layout
            binding.textViewAuthor.text = post.authorUsername ?: "Anonymous"
            binding.textViewPostContent.text = post.content
        }
    }
}
