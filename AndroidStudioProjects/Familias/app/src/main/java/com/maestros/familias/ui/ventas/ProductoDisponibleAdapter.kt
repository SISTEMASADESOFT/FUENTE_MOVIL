package com.maestros.familias.ui.ventas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maestros.familias.R
import com.maestros.familias.data.model.Producto
import com.maestros.familias.data.util.MonedaHelper
import java.text.NumberFormat
import java.util.Locale

class ProductoDisponibleAdapter(
    private var items: List<Producto>,
    private val codMoneda: Int,
    private val onAgregar: (Producto) -> Unit,
    private val onVerImagen: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoDisponibleAdapter.ViewHolder>() {



    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnVer: ImageButton = view.findViewById(R.id.btnVerImagen)
        val txtCodigo: TextView = view.findViewById(R.id.txtCodigo)
        val txtDescripcion: TextView = view.findViewById(R.id.txtDescripcion)
        val txtStock: TextView = view.findViewById(R.id.txtStock)
        val txtPrecio: TextView = view.findViewById(R.id.txtPrecio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_disponible, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val producto = items[position]
        holder.txtCodigo.text = producto.codigoInterno
        holder.txtDescripcion.text = producto.descripcion
        holder.txtStock.text = producto.stock
        holder.txtPrecio.text = MonedaHelper.formatear(producto.precio, codMoneda   )
        holder.itemView.setOnClickListener { onAgregar(producto) }
        holder.btnVer.setOnClickListener { onVerImagen(producto) }
    }

    override fun getItemCount() = items.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}