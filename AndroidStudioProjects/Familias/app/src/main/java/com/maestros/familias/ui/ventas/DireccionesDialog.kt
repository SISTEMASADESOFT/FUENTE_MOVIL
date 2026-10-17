package com.maestros.familias.ui.ventas

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.maestros.familias.R
import com.maestros.familias.data.model.DireccionItem
import com.maestros.familias.data.model.DistritoItem
import com.maestros.familias.data.repository.ClienteRepository

class DireccionesDialog(
    private val activity: AppCompatActivity,
    private val repo: ClienteRepository,
    private val items: MutableList<DireccionItem>,
    private val onCambio: (DireccionItem?) -> Unit
) {
    private class Sel(val dep: String, val prov: String, val dist: String, val desc: String)

    private var indiceEditando = -1
    private var distritoElegido: Sel? = null
    private var ultimosDistritos: List<DistritoItem> = emptyList()
    private var jobDistrito: Job? = null

    fun mostrar() {
        val vista = LayoutInflater.from(activity).inflate(R.layout.dialog_direcciones, null)
        val txtDistrito = vista.findViewById<TextInputEditText>(R.id.txtDistritoDlg)
        val txtDireccion = vista.findViewById<TextInputEditText>(R.id.txtDireccionDlg)
        val txtError = vista.findViewById<TextView>(R.id.txtErrorDlg)
        val btnGrabar = vista.findViewById<MaterialButton>(R.id.btnGrabarDireccion)
        val contenedor = vista.findViewById<LinearLayout>(R.id.listaDirecciones)

        val popup = ListPopupWindow(activity).apply {
            anchorView = txtDistrito
            setOnItemClickListener { _, _, pos, _ ->
                val d = ultimosDistritos.getOrNull(pos) ?: return@setOnItemClickListener
                distritoElegido = Sel(
                    d.codDepartamento.toString(), d.codProvincia.toString(),
                    d.codDistrito.toString(), d.descripcion
                )
                txtDistrito.setText(d.descripcion)
                dismiss()
                txtError.visibility = View.GONE
            }
        }

        fun error(msg: String) {
            txtError.text = msg
            txtError.visibility = View.VISIBLE
        }

        fun limpiarFormulario() {
            indiceEditando = -1
            distritoElegido = null
            txtDistrito.setText("")
            txtDireccion.setText("")
            btnGrabar.text = "GRABAR"
            txtError.visibility = View.GONE
        }

        fun pintarLista() {
            contenedor.removeAllViews()
            items.forEachIndexed { i, d ->
                val fila = LayoutInflater.from(activity)
                    .inflate(R.layout.item_direccion_fila, contenedor, false)
                fila.findViewById<TextView>(R.id.txtFilaDireccion).text = d.etiqueta

                val btnEditar = fila.findViewById<ImageButton>(R.id.btnEditarDir)
                // las direcciones que ya existen en ClienteDireccion no se editan desde aqui
                btnEditar.visibility = if (d.nueva) View.VISIBLE else View.GONE
                btnEditar.setOnClickListener {
                    indiceEditando = i
                    distritoElegido = Sel(d.codDepartamento, d.codProvincia, d.codDistrito, d.distrito)
                    txtDistrito.setText(d.distrito)
                    txtDireccion.setText(d.direccion)
                    btnGrabar.text = "ACTUALIZAR"
                    txtError.visibility = View.GONE
                }

                fila.findViewById<ImageButton>(R.id.btnQuitarDir).setOnClickListener {
                    AlertDialog.Builder(activity)
                        .setTitle("QUITAR DIRECCIÓN")
                        .setMessage(
                            if (d.nueva) "¿Quitar \"${d.etiqueta}\"?"
                            else "¿Quitar \"${d.etiqueta}\" de esta cotización?\n\nNo se borra del cliente."
                        )
                        .setPositiveButton("QUITAR") { _, _ ->
                            val pos = items.indexOf(d)
                            if (pos < 0) return@setPositiveButton
                            items.removeAt(pos)
                            if (indiceEditando == pos) limpiarFormulario()
                            else if (indiceEditando > pos) indiceEditando--
                            pintarLista()
                            onCambio(null)
                        }
                        .setNegativeButton("CANCELAR", null)
                        .show()
                }
                contenedor.addView(fila)
            }
        }

        txtDistrito.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                if (distritoElegido != null) {
                    if (texto == distritoElegido?.desc) return   // acaba de elegirlo
                    distritoElegido = null
                }
                jobDistrito?.cancel()
                if (texto.length < 3) { popup.dismiss(); return }
                jobDistrito = activity.lifecycleScope.launch {
                    delay(400)
                    try {
                        val r = repo.buscarDistritos(texto)
                        ultimosDistritos = r
                        if (r.isEmpty()) popup.dismiss()
                        else {
                            popup.setAdapter(
                                ArrayAdapter(activity, android.R.layout.simple_list_item_1,
                                    r.map { it.descripcion })
                            )
                            popup.show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(activity, "Error buscando distrito: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
        })

        btnGrabar.setOnClickListener {
            val sel = distritoElegido
            val dir = txtDireccion.text.toString().trim().uppercase()

            if (sel == null) { error("ELIGE EL DISTRITO DE LA LISTA"); return@setOnClickListener }
            if (dir.length < 3) { error("INGRESA LA DIRECCIÓN (MÍNIMO 3 CARACTERES)"); return@setOnClickListener }

            val nueva = DireccionItem(
                codDireccion = "0",
                principal = false,
                codDepartamento = sel.dep,
                codProvincia = sel.prov,
                codDistrito = sel.dist,
                direccion = dir.take(100),
                distrito = sel.desc
            )
            if (indiceEditando >= 0) items[indiceEditando] = nueva else items.add(nueva)

            limpiarFormulario()
            pintarLista()
            onCambio(nueva)
        }

        pintarLista()

        AlertDialog.Builder(activity)
            .setView(vista)
            .setPositiveButton("CERRAR", null)
            .setOnDismissListener { popup.dismiss(); jobDistrito?.cancel() }
            .create()
            .show()
    }
}