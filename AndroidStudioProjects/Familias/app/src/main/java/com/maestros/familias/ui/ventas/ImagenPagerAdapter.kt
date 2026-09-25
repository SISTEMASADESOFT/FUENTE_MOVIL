package com.maestros.familias.ui.ventas

import android.graphics.Bitmap
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.maestros.familias.R

class ImagenPagerAdapter(private val imagenesBase64: List<String>) :
    RecyclerView.Adapter<ImagenPagerAdapter.ViewHolder>() {

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imagen_pager, parent, false) as ImageView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bytes = Base64.decode(imagenesBase64[position], Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        holder.imageView.setImageBitmap(bitmap)
    }

    override fun getItemCount() = imagenesBase64.size
}