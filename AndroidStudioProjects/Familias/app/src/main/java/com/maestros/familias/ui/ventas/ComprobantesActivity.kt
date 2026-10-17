package com.maestros.familias.ui.ventas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.Comprobante
import com.maestros.familias.data.model.Producto
import com.maestros.familias.data.model.ProductoSeleccionado
import com.maestros.familias.data.repository.ComprobanteRepository
import com.maestros.familias.data.repository.CotizacionEdicionRepository
import com.maestros.familias.data.repository.FormaPagoRepository
import com.maestros.familias.data.repository.TipoCambioRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

class ComprobantesActivity : AppCompatActivity() {

    private val comprobanteRepository = ComprobanteRepository(RetrofitClient.comprobanteApi)
    private var jobBusqueda: Job? = null
    private lateinit var adapter: ComprobanteAdapter
    private var textoActual = ""

    private var usarFecha = true
    private val desde = java.util.Calendar.getInstance()
    private val hasta = java.util.Calendar.getInstance()
    private val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("es", "PE"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobantes)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { irAInicioVentas() }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { irAInicioVentas() }
        })

        val rv = findViewById<RecyclerView>(R.id.rvComprobantes)
        adapter = ComprobanteAdapter(emptyList()) { comprobante -> mostrarOpciones(comprobante) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val swipe = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeComprobantes)
        swipe.setColorSchemeColors(android.graphics.Color.parseColor("#B08D57"))
        swipe.setOnRefreshListener { cargarCotizaciones(textoActual) }


        val chkFecha = findViewById<android.widget.CheckBox>(R.id.chkFecha)
        val txtDesde = findViewById<TextView>(R.id.txtDesde)
        val txtHasta = findViewById<TextView>(R.id.txtHasta)

        fun refrescarFechas() {
            txtDesde.text = fmt.format(desde.time)
            txtHasta.text = fmt.format(hasta.time)
            txtDesde.isEnabled = usarFecha
            txtHasta.isEnabled = usarFecha
            txtDesde.alpha = if (usarFecha) 1f else 0.4f
            txtHasta.alpha = if (usarFecha) 1f else 0.4f
        }
        refrescarFechas()

        chkFecha.setOnCheckedChangeListener { _, marcado ->
            usarFecha = marcado
            refrescarFechas()
            cargarCotizaciones(textoActual)
        }
        txtDesde.setOnClickListener {
            elegirFecha(desde) {
                if (desde.after(hasta)) hasta.time = desde.time
                refrescarFechas(); cargarCotizaciones(textoActual)
            }
        }
        txtHasta.setOnClickListener {
            elegirFecha(hasta) {
                if (hasta.before(desde)) desde.time = hasta.time
                refrescarFechas(); cargarCotizaciones(textoActual)
            }
        }


        val txtBuscar = findViewById<TextInputEditText>(R.id.txtBuscarComprobante)
        txtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                jobBusqueda?.cancel()
                jobBusqueda = lifecycleScope.launch {
                    delay(400)
                    textoActual = s?.toString()?.trim() ?: ""
                    cargarCotizaciones(textoActual)
                }
            }
        })

        cargarCotizaciones("")
    }

    private fun irAInicioVentas() {
        CotizacionEnCurso.limpiar()
        startActivity(Intent(this, InicioVentasActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun elegirFecha(cal: java.util.Calendar, alElegir: () -> Unit) {
        android.app.DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            alElegir()
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
    private fun cargarCotizaciones(texto: String) {
        val progress = findViewById<ProgressBar>(R.id.progressComprobantes)
        val swipe = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeComprobantes)
        val contador = findViewById<TextView>(R.id.txtContadorComp)
        if (!swipe.isRefreshing) progress.visibility = View.VISIBLE
        mostrarVacio(false)

        lifecycleScope.launch {
            try {
                val lista = comprobanteRepository.listarCotizaciones(
                    SessionManager.getCodAlmacen(),
                    SessionManager.getCodEmpresa(),
                    texto,
                    usarFecha,
                    fmt.format(desde.time),
                    fmt.format(hasta.time)
                )
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                adapter.actualizarLista(lista)
                contador.text = if (lista.size == 1) "1 cotización" else "${lista.size} cotizaciones"
                mostrarVacio(lista.isEmpty())
            } catch (e: Exception) {
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                Toast.makeText(this@ComprobantesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                mostrarVacio(true)
            }
        }
    }


    private fun mostrarOpciones(c: Comprobante) {
        val anulada = c.estado.contains("ANUL", ignoreCase = true)
        val pendiente = c.estado.contains("PEND", ignoreCase = true)

        val opciones = mutableListOf("Ver PDF")
        val acciones = mutableListOf<() -> Unit>({ verPdf(c) })

        if (pendiente) {
            opciones.add("Editar cotización")
            acciones.add { editarCotizacion(c) }
        }
        if (!anulada) {
            opciones.add("Anular cotización")
            acciones.add { pedirMotivoAnulacion(c) }
        }

        AlertDialog.Builder(this)
            .setTitle(c.numero)
            .setItems(opciones.toTypedArray()) { _, i -> acciones[i]() }
            .show()
    }


    private fun verPdf(c: Comprobante) {
        val progress = findViewById<ProgressBar>(R.id.progressComprobantes)
        progress.visibility = View.VISIBLE
        val serie = c.numero.substringBefore("-")

        lifecycleScope.launch {
            try {
                val base64 = comprobanteRepository.obtenerPdf(
                    c.codigo.toInt(),
                    SessionManager.getCodEmpresa(),
                    SessionManager.getCodAlmacen(),
                    serie
                )
                val archivo = withContext(Dispatchers.IO) {
                    val dir = File(cacheDir, "pdfs").apply { mkdirs() }
                    val f = File(dir, "cotizacion_${c.numero}.pdf")
                    f.writeBytes(Base64.decode(base64, Base64.DEFAULT))
                    f
                }
                progress.visibility = View.GONE
                abrirPdf(archivo)
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ComprobantesActivity, "Error PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun abrirPdf(archivo: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", archivo)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No tienes una app para abrir PDF", Toast.LENGTH_LONG).show()
        }
    }


    private fun pedirMotivoAnulacion(c: Comprobante) {
        val input = EditText(this).apply {
            hint = "Motivo de anulación"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Anular ${c.numero}")
            .setMessage("Esta acción no se puede deshacer.")
            .setView(input)
            .setPositiveButton("Anular", null)
            .setNegativeButton("Cancelar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val motivo = input.text.toString().trim()
                        if (motivo.isEmpty()) {
                            input.error = "INGRESA EL MOTIVO"
                        } else {
                            dialog.dismiss()
                            anularCotizacion(c, motivo)
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun anularCotizacion(c: Comprobante, motivo: String) {
        val progress = findViewById<ProgressBar>(R.id.progressComprobantes)
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val msg = comprobanteRepository.anular(
                    c.codigo.toInt(),
                    SessionManager.getCodUsuario(),
                    motivo
                )
                progress.visibility = View.GONE
                Toast.makeText(this@ComprobantesActivity, msg, Toast.LENGTH_LONG).show()
                cargarCotizaciones(textoActual)
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ComprobantesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun editarCotizacion(c: Comprobante) {
        val progress = findViewById<ProgressBar>(R.id.progressComprobantes)
        progress.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val cot = CotizacionEdicionRepository(RetrofitClient.cotizacionApi)
                    .obtener(c.codigo.toInt())

                // 1) La app trabaja con precios con IGV
                if (cot.flagIgv != 1)
                    throw Exception("ESTA COTIZACIÓN ES SIN IGV Y NO SE PUEDE EDITAR DESDE LA APP")

                // 2) La moneda de la cotización debe existir en el combo
                val monedas = FormaPagoRepository(RetrofitClient.formaPagoApi).listarMonedas()
                val moneda = monedas.find { it.codConcepto.toIntOrNull() == cot.codMoneda }
                    ?: throw Exception("MONEDA NO RECONOCIDA (CÓDIGO ${cot.codMoneda})")

                // 3) Los montos deben cuadrar con el total de la lista
                val sumaItems = cot.detalle.fold(BigDecimal.ZERO) { acc, d ->
                    acc.add(d.precio.multiply(BigDecimal(d.cantidad)).setScale(2, RoundingMode.HALF_UP))
                }
                val totalLista = c.total.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (totalLista <= BigDecimal.ZERO || sumaItems.subtract(totalLista).abs() > BigDecimal("0.10"))
                    throw Exception(
                        "LOS MONTOS NO CUADRAN (APP ${sumaItems.setScale(2, RoundingMode.HALF_UP)} / LISTA ${c.total})"
                    )

                val igv = TipoCambioRepository(RetrofitClient.tipoCambioApi).obtenerIgv()

                CotizacionEnCurso.limpiar()
                CotizacionEnCurso.codProformaEdicion = cot.codProforma
                CotizacionEnCurso.cliente = cot.cliente
                CotizacionEnCurso.serie = cot.serie
                CotizacionEnCurso.numero = cot.numero
                CotizacionEnCurso.fechaEmision = cot.emision
                CotizacionEnCurso.codMoneda = cot.codMoneda
                CotizacionEnCurso.moneda = moneda.descripcion
                CotizacionEnCurso.tipoCambio = cot.tipoCambio
                CotizacionEnCurso.codFormaPago = cot.codFormaPago.toString()
                CotizacionEnCurso.codTasa = igv.codTasa
                CotizacionEnCurso.igvPorcentaje = igv.porcentaje
                CotizacionEnCurso.codEmpleado = cot.codEmpleado
                CotizacionEnCurso.placa = cot.placa
                CotizacionEnCurso.km = cot.km
                CotizacionEnCurso.observacion = cot.observacion
                CotizacionEnCurso.atencion = cot.atencion
                CotizacionEnCurso.referencia = cot.referencia
                CotizacionEnCurso.nroOperacion = cot.nroOperacion
                CotizacionEnCurso.flagComisionable = cot.flagComisionable
                CotizacionEnCurso.flagConCodigo = cot.flagConCodigo

                cot.detalle.forEach { d ->
                    val precio = d.precio   // ya viene con IGV y con todos sus decimales, igual que en la web
                    val producto = Producto(
                        codigoInterno = "",
                        descripcion = d.descripcion,
                        stock = "",
                        precio = precio,
                        codProducto = d.codProducto
                    )
                    CotizacionEnCurso.productos.add(
                        ProductoSeleccionado(producto, d.cantidad, d.descripcion, precio, monedaBase = cot.codMoneda)
                    )
                }

                progress.visibility = View.GONE
                startActivity(
                    Intent(this@ComprobantesActivity, NuevaCotizacionActivity::class.java)
                        .putExtra("EDITAR", true)
                )
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@ComprobantesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarVacio(vacio: Boolean) {
        val tv = findViewById<TextView>(R.id.txtSinComprobantes)
        tv.text = if (usarFecha)
            "No hay cotizaciones en el rango seleccionado.\nCambia las fechas o desmarca FECHA para ver todas."
        else
            "No se encontraron cotizaciones"
        tv.visibility = if (vacio) View.VISIBLE else View.GONE
    }

    override fun onRestart() {
        super.onRestart()
        cargarCotizaciones(textoActual)
    }
}

