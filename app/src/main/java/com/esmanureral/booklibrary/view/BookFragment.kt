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
    private var _binding:FragmentBookBinding?=null
    private val binding get()=_binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String>//izin istemek için
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>//galeriye gitmek için
    private var secilenGorsel: Uri?=null //kaynak nerede oldugunu belirtir.
    private var secilenBitmap:Bitmap?=null
    private var secilenBook:Book?=null

    private val mDisposable=CompositeDisposable()//kullan at

    private lateinit var db:BookDatabase
    private lateinit var bookDao:BookDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = Room.databaseBuilder(requireContext(), BookDatabase::class.java, "Kitaplar").build()
        bookDao = db.bookDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentBookBinding.inflate(inflater,container,false)
        val view=binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.kaydetButton.setOnClickListener {
            kaydet(it)
            binding.imageView.setOnClickListener { gorselSec(it) }
            binding.silButton.setOnClickListener { sil(it) }

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
        }
        private fun handleResponse(book:Book){
            binding.isimText.setText(book.isim)
            binding.yazarText.setText(book.yazar)
            val bitmap= BitmapFactory.decodeByteArray(book.gorsel,0,book.gorsel.size)
            binding.imageView.setImageBitmap(bitmap)
            secilenBook=book

        }

        fun kaydet(view: View){
            val isim=binding.isimText.text.toString()
            val yazar=binding.yazarText.text.toString()

            if(secilenBitmap!=null){
                val kucukBitmap=kucukBitmapOlustur(secilenBitmap!!,300)
                val outputStream= ByteArrayOutputStream()
                //compres:sıkıştırmak
                kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
                val byteDizisi=outputStream.toByteArray()

                val book=Book(isim,yazar,byteDizisi)

                //threading
                //RxJava

                mDisposable.add(
                    bookDao.insert(book)
                        .subscribeOn(Schedulers.io()) //io:veritabanı işlemleri,internet
                        .observeOn(AndroidSchedulers.mainThread())//observeOn:sonucu nerde göstericez
                        .subscribe(this::handleResponseForInsert)//sonucunda ne olacagını bir fonk atayabiliyoruzs
                )
            }
        }
        //sonucunda yapacagı
        private fun handleResponseForInsert(){
            //bir önceki fragmentte dön
            val action=BookFragmentDirections.actionBookFragmentToListeFragment()
            Navigation.findNavController(requireView()).navigate((action))

        }

        fun sil(view: View){
            if(secilenBook!=null){
                mDisposable.add(
                    bookDao.delete(book=secilenBook!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponseForInsert)
                )
            }

        }
        fun gorselSec(view: View){

            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){

                //READ_EXTERNAL_STORAGE izinini var mı yok mu kontrol ediyoruz izin verilmediyse if içine giriyor.
                if(ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES)!= PackageManager.PERMISSION_GRANTED){
                    //izin verilmemiş izin istiyoruz
                    if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                            Manifest.permission.READ_MEDIA_IMAGES)){
                        //snackbar gösterip kullanıcıya neden izin istediğimizi söyleyebiliriz.
                        Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz gerekiyor!", Snackbar.LENGTH_INDEFINITE).setAction(
                            "izin ver",
                            View.OnClickListener {
                                //izin isteyeceğiz.
                                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        ).show()
                    }
                    else{
                        //izin isteyeceğiz
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)

                    }
                }
                else{
                    //izin verilmiş,galeriye gidebilir.
                    val intenToGallery=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intenToGallery)
                }


            }
            else{
                //READ_EXTERNAL_STORAGE izinini var mı yok mu kontrol ediyoruz izin verilmediyse if içine giriyor.
                if(ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    //izin verilmemiş izin istiyoruz
                    if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                            Manifest.permission.READ_EXTERNAL_STORAGE)){
                        //snackbar gösterip kullanıcıya neden izin istediğimizi söyleyebiliriz.
                        Snackbar.make(view,"Galeriye ulaşıp görsel seçmemiz gerekiyor!", Snackbar.LENGTH_INDEFINITE).setAction(
                            "izin ver",
                            View.OnClickListener {
                                //izin isteyeceğiz.
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        ).show()
                    }
                    else{
                        //izin isteyeceğiz
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                    }
                }
                else{
                    //izin verilmiş,galeriye gidebilir.
                    val intenToGallery=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intenToGallery)
                }


            }




        }


        private fun registerLauncher(){

            //galeriye gitme işi
            //StartActivityForResult: sonucunu bize geri döndürür.
            activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                    result->
                if(result.resultCode== AppCompatActivity.RESULT_OK){//galeride görseli seçtiyse
                    val intentFromResult=result.data
                    if(intentFromResult!=null){
                        secilenGorsel=intentFromResult.data

                        try {
                            if(Build.VERSION.SDK_INT>=28){
                                val source= ImageDecoder.createSource(requireActivity().contentResolver,secilenGorsel!!)
                                secilenBitmap= ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(secilenBitmap)
                            }
                            else{
                                secilenBitmap= MediaStore.Images.Media.getBitmap(requireActivity().contentResolver,secilenGorsel)
                                binding.imageView.setImageBitmap(secilenBitmap)
                            }
                        }
                        catch (e: IOException){
                            println(e.localizedMessage)
                        }
                    }
                }
            }


            //RequestPermission:izin iste
            permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    result->
                if(result){
                    //izin verildi galeriye gidebiliriz.
                    val intenToGallery=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    activityResultLauncher.launch(intenToGallery)
                }
                else{
                    //izin verilmedi
                    Toast.makeText(requireContext(),"izin verilmedi", Toast.LENGTH_LONG).show()
                }
            }

        }
        //ALINAN RESMİ KÜÇÜLTMEMİZ GEREK

        private fun kucukBitmapOlustur(kullanicininSectigiBitmap:Bitmap,maximumBoyut:Int):Bitmap{
            var width=kullanicininSectigiBitmap.width
            var height=kullanicininSectigiBitmap.height

            val bitmapOrani:Double=width.toDouble()/height.toDouble()

            if(bitmapOrani>1){
                //görsel yatay
                width=maximumBoyut
                val kisaltilmisYukseklik=width/bitmapOrani
                height=kisaltilmisYukseklik.toInt()
            }
            else{
                //görsel dikey
                height=maximumBoyut
                val kisaltilmisGenislik=height*bitmapOrani
                width=kisaltilmisGenislik.toInt()
            }
            return Bitmap.createScaledBitmap(kullanicininSectigiBitmap,width,height,true)

        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding=null
            mDisposable.clear()
        }
    }

