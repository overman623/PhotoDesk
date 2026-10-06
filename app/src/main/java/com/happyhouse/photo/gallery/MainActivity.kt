package com.happyhouse.photo.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.happyhouse.photo.gallery.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { GalleryRepository(this) }
    private val adapter = AlbumAdapter { album ->
        startActivity(
            Intent(this, AlbumActivity::class.java)
                .putExtra(AlbumActivity.EXTRA_BUCKET_ID, album.bucketId)
                .putExtra(AlbumActivity.EXTRA_ALBUM_NAME, album.name)
        )
    }
    private var pendingAlbumName: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { loadAlbums() }

    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val name = pendingAlbumName
        pendingAlbumName = null
        if (uri == null || name.isNullOrBlank()) {
            return@registerForActivityResult
        }
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TREE, uri.toString()).apply()
        } catch (security: SecurityException) {
            Toast.makeText(this, R.string.album_failed, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        createDirectory(uri, name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.albumList.layoutManager = GridLayoutManager(this, 2)
        binding.albumList.adapter = adapter
        binding.sortPhotos.setOnClickListener {
            if (hasPhotoPermission()) {
                startActivity(Intent(this, OrganizeActivity::class.java))
            } else {
                permissionLauncher.launch(requiredPermissions())
            }
        }
        if (hasPhotoPermission()) {
            loadAlbums()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasPhotoPermission()) {
            loadAlbums()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_new_album) {
            askAlbumName()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadAlbums() {
        if (!hasPhotoPermission()) {
            binding.emptyText.text = getString(R.string.permission_needed)
            binding.emptyText.visibility = android.view.View.VISIBLE
            adapter.submit(emptyList())
            return
        }
        lifecycleScope.launch {
            val albums = withContext(Dispatchers.IO) { repository.loadAlbums() }
            adapter.submit(albums)
            binding.emptyText.visibility = if (albums.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.emptyText.text = getString(R.string.no_albums)
        }
    }

    private fun askAlbumName() {
        val input = EditText(this)
        input.hint = getString(R.string.album_name)
        AlertDialog.Builder(this)
            .setTitle(R.string.new_album)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name.contains('/') || name.contains('\\')) {
                    Toast.makeText(this, R.string.album_failed, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TREE, null)
                if (saved != null) {
                    createDirectory(Uri.parse(saved), name)
                } else {
                    pendingAlbumName = name
                    folderLauncher.launch(null)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun createDirectory(treeUri: Uri, name: String) {
        val root = DocumentFile.fromTreeUri(this, treeUri)
        val created = root?.createDirectory(name)
        if (created != null) {
            Toast.makeText(this, R.string.album_created, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, R.string.album_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasPhotoPermission(): Boolean {
        return requiredPermissions().any { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    companion object {
        private const val PREFS = "photodesk"
        private const val KEY_TREE = "tree_uri"
    }
}
