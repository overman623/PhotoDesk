package com.happyhouse.photo.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.happyhouse.photo.gallery.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.Holder>() {
    private val items = mutableListOf<Photo>()

    fun submit(photos: List<Photo>) {
        items.clear()
        items.addAll(photos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.photo.load(items[position].uri) {
            crossfade(true)
        }
        holder.itemView.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)
}
