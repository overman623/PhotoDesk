package com.happyhouse.photo.gallery

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.happyhouse.photo.gallery.databinding.ActivityAlbumBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlbumBinding
    private val repository by lazy { GalleryRepository(this) }
    private var bucketId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bucketId = intent.getStringExtra(EXTRA_BUCKET_ID).orEmpty()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = intent.getStringExtra(EXTRA_ALBUM_NAME) ?: getString(R.string.app_name)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val adapter = PhotoAdapter { index ->
            startActivity(
                Intent(this, ViewerActivity::class.java)
                    .putExtra(ViewerActivity.EXTRA_BUCKET_ID, bucketId)
                    .putExtra(ViewerActivity.EXTRA_INDEX, index)
            )
        }
        binding.photoList.layoutManager = GridLayoutManager(this, 3)
        binding.photoList.adapter = adapter
        lifecycleScope.launch {
            val photos = withContext(Dispatchers.IO) { repository.loadPhotos(bucketId) }
            adapter.submit(photos)
            binding.emptyText.visibility = if (photos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    companion object {
        const val EXTRA_BUCKET_ID = "bucket_id"
        const val EXTRA_ALBUM_NAME = "album_name"
    }
}
