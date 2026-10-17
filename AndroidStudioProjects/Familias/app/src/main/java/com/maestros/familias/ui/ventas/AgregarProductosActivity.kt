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
import com.maestros.familias.data.util.CampoError
import com.maestros.familias.data.util.MonedaHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

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
            CampoError.limpiar(txtBuscar)

            if (seleccionados.isEmpty()) {
                CampoError.mostrar(txtBuscar, "AGREGA AL MENOS UN PRODUCTO")

                return@setOnClickListener
            }
            if (igvPorcentaje <= BigDecimal.ZERO){
                Toast.makeText(this, "NO SE PUDO CARGAR EL IGV",
                    Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            CotizacionEnCurso.productos.clear()
            CotizacionEnCurso.productos.addAll(seleccionados)
            CotizacionEnCurso.igvPorcentaje = igvPorcentaje
            startActivity(  Intent(this,
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
        txtPrecio.setText((existente?.precioUnitario ?: producto.precio)
            .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())
        txtCantidad.setText((existente?.cantidad ?: 1).toString())

        val dialog = AlertDialog.Builder(this)
            .setTitle(if(existente == null) "Agregar producto" else "Editar producto")
            .setView(dialogView)
            .setPositiveButton(if(existente == null) "Agregar" else "Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                CampoError.limpiar(txtDescripcion)
                CampoError.limpiar(txtPrecio)
                CampoError.limpiar(txtCantidad)

                val descripcion = txtDescripcion.text.toString().trim()
                val precio = txtPrecio.text.toString().toBigDecimalOrNull()
                val cantidad = txtCantidad.text.toString().toIntOrNull()

               var valido = true

                if(descripcion.isBlank()){
                    CampoError.mostrar(txtDescripcion, "INGRESA LA DESCRIPCION")
                    valido = false
                }

                if (precio == null || precio <= BigDecimal.ZERO) {
                    CampoError.mostrar(txtPrecio, "INGRESA UN PRECIO VÁLIDO")
                    valido = false
                }

                if(precio == null || cantidad == null || cantidad <= 0) {
                    CampoError.mostrar(txtCantidad, "INGRESA UNA CANTIDAD VALIDA")
                    valido = false
                }
                if (!valido || precio == null || cantidad == null) return@setOnClickListener


                if (existente != null) {
                    val precioMostrado = existente.precioUnitario.setScale(2, java.math.RoundingMode.HALF_UP)
                    if (precio.compareTo(precioMostrado) != 0) {
                        // el usuario cambió el precio: pasa a ser el nuevo precio de origen
                        existente.precioBase = precio
                        existente.monedaBase = CotizacionEnCurso.codMoneda
                        existente.precioUnitario = precio
                    }
                    existente.descripcion = descripcion
                    existente.cantidad = cantidad
                } else {
                    val yaExiste = seleccionados.find { it.producto.codProducto == producto.codProducto }
                    if (yaExiste != null) {
                        yaExiste.cantidad += cantidad
                    } else {
                        seleccionados.add(
                            ProductoSeleccionado(producto, cantidad, descripcion, precio,
                                monedaBase = CotizacionEnCurso.codMoneda)
                        )
                    }
                }
                actualizarSeleccionados()
                dialog.dismiss()
            }

            }
        dialog.show()
        }



    private fun actualizarSeleccionados() {
        if (seleccionados.isNotEmpty()) CampoError.limpiar(findViewById<View>(R.id.txtBuscarProducto))

        CotizacionEnCurso.productos.clear()
        CotizacionEnCurso.productos.addAll(seleccionados)

        adapterSeleccionados.actualizarLista(seleccionados.toList())

        val total = seleccionados.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal)
            .setScale(2, java.math.RoundingMode.HALF_UP)
        }
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