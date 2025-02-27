package com.esmanureral.booklibrary.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.esmanureral.booklibrary.R
import com.esmanureral.booklibrary.adapter.BookAdapter
import com.esmanureral.booklibrary.databinding.FragmentListeBinding
import com.esmanureral.booklibrary.model.Book
import com.esmanureral.booklibrary.roomdb.BookDAO
import com.esmanureral.booklibrary.roomdb.BookDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers


class ListeFragment : Fragment() {
    private var _binding: FragmentListeBinding?=null
    private val binding get()=_binding!!
    private lateinit var db:BookDatabase
    private lateinit var bookDao:BookDAO
    private val mDisposable=CompositeDisposable()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = Room.databaseBuilder(requireContext(),BookDatabase::class.java,"Kitaplar").build()
        bookDao=db.bookDao()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentListeBinding.inflate(inflater,container,false)
        val view=binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener{yeniEkle(it)}//bunu yazmadan onClick metodu çalışmaz
        binding.listeRecylerView.layoutManager=LinearLayoutManager(requireContext())
        verileriAl()
    }

    private fun verileriAl(){
        mDisposable.add(
            bookDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )

    }
    //veri veriliyor
    private fun handleResponse(Kitaplar:List<Book>){
        val adapter=BookAdapter(Kitaplar)
        binding.listeRecylerView.adapter=adapter



    }
    //burası çalıştığında floatingbutona tıklandığında tarifFragmentine geçiş yapılıyor
    fun yeniEkle(view: View){
        val action=ListeFragmentDirections.actionListeFragmentToBookFragment(bilgi = "yeni",id=0)
        Navigation.findNavController(view).navigate(action)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
        mDisposable.clear()
    }
}