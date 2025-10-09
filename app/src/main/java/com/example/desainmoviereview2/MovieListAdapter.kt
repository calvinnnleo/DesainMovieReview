package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

class MovieListAdapter(
    private var movies: MutableList<MovieItem>,
    private val listener: (MovieItem) -> Unit
) : RecyclerView.Adapter<MovieListAdapter.MovieViewHolder>() {

    fun updateMovies(newMovies: List<MovieItem>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }

    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImageView: ImageView = itemView.findViewById(R.id.movie_poster)
        private val titleTextView: TextView = itemView.findViewById(R.id.movie_title)
        private val descTextView: TextView = itemView.findViewById(R.id.movie_desc)
        private val detailsTextView: TextView = itemView.findViewById(R.id.movie_details)
        private val directorTextView: TextView = itemView.findViewById(R.id.movie_director)
        private val writersTextView: TextView = itemView.findViewById(R.id.movie_writers)

        fun bind(movie: MovieItem, listener: (MovieItem) -> Unit) {
            titleTextView.text = movie.title
            descTextView.text = movie.genres ?: ""

            val ratingText = String.format(Locale.US, "%.1f", movie.rating ?: 0.0)
            val yearText = movie.year?.toString() ?: "N/A"
            val runtimeText = movie.runtime_minutes?.toInt()?.toString()?.let { "$it min" } ?: "N/A"

            detailsTextView.text = "$yearText | $runtimeText | Rating: $ratingText"

            if (movie.directors == movie.writers) {
                if (movie.directors != null) {
                    directorTextView.text = "Director & Writer: ${movie.directors}"
                    writersTextView.visibility = View.GONE
                } else {
                    directorTextView.visibility = View.GONE
                    writersTextView.visibility = View.GONE
                }
            } else {
                if (movie.directors != null) {
                    directorTextView.text = "Director: ${movie.directors}"
                    directorTextView.visibility = View.VISIBLE
                } else {
                    directorTextView.visibility = View.GONE
                }

                if (movie.writers != null) {
                    writersTextView.text = "Writers: ${movie.writers}"
                    writersTextView.visibility = View.VISIBLE
                } else {
                    writersTextView.visibility = View.GONE
                }
            }

            Glide.with(itemView.context)
                .load(movie.primary_image_url)
                .placeholder(R.drawable.ic_movie_list)
                .error(R.drawable.ic_movie_list)
                .into(posterImageView)

            itemView.setOnClickListener {
                if (movie.movie_id.isNullOrBlank()) {
                    Toast.makeText(itemView.context, "Cannot open forum for this movie.", Toast.LENGTH_SHORT).show()
                } else {
                    listener(movie)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie_list, parent, false)
        return MovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position], listener)
    }

    override fun getItemCount(): Int = movies.size
}
