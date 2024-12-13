package com.pardeep.sbfsassignment

import android.content.Context
import android.content.Intent
import android.media.Image
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MyAdapter(var itemData: ArrayList<ItemData>,
    var recyclerInterface: RecInterface,
    var context : Context) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {
    class ViewHolder(var view : View) : RecyclerView.ViewHolder(view) {
        var cardView : CardView = view.findViewById(R.id.cardView)
        var nameTv : TextView = view.findViewById(R.id.nameTv)
        var imageView : ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recycler_layout,parent,false))
    }

    override fun getItemCount(): Int {
        return itemData.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.nameTv.setText(itemData[position].name)
        holder.cardView.setOnClickListener {
            recyclerInterface.onClick(position)
        }
        Glide.with(context)
            .load(itemData[position].image)
            .into(holder.imageView)

    }


}
