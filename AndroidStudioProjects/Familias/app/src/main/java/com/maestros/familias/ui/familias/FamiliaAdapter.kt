package com.maestros.familias.ui.familias

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.maestros.familias.R
import com.maestros.familias.data.model.Familia

class FamiliaAdapter(
    private var lista: List<Familia>,
    private val onEditar: (Familia) -> Unit,
    private val onEliminar: (Familia) -> Unit
) : RecyclerView.Adapter<FamiliaAdapter.FamiliaViewHolder>() {

    class FamiliaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewAccent: TextView = view.findViewById(R.id.viewAccent)
        val txtDscFamilia: TextView = view.findViewById(R.id.txtDscFamilia)
        val txtCodFamilia: TextView = view.findViewById(R.id.txtCodFamilia)

        val chipEstado: Chip = view.findViewById(R.id.chipEstado)
        val btnEditar: MaterialButton = view.findViewById(R.id.btnEditar)
        val btnEliminar: MaterialButton = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FamiliaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_familia, parent, false)
        return FamiliaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FamiliaViewHolder, position: Int) {
        val familia = lista[position]

        holder.viewAccent.text = familia.dscFamilia.firstOrNull()?.uppercase() ?: "F"


        holder.txtDscFamilia.text = familia.dscFamilia
        holder.txtCodFamilia.text = "Código: ${familia.codFamilia}"


        holder.chipEstado.text = familia.estado
        val esActivo = familia.estado.trim().equals("ACTIVO", ignoreCase = true)
        if (esActivo) {
            holder.chipEstado.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#DDF4E4"))
            holder.chipEstado.setTextColor(Color.parseColor("#1E8E3E"))
        } else {
            holder.chipEstado.chipBackgroundColor =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#FCE1E1"))
            holder.chipEstado.setTextColor(Color.parseColor("#C62828"))
        }

        holder.btnEditar.setOnClickListener { onEditar(familia) }
        holder.btnEliminar.setOnClickListener { onEliminar(familia) }
    }

    override fun getItemCount() = lista.size

    fun actualizar(nuevaLista: List<Familia>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}