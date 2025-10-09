// com.example.desainmoviereview2.NotificationAdapter.kt

package com.example.desainmoviereview2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desainmoviereview2.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position], onItemClick)
    }

    override fun getItemCount() = notifications.size

    class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: NotificationItem, onClick: (NotificationItem) -> Unit) {
            binding.messageText.text = notification.message ?: "New notification"
            binding.root.setOnClickListener { onClick(notification) }
        }
    }
}