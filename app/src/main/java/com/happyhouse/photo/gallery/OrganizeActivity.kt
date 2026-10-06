package com.happyhouse.photo.gallery

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.happyhouse.photo.gallery.databinding.ActivityOrganizeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OrganizeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizeBinding
    private val repository by lazy { GalleryRepository(this) }
    private val photos = mutableListOf<Photo>()
    private var index = 0
    private var slots = MutableList(OrganizeCircleView.SLOT_COUNT) { "" }
    private var pendingMove: Pair<Photo, String>? = null

    private val writeAccess = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val move = pendingMove
        pendingMove = null
        if (result.resultCode == RESULT_OK && move != null) {
            moveCurrent(move.second, requestPermission = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        slots = loadSlots()
        binding.circle.labels = slots
        binding.circle.onChooseSlot = { slot -> chooseAlbum(slot) }
        binding.circle.onFilePhoto = { slot -> fileInto(slot) }
        binding.skip.setOnClickListener { showNext() }
        loadQueue()
    }

    private fun loadQueue() {
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) { repository.loadAllPhotos() }
            photos.clear()
            photos.addAll(loaded)
            index = 0
            showCurrent()
        }
    }

    private fun showCurrent() {
        val photo = photos.getOrNull(index)
        binding.progress.text = if (photos.isEmpty()) {
            getString(R.string.sort_done)
        } else {
            getString(R.string.sort_progress, index + 1, photos.size)
        }
        if (photo == null) {
            binding.circle.photo = null
            binding.hint.text = getString(R.string.sort_done)
            return
        }
        binding.hint.text = getString(R.string.sort_hint)
        lifecycleScope.launch {
            binding.circle.photo = loadBitmap(photo)
        }
    }

    private suspend fun loadBitmap(photo: Photo): Bitmap? {
        val result = imageLoader.execute(
            ImageRequest.Builder(this)
                .data(photo.uri)
                .size(900)
                .allowHardware(false)
                .build()
        )
        val drawable = (result as? SuccessResult)?.drawable ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun fileInto(slot: Int) {
        val album = slots.getOrNull(slot).orEmpty()
        if (album.isBlank()) {
            Toast.makeText(this, R.string.slot_empty, Toast.LENGTH_SHORT).show()
            chooseAlbum(slot)
            return
        }
        moveCurrent(album, requestPermission = true)
    }

    private fun moveCurrent(album: String, requestPermission: Boolean) {
        val photo = photos.getOrNull(index) ?: return
        lifecycleScope.launch {
            val moved = withContext(Dispatchers.IO) {
                try {
                    repository.movePhoto(photo, album)
                    true
                } catch (security: SecurityException) {
                    if (requestPermission) {
                        val sender = writeRequest(security)
                        if (sender != null) {
                            pendingMove = photo to album
                            withContext(Dispatchers.Main) {
                                writeAccess.launch(IntentSenderRequest.Builder(sender).build())
                            }
                        }
                    }
                    false
                } catch (error: Exception) {
                    false
                }
            }
            if (moved) {
                photos.removeAt(index)
                if (index >= photos.size) index = 0
                showCurrent()
            } else if (pendingMove == null) {
                Toast.makeText(this@OrganizeActivity, R.string.move_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun writeRequest(error: SecurityException): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val recoverable = error as? RecoverableSecurityException ?: return null
        return recoverable.userAction.actionIntent.intentSender
    }

    private fun showNext() {
        if (photos.isEmpty()) return
        index = (index + 1) % photos.size
        showCurrent()
    }

    private fun chooseAlbum(slot: Int) {
        lifecycleScope.launch {
            val albums = withContext(Dispatchers.IO) { repository.loadAlbums() }
            val names = albums.map { it.name }.distinct().sorted()
            val options = names + getString(R.string.new_album)
            AlertDialog.Builder(this@OrganizeActivity)
                .setTitle(R.string.choose_slot_album)
                .setItems(options.toTypedArray()) { _, which ->
                    if (which == names.size) {
                        askNewAlbum(slot)
                    } else {
                        assignSlot(slot, names[which])
                    }
                }
                .show()
        }
    }

    private fun askNewAlbum(slot: Int) {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.new_album)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name.contains('/') || name.contains('\\')) {
                    Toast.makeText(this, R.string.album_failed, Toast.LENGTH_SHORT).show()
                } else {
                    assignSlot(slot, name)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun assignSlot(slot: Int, name: String) {
        slots[slot] = name
        binding.circle.labels = slots.toList()
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_SLOTS, slots.joinToString("\u001f"))
            .apply()
    }

    private fun loadSlots(): MutableList<String> {
        val saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SLOTS, null)
            ?: return MutableList(OrganizeCircleView.SLOT_COUNT) { "" }
        val parts = saved.split('\u001f')
        return MutableList(OrganizeCircleView.SLOT_COUNT) { index -> parts.getOrElse(index) { "" } }
    }

    companion object {
        private const val PREFS = "photodesk"
        private const val KEY_SLOTS = "organize_slots"
    }
}
