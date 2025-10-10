package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentMovieListBinding
import com.google.android.material.chip.Chip
import com.google.firebase.database.*

/**
 * Fragment for displaying a list of movies.
 */
class MovieListFragment : Fragment() {

    private var _binding: FragmentMovieListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var movieListAdapter: MovieListAdapter

    private val movieList = mutableListOf<MovieItem>()
    private var movieListener: ValueEventListener? = null

    private var currentGenreFilter = "All"
    private val filterGenres = listOf(
        "All",
        "Action",
        "Comedy",
        "Drama",
        "Horror",
        "Thriller",
        "Sci-Fi"
    )
    private var currentSortBy = "Default"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance("https://movie-recommendation-b7ce0-default-rtdb.asia-southeast1.firebasedatabase.app").reference

        setupRecyclerView()
        setupFilterMenu()
        setupFilterChips()
        fetchMoviesFromDatabase()
    }

    /**
     * Sets up the RecyclerView for the movie list.
     */
    private fun setupRecyclerView() {
        movieListAdapter = MovieListAdapter(mutableListOf()) { movie ->
            val bundle = bundleOf("movieItem" to movie)
            findNavController().navigate(R.id.action_global_forumFragment, bundle)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = movieListAdapter
    }

    /**
     * Sets up the filter menu.
     */
    private fun setupFilterMenu() {
        binding.filterButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(requireContext(), view)
            popupMenu.menuInflater.inflate(R.menu.sort_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                currentSortBy = menuItem.title.toString()
                applyFiltersAndSorting()
                true
            }
            popupMenu.show()
        }
    }

    /**
     * Sets up the filter chips for genres.
     */
    private fun setupFilterChips() {
        // Clear any chips that might exist from the XML layout (good practice)
        binding.filterChipGroup.removeAllViews()

        for (genre in filterGenres) {
            val chip = Chip(context) // Create a new Chip
            chip.text = genre
            chip.isClickable = true
            chip.isCheckable = true

            binding.filterChipGroup.addView(chip)

            if (genre == "All") {
                chip.isChecked = true
            }
        }

        binding.filterChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                // If the user unchecks everything, we can default back to the first chip ("All")
                (group.getChildAt(0) as? Chip)?.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            val selectedChip: Chip = group.findViewById(checkedIds.first())
            currentGenreFilter = selectedChip.text.toString()
            applyFiltersAndSorting()
        }
    }

    /**
     * Fetches the movies from the database.
     */
    private fun fetchMoviesFromDatabase() {
        val moviesRef = database.child("movies")

        movieListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                movieList.clear()
                for (snapshot in dataSnapshot.children) {
                    val movie = parseMovie(snapshot)
                    if (movie != null) {
                        movieList.add(movie)
                    }
                }
                applyFiltersAndSorting()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("MovieListFragment", "loadMovies:onCancelled", databaseError.toException())
            }
        }
        moviesRef.addValueEventListener(movieListener!!)
    }

    private fun parseMovie(movieSnapshot: DataSnapshot): MovieItem? {
        val movieMap = movieSnapshot.getValue(object : GenericTypeIndicator<Map<String, Any?>>() {})
        return if (movieMap != null) {
            val rating = (movieMap["rating"] as? Number)?.toDouble()
            val numVotes = (movieMap["num_votes"] as? Number)?.toDouble()
            val runtimeMinutes = (movieMap["runtime_minutes"] as? Number)?.toDouble()

            MovieItem(
                movie_id = movieSnapshot.key,
                title = movieMap["title"] as? String,
                year = movieMap["year"]?.toString(),
                rating = rating,
                num_votes = numVotes,
                runtime_minutes = runtimeMinutes,
                directors = movieMap["directors"] as? String,
                writers = movieMap["writers"] as? String,
                genres = movieMap["genres"] as? String,
                overview = movieMap["overview"] as? String,
                crew = movieMap["crew"] as? String,
                primary_image_url = movieMap["primary_image_url"] as? String,
                thumbnail_url = movieMap["thumbnail_url"] as? String
            )
        } else {
            null
        }
    }

    /**
     * Applies the current filters and sorting to the movie list.
     */
    private fun applyFiltersAndSorting() {
        // 1. Filter the list by genre
        val filteredList = if (currentGenreFilter == "All") {
            movieList
        } else {
            movieList.filter { movie ->
                movie.genres?.contains(currentGenreFilter, ignoreCase = true) == true
            }
        }

        // 2. Sort the filtered list
        val sortedList = when (currentSortBy) {
            "Default" -> filteredList.sortedWith(
                compareByDescending<MovieItem> { it.num_votes }
                    .thenByDescending { it.rating }
            )
            "Rating" -> filteredList.sortedByDescending { it.rating }
            "Newest" -> filteredList.sortedByDescending { it.year?.toIntOrNull() }
            else -> filteredList.sortedByDescending { it.title }
        }
        movieListAdapter.updateMovies(sortedList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        movieListener?.let {
            database.child("movies").removeEventListener(it)
        }
        _binding = null
    }
}
