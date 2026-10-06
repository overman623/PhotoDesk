package com.happyhouse.photo.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.happyhouse.photo.gallery.databinding.ItemViewerBinding

class ViewerAdapter(
    private val photos: List<Photo>
) : RecyclerView.Adapter<ViewerAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.page.load(photos[position].uri)
    }

    override fun getItemCount(): Int = photos.size

    class Holder(val binding: ItemViewerBinding) : RecyclerView.ViewHolder(binding.root)
}
