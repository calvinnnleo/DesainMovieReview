package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
// import com.bumptech.glide.Glide // Jika menggunakan Glide untuk memuat gambar dari URL

class MovieAdapter(private var movies: List<MovieItem>) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    // ViewHolder: Memegang referensi ke view di dalam item_movie.xml
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posterImageView: ImageView = itemView.findViewById(R.id.movie_poster)
        val titleTextView: TextView = itemView.findViewById(R.id.movie_title)
        val descTextView: TextView = itemView.findViewById(R.id.movie_desc)

        fun bind(movie: MovieItem) {
            titleTextView.text = movie.title
            descTextView.text = "Tanggal Rilis: ${movie.desc}" // Contoh format

            // Untuk memuat gambar dari URL menggunakan Glide (contoh):
            // Glide.with(itemView.context)
            //    .load(movie.posterUrl)
            //    .placeholder(R.drawable.placeholder_image) // Opsional placeholder
            //    .error(R.drawable.error_image) // Opsional gambar error
            //    .into(posterImageView)

            // Jika gambar dari drawable:
             posterImageView.setImageResource(movie.imageRes) // Jika posterUrl adalah @DrawableRes int
        }
    }

    // Dipanggil ketika RecyclerView membutuhkan ViewHolder baru
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false) // Menggunakan item_movie.xml
        return MovieViewHolder(view)
    }

    // Dipanggil untuk menampilkan data pada posisi tertentu
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    // Mengembalikan jumlah total item dalam daftar
    override fun getItemCount(): Int = movies.size

    // Fungsi untuk memperbarui data di adapter (opsional, tapi berguna)
    fun updateMovies(newMovies: List<MovieItem>) {
        movies = newMovies
        notifyDataSetChanged() // Memberitahu RecyclerView bahwa data telah berubah
        // Pertimbangkan DiffUtil untuk performa yang lebih baik pada daftar besar
    }
}
