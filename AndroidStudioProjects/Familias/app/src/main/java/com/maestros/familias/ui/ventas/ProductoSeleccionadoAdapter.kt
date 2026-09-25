package com.maestros.familias.ui.ventas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maestros.familias.R
import com.maestros.familias.data.model.ProductoSeleccionado
import com.maestros.familias.data.util.MonedaHelper
import java.text.NumberFormat
import java.util.Locale

class ProductoSeleccionadoAdapter(
    private var items: List<ProductoSeleccionado>,
    private val codMoneda: Int,
    private val onEliminar: (ProductoSeleccionado) -> Unit,
    private val onEditar: (ProductoSeleccionado) -> Unit,
    private val soloLectura: Boolean = false

) : RecyclerView.Adapter<ProductoSeleccionadoAdapter.ViewHolder>() {



    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDesc: TextView = view.findViewById(R.id.txtDescSel)
        val txtCant: TextView = view.findViewById(R.id.txtCantSel)
        val txtPUnit: TextView = view.findViewById(R.id.txtPUnitSel)
        val txtSubtotal: TextView = view.findViewById(R.id.txtSubtotalSel)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarSel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_seleccionado, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sel = items[position]
        holder.txtDesc.text = sel.descripcion
        holder.txtCant.text = sel.cantidad.toString()
        holder.txtPUnit.text = MonedaHelper.formatear(sel.precioUnitario, codMoneda)
        holder.txtSubtotal.text = MonedaHelper.formatear(sel.subtotal, codMoneda)
        holder.itemView.setOnClickListener { onEditar(sel) }

        if(soloLectura){
            holder.btnEliminar.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
        }else{
            holder.btnEliminar.visibility = View.VISIBLE
            holder.itemView.setOnClickListener { onEditar(sel) }
            holder.btnEliminar.setOnClickListener { onEliminar(sel) }
        }
    }

    override fun getItemCount() = items.size

    fun actualizarLista(nuevaLista: List<ProductoSeleccionado>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}