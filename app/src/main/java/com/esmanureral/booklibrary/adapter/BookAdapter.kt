package com.esmanureral.booklibrary.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.esmanureral.booklibrary.databinding.RecylerRowBinding
import com.esmanureral.booklibrary.model.Book
import com.esmanureral.booklibrary.view.ListeFragmentDirections

//KİTAP LİSTESİNİ RECLERVİEW A BAĞLAR

//BookHolder,RecyclerView in her bir satırı
class BookAdapter(val kitapListesi:List<Book>):RecyclerView.Adapter<BookAdapter.BookHolder>(){
    class BookHolder(val binding:RecylerRowBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookHolder {
        val recyclerRowBinding=RecylerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return BookHolder(recyclerRowBinding)
     /*
    LayoutInflater, XML dosyasını Kotlin View objesine çeviriyor.
    Parent, RecyclerView’ın içine yerleştirilecek görünüm grubu.
    False, satırın hemen yerleştirilmemesi gerektiğini belirtiyor.
    */
    }

    //recyclerview in kaç satır olacağı
    override fun getItemCount(): Int {
        return kitapListesi.size

    }

    override fun onBindViewHolder(holder: BookHolder, position: Int) {
        holder.binding.recylerViewTextView.text=kitapListesi[position].isim
        holder.itemView.setOnClickListener {
            val action=ListeFragmentDirections.actionListeFragmentToBookFragment(bilgi="eski",id=kitapListesi[position].id)
            Navigation.findNavController(it).navigate(action)
        }
    }
}