package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MovieAdapter(
    private val movies: List<MovieItem>,
    private val listener: (MovieItem) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImageView: ImageView = itemView.findViewById(R.id.movie_poster)
        private val titleTextView: TextView = itemView.findViewById(R.id.movie_title)
        private val descTextView: TextView = itemView.findViewById(R.id.movie_desc)

        fun bind(movie: MovieItem, listener: (MovieItem) -> Unit) {
            titleTextView.text = movie.title
            descTextView.text = "Directed by: ${movie.director} (${movie.releaseYear})"
            
            Glide.with(itemView.context)
                .load(movie.posterUrl)
                .placeholder(R.drawable.ic_movie_list) // Optional: a placeholder image
                .error(R.drawable.ic_movie_list) // Optional: an error image
                .into(posterImageView)

            itemView.setOnClickListener { listener(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position], listener)
    }

    override fun getItemCount(): Int = movies.size
}
