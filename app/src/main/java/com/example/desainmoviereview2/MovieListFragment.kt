package com.example.desainmoviereview2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desainmoviereview2.databinding.FragmentMovieListBinding
import com.google.android.material.chip.Chip
import com.google.firebase.database.*

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
    private var currentSortBy = "Popularity"

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
        setupSortSpinner()
        setupFilterChips()
        fetchMoviesFromDatabase()
    }

    private fun setupRecyclerView() {
        movieListAdapter = MovieListAdapter(mutableListOf()) { movie ->
            val bundle = bundleOf("movieItem" to movie)
            findNavController().navigate(R.id.action_global_forumFragment, bundle)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = movieListAdapter
    }

    private fun setupSortSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_by_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.sortBySpinner.adapter = adapter
        }

        binding.sortBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSortBy = parent.getItemAtPosition(position).toString()
                applyFiltersAndSorting() // Re-apply logic when selection changes
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

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

    private fun fetchMoviesFromDatabase() {
        val moviesRef = database.child("movies")

        movieListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                movieList.clear()
                for (snapshot in dataSnapshot.children) {
                    val movie = snapshot.getValue(MovieItem::class.java)
                    if (movie != null) {
                        // **FIX:** Manually set the movie_id from the snapshot's key
                        movie.movie_id = snapshot.key
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
        // Kalau mau tambah sort jangan lupa taro juga di strings.xml
        val sortedList = when (currentSortBy) {
            "Ranking" -> filteredList.sortedWith(
                compareByDescending<MovieItem> { it.num_votes }
                    .thenByDescending { it.rating }
            )
            "Rating" -> filteredList.sortedByDescending { it.rating }
            "Release Date" -> filteredList.sortedByDescending { it.year }
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
