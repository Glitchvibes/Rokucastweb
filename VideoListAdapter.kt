package com.elieltv.castrokuweb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideoListAdapter(
    private val items: MutableList<DetectedVideo>,
    private val onCastClick: (DetectedVideo) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.itemTitle)
        val url: TextView = view.findViewById(R.id.itemUrl)
        val castBtn: ImageButton = view.findViewById(R.id.btnCastItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = if (item.title.isNotBlank()) item.title else "Video ${position + 1}"
        holder.url.text = item.url
        holder.castBtn.setOnClickListener { onCastClick(item) }
        holder.itemView.setOnClickListener { onCastClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newItems: List<DetectedVideo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
