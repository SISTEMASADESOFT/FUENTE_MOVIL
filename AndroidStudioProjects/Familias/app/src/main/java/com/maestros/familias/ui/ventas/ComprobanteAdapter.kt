package com.maestros.familias.ui.ventas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.maestros.familias.R
import com.maestros.familias.data.model.Comprobante

class ComprobanteAdapter(
    private var items: List<Comprobante>,
    private val onClick: (Comprobante) -> Unit
) : RecyclerView.Adapter<ComprobanteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNumero: TextView = view.findViewById(R.id.txtNumeroComp)
        val txtCliente: TextView = view.findViewById(R.id.txtClienteComp)
        val txtFecha: TextView = view.findViewById(R.id.txtFechaComp)
        val txtMonto: TextView = view.findViewById(R.id.txtMontoComp)
        val txtEstado: TextView = view.findViewById(R.id.txtEstadoComp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comprobante, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtNumero.text = item.numero
        holder.txtCliente.text = item.cliente
        holder.txtFecha.text = item.fechaEmision
        val simbolo = if (item.moneda.contains("2") || item.moneda.contains("DOL", true)) "US$" else "S/"
        holder.txtMonto.text = "$simbolo ${item.total}"
        holder.txtEstado.text = item.estado
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun actualizarLista(nuevaLista: List<Comprobante>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}