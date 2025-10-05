package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.desainmoviereview2.databinding.ItemBannerBinding
import jp.wasabeef.glide.transformations.BlurTransformation

class HomeBannerAdapter(
    private val onItemClick: (MovieItem) -> Unit
) : ListAdapter<MovieItem, HomeBannerAdapter.BannerViewHolder>(BannerDiffCallback()) {

    inner class BannerViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bannerItem: MovieItem) {
            val imageUrl = bannerItem.primary_image_url

            // 1. Load the main image with fitCenter (no crop)
            Glide.with(itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_movie_list)
                .fitCenter()
                .into(binding.bannerImage)

            // 2. Load the background image with centerCrop and a Blur
            Glide.with(itemView.context)
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3))) // 25 is radius, 3 is sampling
                .into(binding.bannerImageBackground)

            binding.bannerTitle.text = bannerItem.title
            // Use the new fields for the description
            binding.bannerDesc.text = "Directed by: ${bannerItem.directors}"
            itemView.setOnClickListener {
                // Use getItem(position) to ensure you get the correct, non-null item.
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val actualPosition = position % currentList.size
                    onItemClick(getItem(actualPosition))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // 4. Handle the infinite scroll logic with the new ListAdapter data source.
        if (currentList.isEmpty()) return

        val actualPosition = position % currentList.size
        holder.bind(getItem(actualPosition))
    }

    override fun getItemCount(): Int {
        // 5. Keep the infinite loop logic, but base it on the ListAdapter's list.
        return if (currentList.isNotEmpty()) Int.MAX_VALUE else 0
    }

    class BannerDiffCallback : DiffUtil.ItemCallback<MovieItem>() {
        override fun areItemsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            return oldItem.movie_id == newItem.movie_id
        }

        override fun areContentsTheSame(oldItem: MovieItem, newItem: MovieItem): Boolean {
            return oldItem == newItem
        }
    }
}
