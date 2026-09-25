package com.maestros.familias.ui.ventas

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.repository.ComprobanteRepository
import com.maestros.familias.data.session.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ComprobantesActivity : AppCompatActivity() {

    private val comprobanteRepository = ComprobanteRepository(RetrofitClient.comprobanteApi)
    private var jobBusqueda: Job? = null
    private lateinit var adapter: ComprobanteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobantes)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvComprobantes)
        adapter = ComprobanteAdapter(emptyList()) { comprobante ->
            Toast.makeText(this, "Ver detalle: ${comprobante.numero} (próximamente)", Toast.LENGTH_SHORT).show()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val tabs = findViewById<TabLayout>(R.id.tabsComprobantes)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0, 3 -> cargarCotizaciones("") // Todos / Cotizaciones -> mismo dato real por ahora
                    1 -> {
                        Toast.makeText(this@ComprobantesActivity, "Facturas: próximamente", Toast.LENGTH_SHORT).show()
                        adapter.actualizarLista(emptyList())
                        mostrarVacio(true)
                    }
                    2 -> {
                        Toast.makeText(this@ComprobantesActivity, "Boletas: próximamente", Toast.LENGTH_SHORT).show()
                        adapter.actualizarLista(emptyList())
                        mostrarVacio(true)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val txtBuscar = findViewById<TextInputEditText>(R.id.txtBuscarComprobante)
        txtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                jobBusqueda?.cancel()
                jobBusqueda = lifecycleScope.launch {
                    delay(400)
                    cargarCotizaciones(s?.toString()?.trim() ?: "")
                }
            }
        })

        cargarCotizaciones("")
    }

    private fun cargarCotizaciones(texto: String) {
        val progress = findViewById<ProgressBar>(R.id.progressComprobantes)
        progress.visibility = View.VISIBLE
        mostrarVacio(false)

        lifecycleScope.launch {
            try {
                val lista = comprobanteRepository.listarCotizaciones(
                    SessionManager.getCodAlmacen(),
                    SessionManager.getCodEmpresa(),
                    texto
                )
                progress.visibility = View.GONE
                adapter.actualizarLista(lista)
                mostrarVacio(lista.isEmpty())
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ComprobantesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                mostrarVacio(true)
            }
        }
    }

    private fun mostrarVacio(vacio: Boolean) {
        findViewById<TextView>(R.id.txtSinComprobantes).visibility = if (vacio) View.VISIBLE else View.GONE
    }
}