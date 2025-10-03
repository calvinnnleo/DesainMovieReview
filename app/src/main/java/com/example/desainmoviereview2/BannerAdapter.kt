package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.desainmoviereview2.databinding.ItemBannerBinding

class BannerAdapter(
    private val banners: List<MovieItem>,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private val actualBannerCount = banners.size

    inner class BannerViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bannerItem: MovieItem) {
            // Use Glide to load the image from the URL
            Glide.with(itemView.context)
                .load(bannerItem.posterUrl)
                .placeholder(R.drawable.ic_movie_list) // Optional placeholder
                .into(binding.bannerImage)

            binding.bannerTitle.text = bannerItem.title
            // Use the new fields for the description
            binding.bannerDesc.text = "Directed by: ${bannerItem.director}"
            itemView.setOnClickListener { onItemClick(bannerItem) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        if (actualBannerCount == 0) return

        val actualPosition = position % actualBannerCount
        holder.bind(banners[actualPosition])
    }

    override fun getItemCount(): Int {
        return if (actualBannerCount > 0) Int.MAX_VALUE else 0
    }
}
