package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desainmoviereview2.databinding.ItemBannerBinding

class BannerAdapter(private val banners: List<MovieItem>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    // Keep the actual size of your banner list
    private val actualBannerCount = banners.size

    inner class BannerViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bannerItem: MovieItem) {
            binding.bannerImage.setImageResource(bannerItem.imageRes)
            binding.bannerTitle.text = bannerItem.title
            binding.bannerDesc.text = bannerItem.desc
            // Add any other binding logic here
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        if (actualBannerCount == 0) return // Avoid division by zero if banners list is empty

        // Use modulo to get the actual item from the list
        val actualPosition = position % actualBannerCount
        holder.bind(banners[actualPosition])
    }

    override fun getItemCount(): Int {
        // Return a very large number to simulate infinity
        // Only do this if you have items, otherwise ViewPager2 might have issues.
        return if (actualBannerCount > 0) Int.MAX_VALUE else 0
    }
}



