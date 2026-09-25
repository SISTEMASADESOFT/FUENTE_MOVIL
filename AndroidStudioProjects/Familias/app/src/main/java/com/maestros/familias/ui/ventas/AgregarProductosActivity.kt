package com.maestros.familias.ui.ventas

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.Producto
import com.maestros.familias.data.model.ProductoSeleccionado
import com.maestros.familias.data.repository.ImagenRepository
import com.maestros.familias.data.repository.ProductoRepository
import com.maestros.familias.data.repository.TipoCambioRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.MonedaHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class AgregarProductosActivity : AppCompatActivity() {

    private val productoRepository = ProductoRepository(RetrofitClient.productoApi)
    private var jobBusqueda: Job? = null

    private var igvPorcentaje = BigDecimal.ZERO

    private var resultadosCompletos: List<Producto> = emptyList()
    private var paginaActual = 0
    private val itemsPorPagina = 6

    private val seleccionados = mutableListOf<ProductoSeleccionado>().apply {
        addAll(CotizacionEnCurso.productos)
    }

    private lateinit var adapterDisponibles: ProductoDisponibleAdapter
    private lateinit var adapterSeleccionados: ProductoSeleccionadoAdapter




    private fun getCodAlmacen(): String = SessionManager.getCodAlmacen().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_productos)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            cancelarCotizacion()
        }

        val rvDisponibles = findViewById<RecyclerView>(R.id.rvProductosDisponibles)
        val rvSeleccionados = findViewById<RecyclerView>(R.id.rvProductosSeleccionados)

        adapterDisponibles = ProductoDisponibleAdapter(
            emptyList(),
            CotizacionEnCurso.codMoneda,
             onAgregar = { producto -> mostrarDialogoProducto(producto)},
            onVerImagen = { producto -> mostrarImagenesProducto(producto)}
        )

        adapterSeleccionados = ProductoSeleccionadoAdapter(
            emptyList(),
            CotizacionEnCurso.codMoneda,
            onEliminar = { seleccionado ->
                confirmarEliminarProducto(seleccionado)
            },
            onEditar = { seleccionado ->
                mostrarDialogoProducto(seleccionado.producto, seleccionado)
            }
        )


        rvDisponibles.layoutManager = LinearLayoutManager(this)
        rvDisponibles.adapter = adapterDisponibles

        rvSeleccionados.layoutManager = LinearLayoutManager(this)
        rvSeleccionados.adapter = adapterSeleccionados

        val txtBuscar = findViewById<TextInputEditText>(R.id.txtBuscarProducto)
        txtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                jobBusqueda?.cancel()

                if (texto.length < 2) {
                    resultadosCompletos = emptyList()
                    paginaActual = 0
                    actualizarPaginaDisponibles()
                    return
                }

                jobBusqueda = lifecycleScope.launch {
                    delay(400)
                    try {
                        resultadosCompletos = productoRepository.buscar(texto, getCodAlmacen(),
                            CotizacionEnCurso.codMoneda)
                        paginaActual = 0
                        actualizarPaginaDisponibles()

                        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(txtBuscar.windowToken, 0)
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@AgregarProductosActivity,
                            "Error buscando producto: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        findViewById<android.widget.ImageButton>(R.id.btnPaginaAnterior).setOnClickListener {
            if (paginaActual > 0) {
                paginaActual--
                actualizarPaginaDisponibles()
            }
        }
        findViewById<android.widget.ImageButton>(R.id.btnPaginaSiguiente).setOnClickListener {
            val totalPaginas = (resultadosCompletos.size + itemsPorPagina - 1) / itemsPorPagina
            if (paginaActual < totalPaginas - 1) {
                paginaActual++
                actualizarPaginaDisponibles()
            }
        }

        findViewById<MaterialButton>(R.id.btnAtras).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSiguientePaso).setOnClickListener {
            if (seleccionados.isEmpty()) {
                Toast.makeText(this, "Agrega al menos un producto",
                    Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (igvPorcentaje <= BigDecimal.ZERO){
                Toast.makeText(this, "No se pudo cargar el IGV",
                    Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            CotizacionEnCurso.productos.clear()
            CotizacionEnCurso.productos.addAll(seleccionados)
            CotizacionEnCurso.igvPorcentaje = igvPorcentaje
            startActivity(android.content.Intent(this,
                ConfirmarEmitirActivity::class.java))
        }

        lifecycleScope.launch {
            try{
                val igv = TipoCambioRepository(RetrofitClient.tipoCambioApi).obtenerIgv()
                igvPorcentaje = igv.porcentaje
                CotizacionEnCurso.codTasa = igv.codTasa
                actualizarSeleccionados()
            }catch(e: Exception){
                Toast.makeText(
                    this@AgregarProductosActivity,
                    "Error obteniendo IGV: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        actualizarSeleccionados()
    }

    private fun confirmarEliminarProducto(seleccionado: ProductoSeleccionado) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmar_eliminar, null)
        dialogView.findViewById<TextView>(R.id.txtNombreProductoEliminar).text = seleccionado.descripcion

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Sí, eliminar") { _, _ ->
                seleccionados.remove(seleccionado)
                actualizarSeleccionados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun cancelarCotizacion() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar cotización")
            .setMessage(
                "¿Deseas cancelar toda la cotización? " +
                        "Se perderán los datos y productos ingresados."
            )
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, cancelar") { _, _ ->

                seleccionados.clear()
                CotizacionEnCurso.limpiar()

                val intent = Intent(this, InicioVentasActivity::class.java)


                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun actualizarPaginaDisponibles() {
        val desde = paginaActual * itemsPorPagina
        val hasta = minOf(desde + itemsPorPagina, resultadosCompletos.size)
        val pagina = if (desde < resultadosCompletos.size) resultadosCompletos.subList(desde, hasta) else emptyList()

        adapterDisponibles.actualizarLista(pagina)

        val txtPaginacion = findViewById<TextView>(R.id.txtPaginacion)
        txtPaginacion.text = if (resultadosCompletos.isEmpty()) {
            "0 resultados"
        } else {
            "${desde + 1} - $hasta de ${resultadosCompletos.size}"
        }
    }

    private fun mostrarImagenesProducto(producto: Producto) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_visualizar_imagenes, null)
        val progress = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressImagenes)
        val txtSinImagenes = dialogView.findViewById<TextView>(R.id.txtSinImagenes)
        val viewPager = dialogView.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerImagenes)

        val dialog = AlertDialog.Builder(this)
            .setTitle(producto.descripcion)
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()

        val codProducto = producto.codProducto.toIntOrNull() ?: 0

        lifecycleScope.launch {
            try {
                val imagenes = ImagenRepository(RetrofitClient.imagenApi).obtenerImagenesBase64(codProducto)
                progress.visibility = View.GONE

                if (imagenes.isEmpty()) {
                    txtSinImagenes.visibility = View.VISIBLE
                } else {
                    viewPager.visibility = View.VISIBLE
                    viewPager.adapter = ImagenPagerAdapter(imagenes)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                txtSinImagenes.visibility = View.VISIBLE
                txtSinImagenes.text = "Error al cargar imágenes: ${e.message}"
            }
        }
    }

    private fun mostrarDialogoProducto(producto: Producto, existente: ProductoSeleccionado? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_producto_seleccion, null)

        val txtDescripcion = dialogView.findViewById<TextInputEditText>(R.id.txtDescripcionDialog)
        val txtPrecio = dialogView.findViewById<TextInputEditText>(R.id.txtPrecioDialog)
        val txtCantidad = dialogView.findViewById<TextInputEditText>(R.id.txtCantidadDialog)

        txtDescripcion.setText(existente?.descripcion ?: producto.descripcion)
        txtPrecio.setText((existente?.precioUnitario ?: producto.precio).toPlainString())
        txtCantidad.setText((existente?.cantidad ?: 1).toString())

        AlertDialog.Builder(this)
            .setTitle(if (existente == null) "Agregar producto" else "Editar producto")
            .setView(dialogView)
            .setPositiveButton(if (existente == null) "Agregar" else "Guardar") { _, _ ->
                val descripcion = txtDescripcion.text.toString().trim()
                val precio = txtPrecio.text.toString().toBigDecimalOrNull()
                val cantidad = txtCantidad.text.toString().toIntOrNull()

                if (descripcion.isBlank() || precio == null || precio <= BigDecimal.ZERO || cantidad == null || cantidad <= 0) {
                    Toast.makeText(this, "Revisa descripción, precio y cantidad", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (existente != null) {
                    existente.descripcion = descripcion
                    existente.precioUnitario = precio
                    existente.cantidad = cantidad
                } else {
                    val yaExiste = seleccionados.find { it.producto.codProducto == producto.codProducto }
                    if (yaExiste != null) {
                        yaExiste.cantidad += cantidad
                    } else {
                        seleccionados.add(
                            ProductoSeleccionado(producto, cantidad, descripcion, precio)
                        )
                    }
                }
                actualizarSeleccionados()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarSeleccionados() {
        CotizacionEnCurso.productos.clear()
        CotizacionEnCurso.productos.addAll(seleccionados)

        adapterSeleccionados.actualizarLista(seleccionados.toList())

        val total = seleccionados.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
        val divisor = BigDecimal.ONE.add(igvPorcentaje)
        val subtotal = if (divisor > BigDecimal.ZERO) {
            total.divide(
                divisor,
                2,
                java.math.RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
        val igv = total.subtract(subtotal)

        findViewById<TextView>(R.id.txtSubtotal).text = MonedaHelper.formatear(subtotal, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtIgv).text = MonedaHelper.formatear(igv, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtTotal).text = MonedaHelper.formatear(total, CotizacionEnCurso.codMoneda)
    }
}