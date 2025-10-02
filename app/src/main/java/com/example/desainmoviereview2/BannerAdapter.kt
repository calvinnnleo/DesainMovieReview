package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desainmoviereview2.databinding.ItemBannerBinding

class BannerAdapter(
    private val banners: List<MovieItem>,
    private val onItemClick: (MovieItem) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private val actualBannerCount = banners.size

    inner class BannerViewHolder(val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bannerItem: MovieItem) {
            binding.bannerImage.setImageResource(bannerItem.imageRes)
            binding.bannerTitle.text = bannerItem.title
            binding.bannerDesc.text = bannerItem.desc
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
