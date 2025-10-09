package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar

class HomeListAdapter(
    private val listener: (MovieItem) -> Unit
) : ListAdapter<MovieItem, HomeListAdapter.MovieViewHolder>(MovieDiffCallback()) {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImageView: ImageView = itemView.findViewById(R.id.movie_poster)
        private val titleTextView: TextView = itemView.findViewById(R.id.movie_title)

        fun bind(movie: MovieItem, listener: (MovieItem) -> Unit) {
            titleTextView.text = movie.title

            Glide.with(itemView.context)
                .load(movie.primary_image_url)
                .placeholder(R.drawable.ic_movie_list)
                .error(R.drawable.ic_movie_list)
                .into(posterImageView)

            itemView.setOnClickListener {
                // **FIX:** Add a guard clause to prevent navigation if movie_id is null.
                if (movie.movie_id.isNullOrBlank()) {
                    val snackbar = Snackbar.make(itemView, "Error: Cannot open forum for this movie.", Snackbar.LENGTH_SHORT)
                    snackbar.view.setBackgroundColor(itemView.context.getColor(android.R.color.white))
                    val textView = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                    textView.setTextColor(itemView.context.getColor(android.R.color.black))
                    snackbar.show()
                } else {
                    listener(movie)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_home, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie, listener)
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<MovieItem>() {
        override fun areItemsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            // Use a unique identifier for your movie item
            return oldItem.movie_id == newItem.movie_id
        }

        override fun areContentsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            // Check if the contents that are displayed have changed
            return oldItem == newItem
        }
    }
}
