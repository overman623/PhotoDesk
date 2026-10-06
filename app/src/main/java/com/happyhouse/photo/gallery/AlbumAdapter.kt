package com.happyhouse.photo.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.happyhouse.photo.gallery.databinding.ItemAlbumBinding

class AlbumAdapter(
    private val onClick: (Album) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.Holder>() {
    private val items = mutableListOf<Album>()

    fun submit(albums: List<Album>) {
        items.clear()
        items.addAll(albums)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val album = items[position]
        holder.binding.albumName.text = album.name
        holder.binding.albumCount.text = holder.itemView.context.getString(R.string.photo_count, album.count)
        holder.binding.albumCover.load(album.coverUri) {
            crossfade(true)
        }
        holder.itemView.setOnClickListener { onClick(album) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root)
}
