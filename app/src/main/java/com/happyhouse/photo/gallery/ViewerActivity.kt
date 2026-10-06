package com.happyhouse.photo.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.happyhouse.photo.gallery.databinding.ActivityViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val bucketId = intent.getStringExtra(EXTRA_BUCKET_ID).orEmpty()
        val startIndex = intent.getIntExtra(EXTRA_INDEX, 0)
        val repository = GalleryRepository(this)
        lifecycleScope.launch {
            val photos = withContext(Dispatchers.IO) { repository.loadPhotos(bucketId) }
            binding.pager.adapter = ViewerAdapter(photos)
            binding.pager.setCurrentItem(startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)), false)
            updateTitle(binding.pager.currentItem, photos.size)
            binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTitle(position, photos.size)
                }
            })
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun updateTitle(position: Int, count: Int) {
        binding.toolbar.title = if (count == 0) {
            getString(R.string.app_name)
        } else {
            getString(R.string.photo_position, position + 1, count)
        }
    }

    companion object {
        const val EXTRA_BUCKET_ID = "bucket_id"
        const val EXTRA_INDEX = "index"
    }
}
