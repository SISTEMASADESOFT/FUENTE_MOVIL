package com.maestros.familias.ui.ventas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maestros.familias.R
import com.maestros.familias.data.model.AccesoRapido


class AccesoRapidoAdapter(
    private val items: List<AccesoRapido>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AccesoRapidoAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgAcceso)
        val txt: TextView = view.findViewById(R.id.txtAcceso)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_acceso_rapido, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.img.setImageResource(item.icono)
        holder.txt.text = item.label
        holder.itemView.setOnClickListener { onClick(item.label) }
    }

    override fun getItemCount() = items.size
}