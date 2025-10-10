package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.network.TmdbMovie

class SearchPredictionAdapter(
    private var predictions: List<TmdbMovie>,
    private val listener: (TmdbMovie) -> Unit
) : RecyclerView.Adapter<SearchPredictionAdapter.PredictionViewHolder>() {

    fun updatePredictions(newPredictions: List<TmdbMovie>) {
        predictions = newPredictions
        notifyDataSetChanged()
    }

    class PredictionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImageView: ImageView = itemView.findViewById(R.id.imageViewPoster)
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewMovieTitle)
        private val releaseDateTextView: TextView = itemView.findViewById(R.id.textViewReleaseDate)

        fun bind(movie: TmdbMovie, listener: (TmdbMovie) -> Unit) {
            titleTextView.text = movie.title
            // Mengambil tahun saja dari tanggal rilis
            releaseDateTextView.text = movie.releaseDate?.substringBefore("-") ?: "N/A"
            Glide.with(itemView.context)
                .load("https://image.tmdb.org/t/p/w200${movie.posterPath}")
                .placeholder(R.drawable.ic_movie_list)
                .error(R.drawable.ic_movie_list)
                .into(posterImageView)
            itemView.setOnClickListener { listener(movie) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_prediction, parent, false)
        return PredictionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PredictionViewHolder, position: Int) {
        holder.bind(predictions[position], listener)
    }

    override fun getItemCount(): Int = predictions.size
}