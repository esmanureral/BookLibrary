package com.esmanureral.booklibrary.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.esmanureral.booklibrary.R
import com.esmanureral.booklibrary.databinding.FragmentBookBinding
import com.esmanureral.booklibrary.model.Book
import com.esmanureral.booklibrary.roomdb.BookDAO
import com.esmanureral.booklibrary.roomdb.BookDatabase
import com.google.android.material.snackbar.Snackbar
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream
import java.io.IOException


class BookFragment : Fragment() {
    private var _binding: FragmentBookBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var secilenGorsel: Uri? = null
    private var secilenBitmap: Bitmap? = null
    private var secilenBook: Book? = null
    private val mDisposable = CompositeDisposable()
    private lateinit var db: BookDatabase
    private lateinit var bookDao: BookDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()
        db = Room.databaseBuilder(requireContext(), BookDatabase::class.java, "Kitaplar").build()
        bookDao = db.bookDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kaydetButton.setOnClickListener {
            kaydet(it)
        }
        binding.imageView.setOnClickListener {
            gorselSec(it)
        }
        binding.silButton.setOnClickListener {
            sil(it)
        }

        arguments?.let {
            val bilgi = BookFragmentArgs.fromBundle(it).bilgi
            if (bilgi == "yeni") {
                binding.silButton.isEnabled = false
                binding.kaydetButton.isEnabled = true
            } else {
                binding.silButton.isEnabled = true
                binding.kaydetButton.isEnabled = false
                val id = BookFragmentArgs.fromBundle(it).id

                mDisposable.add(
                    bookDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponse)
                )
            }
        }
    }

    private fun handleResponse(book: Book) {
        binding.isimText.setText(book.isim)
        binding.yazarText.setText(book.yazar)
        val bitmap = BitmapFactory.decodeByteArray(book.gorsel, 0, book.gorsel.size)
        binding.imageView.setImageBitmap(bitmap)
        secilenBook = book
    }

    private fun kaydet(view: View) {
        val isim = binding.isimText.text.toString()
        val yazar = binding.yazarText.text.toString()

        if (secilenBitmap != null) {
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
            val byteDizisi = outputStream.toByteArray()

            val book = Book(isim, yazar, byteDizisi)

            mDisposable.add(
                bookDao.insert(book)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }
    }

    private fun handleResponseForInsert() {
        val action = BookFragmentDirections.actionBookFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    private fun sil(view: View) {
        secilenBook?.let {
            mDisposable.add(
                bookDao.delete(it)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleResponseForInsert)
            )
        }
    }

    private fun gorselSec(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), permission)) {
                Snackbar.make(view, "Galeriye ulaşıp görsel seçmemiz gerekiyor!", Snackbar.LENGTH_INDEFINITE)
                    .setAction("izin ver") {
                        permissionLauncher.launch(permission)
                    }
                    .show()
            } else {
                permissionLauncher.launch(permission)
            }
        } else {
            val intenToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intenToGallery)
        }
    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val intentFromResult = result.data
                intentFromResult?.data?.let { uri ->
                    secilenGorsel = uri
                    try {
                        secilenBitmap = if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                            ImageDecoder.decodeBitmap(source)
                        } else {
                            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                        }
                        binding.imageView.setImageBitmap(secilenBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intenToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intenToGallery)
            } else {
                Toast.makeText(requireContext(), "izin verilmedi", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kucukBitmapOlustur(kullanicininSectigiBitmap: Bitmap, maximumBoyut: Int): Bitmap {
        var width = kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height
        val bitmapOrani: Double = width.toDouble() / height.toDouble()

        if (bitmapOrani > 1) {
            width = maximumBoyut
            height = (width / bitmapOrani).toInt()
        } else {
            height = maximumBoyut
            width = (height * bitmapOrani).toInt()
        }

        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap, width, height, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}
