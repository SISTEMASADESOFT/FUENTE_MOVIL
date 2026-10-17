package com.maestros.familias.ui.ventas

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.Cliente
import com.maestros.familias.data.model.ConsultaDocumento
import com.maestros.familias.data.model.DireccionItem
import com.maestros.familias.data.model.DistritoItem
import com.maestros.familias.data.model.Moneda
import com.maestros.familias.data.repository.ClienteRepository
import com.maestros.familias.data.repository.CorrelativoRepository
import com.maestros.familias.data.repository.DocumentoRepository
import com.maestros.familias.data.repository.FormaPagoRepository
import com.maestros.familias.data.repository.TipoCambioRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.Documentos
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.CampoError
import com.maestros.familias.data.util.ErrorHelper
import com.maestros.familias.data.util.MonedaHelper
import com.maestros.familias.data.util.ValidacionHelper
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NuevaCotizacionActivity : AppCompatActivity() {

    private val clienteRepository = ClienteRepository(RetrofitClient.clienteApi)
    private var jobBusqueda: kotlinx.coroutines.Job? = null
    private var clienteSeleccionado: Cliente? = null
    private var ultimosResultados: List<Cliente> = emptyList()

    private var pedirDistrito = false
    private var distritoElegido: DistritoItem? = null
    private var jobDistrito: kotlinx.coroutines.Job? = null
    private var ultimosDistritos: List<DistritoItem> = emptyList()

    private val direcciones = mutableListOf<DireccionItem>()
    private var direccionElegida: DireccionItem? = null
    private var jobDirecciones: kotlinx.coroutines.Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_cotizacion)

        val modoEdicion = intent.getBooleanExtra("EDITAR", false) && CotizacionEnCurso.esEdicion
        val tipoDoc = if(modoEdicion) 15 else intent.getIntExtra("TIPO_DOC", 15)
        //Si quedó una edición a medias y ahora se abre cotización nueva, se limpia
        if (!modoEdicion && (CotizacionEnCurso.esEdicion || CotizacionEnCurso.tipoDoc != tipoDoc)){
            CotizacionEnCurso.limpiar()
        }
        CotizacionEnCurso.tipoDoc = tipoDoc
        val config = Documentos.porTipo(tipoDoc)

        setDatoCliente(R.id.rowRuc, "RUC", "-")
        setDatoCliente(R.id.rowRazonSocial, "Razón social", "-")


        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = if (modoEdicion) "Editar cotización" else config.titulo

        val txtBuscarCliente = findViewById<TextInputEditText>(R.id.txtBuscarCliente)
        val listPopup = android.widget.ListPopupWindow(this).apply {
            anchorView = txtBuscarCliente
            setOnItemClickListener { _, _, position, _ ->
                val cliente = ultimosResultados.getOrNull(position) ?: return@setOnItemClickListener
                clienteSeleccionado = cliente

                val esNuevo = cliente.codCtaCte == "0"
                pedirDistrito = false

                mostrarPanelClienteNuevo(false)

                setDatoCliente(
                    R.id.rowRuc,
                    if (cliente.ruc.length == 8) "DNI" else "RUC",
                    cliente.ruc
                )

                setDatoCliente(
                    R.id.rowRazonSocial,
                    "Razon social",
                    nombreLimpio(cliente)
                )



                if (esNuevo) {
                    cargarDireccionDeApi(cliente)
                } else {
                    cargarDirecciones(cliente)
                }

                txtBuscarCliente.setText(cliente.razonSocial)
                dismiss()

                CampoError.limpiar(
                    txtBuscarCliente,
                    findViewById<TextView>(R.id.txtErrorCliente)
                )



            }
        }

        if (modoEdicion) {
            val cli = CotizacionEnCurso.cliente
            if (cli != null) {
                clienteSeleccionado = cli
                setDatoCliente(R.id.rowRuc, if(cli.ruc.length == 8) "DNI" else "RUC", cli.ruc)
                setDatoCliente(R.id.rowRazonSocial, "Razón social", nombreLimpio(cli))

                txtBuscarCliente.setText(cli.razonSocial)

                if (cli.codCtaCte != "0") cargarDirecciones(cli)
            }
        }

        txtBuscarCliente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                //si el usuario selecciona un cliente y luego edita el texto, invalidamos la selección
                if (clienteSeleccionado != null && s?.toString() != clienteSeleccionado?.razonSocial) {
                    clienteSeleccionado = null
                    pedirDistrito = false
                    mostrarPanelClienteNuevo(false)

                    ocultarDirecciones()

                    setDatoCliente(R.id.rowRuc, "RUC", "-")
                    setDatoCliente(R.id.rowRazonSocial, "Razón social", "-")

                }

                val texto = s?.toString()?.trim() ?: ""
                jobBusqueda?.cancel()

                // si el texto es justo el cliente que se acaba de elegir, no se vuelve a buscar
                if (clienteSeleccionado != null && texto == clienteSeleccionado?.razonSocial?.trim()) {
                    listPopup.dismiss()
                    return
                }

                if (texto.length < 3) {
                    listPopup.dismiss()
                    return
                }

                jobBusqueda = lifecycleScope.launch {
                    kotlinx.coroutines.delay(400)
                    if (texto.length == 8 && texto.all { it.isDigit() }) kotlinx.coroutines.delay(600)
                    try {
                        val resultados = clienteRepository.buscar(texto)
                        ultimosResultados = resultados

                        //solo se muestran clientes que YA existen
                        if (resultados.isEmpty()) {
                            listPopup.dismiss()
                            val esDoc = texto.all { it.isDigit() } &&
                                    (texto.length == 11 || (!config.soloRuc && texto.length == 8))
                            if (esDoc) {
                                when (val r = clienteRepository.consultarDocumento(texto, SessionManager.getCodEmpresa())) {
                                    is ConsultaDocumento.Nuevo -> {
                                        ultimosResultados = listOf(r.cliente)
                                        listPopup.setAdapter(
                                            ArrayAdapter(
                                                this@NuevaCotizacionActivity,
                                                android.R.layout.simple_list_item_1,
                                                listOf("➕ NUEVO: ${r.cliente.razonSocial}")
                                            )
                                        )
                                        listPopup.show()
                                    }
                                    is ConsultaDocumento.Existe -> {
                                        ultimosResultados = listOf(r.cliente)
                                        listPopup.setAdapter(
                                            ArrayAdapter(
                                                this@NuevaCotizacionActivity,
                                                android.R.layout.simple_list_item_1,
                                                listOf("✔ ${r.cliente.razonSocial}")
                                            )
                                        )
                                        listPopup.show()
                                    }
                                }
                            }
                        } else {
                            listPopup.setAdapter(
                                ArrayAdapter(
                                    this@NuevaCotizacionActivity,
                                    android.R.layout.simple_list_item_1,
                                    resultados.map { it.razonSocial }
                                )
                            )
                            listPopup.show()
                        }
                    } catch (e: Exception) {
                       Toast.makeText(
                            this@NuevaCotizacionActivity,
                            "Error buscando cliente: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })




        val txtDistritoNuevo = findViewById<TextInputEditText>(R.id.txtDistritoNuevo)
        val popupDistrito = android.widget.ListPopupWindow(this).apply {
            anchorView = txtDistritoNuevo
            setOnItemClickListener { _, _, position, _ ->
                val d = ultimosDistritos.getOrNull(position) ?: return@setOnItemClickListener
                distritoElegido = d
                txtDistritoNuevo.setText(d.descripcion)
                dismiss()
                CampoError.limpiar(txtDistritoNuevo, findViewById<TextView>(R.id.txtErrorDistrito))
            }
        }
        txtDistritoNuevo.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                if (distritoElegido != null) {
                    if (texto == distritoElegido?.descripcion) return   // acaba de elegirlo
                    distritoElegido = null
                }
                jobDistrito?.cancel()
                if (texto.length < 3) { popupDistrito.dismiss(); return }
                jobDistrito = lifecycleScope.launch {
                    kotlinx.coroutines.delay(400)
                    try {
                        val r = clienteRepository.buscarDistritos(texto)
                        ultimosDistritos = r
                        if (r.isEmpty()) popupDistrito.dismiss()
                        else {
                            popupDistrito.setAdapter(
                                ArrayAdapter(this@NuevaCotizacionActivity,
                                    android.R.layout.simple_list_item_1, r.map { it.descripcion })
                            )
                            popupDistrito.show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@NuevaCotizacionActivity,
                            "Error buscando distrito: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })


        val ddlMoneda = findViewById<AutoCompleteTextView>(R.id.ddlMoneda)
        var monedasCache: List<Moneda> = emptyList()
        val txtTipoCambio = findViewById<TextInputEditText>(R.id.txtTipoCambio)

        lifecycleScope.launch {
            try {
                monedasCache =  FormaPagoRepository(RetrofitClient.formaPagoApi).listarMonedas()
                if (monedasCache.isNotEmpty()) {
                    ddlMoneda.setAdapter(
                        ArrayAdapter(
                            this@NuevaCotizacionActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            monedasCache.map { it.descripcion }
                        )
                    )
                    val inicial = if (modoEdicion) {
                        monedasCache.find { it.codConcepto.toIntOrNull() == CotizacionEnCurso.codMoneda }
                            ?: monedasCache[0]
                    } else {
                        monedasCache[0]
                    }
                    ddlMoneda.setText(inicial.descripcion, false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@NuevaCotizacionActivity,
                    "Error cargando moneda: ${e.message}",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }

        ddlMoneda.setOnItemClickListener { _, _, position, _ ->
            val nuevoCodMoneda = monedasCache.getOrNull(position)?.codConcepto?.toIntOrNull() ?: 0
            CampoError.limpiar(ddlMoneda)

            if (CotizacionEnCurso.productos.isNotEmpty() && nuevoCodMoneda != 0 &&
                nuevoCodMoneda != CotizacionEnCurso.codMoneda) {

                val tc = txtTipoCambio.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

                if (tc <= BigDecimal.ZERO) {

                    val anterior =
                        monedasCache.find { it.codConcepto.toIntOrNull() == CotizacionEnCurso.codMoneda }
                    ddlMoneda.setText(anterior?.descripcion, false)
                    CampoError.mostrar(txtTipoCambio, "TIPO DE CAMBIO INVÁLIDO")
                } else {
                    CotizacionEnCurso.productos.forEach { item ->
                        if(item.monedaBase == 0){
                            item.monedaBase = CotizacionEnCurso.codMoneda
                            item.precioBase = item.precioUnitario
                        }
                        item.precioUnitario =
                            if(nuevoCodMoneda == item.monedaBase) item.precioBase
                        else MonedaHelper.convertirPrecio(item.precioBase, nuevoCodMoneda, tc)

                    }
                    CotizacionEnCurso.codMoneda = nuevoCodMoneda
                }
            }
        }

        val correlativoRepository = CorrelativoRepository(RetrofitClient.correlativoApi)
        val ddlSerie = findViewById<AutoCompleteTextView>(R.id.ddlSerie)
        val txtNumero = findViewById<TextInputEditText>(R.id.txtNumero)


        if (modoEdicion) {

            ddlSerie.setText(CotizacionEnCurso.serie, false)
            txtNumero.setText(CotizacionEnCurso.numero)
            ddlSerie.isEnabled = false
            txtNumero.isEnabled = false
        } else if (tipoDoc != Documentos.COTIZACION.tipoDoc) {
            val documentoRepository = DocumentoRepository(RetrofitClient.documentoApi)
            lifecycleScope.launch {

                val series = try {
                    documentoRepository.listarSeries(
                        tipoDoc, SessionManager.getCodAlmacen(), SessionManager.getCodEmpresa()
                    )
                } catch (e: Exception) {
                    ErrorHelper.mostrar(this@NuevaCotizacionActivity, "Error cargando SERIES", e)
                    return@launch
                }

                if (series.isEmpty()) {
                    CampoError.mostrar(ddlSerie, "NO HAY SERIES ACTIVAS")
                    return@launch
                }

                ddlSerie.setAdapter(
                    ArrayAdapter(
                        this@NuevaCotizacionActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        series.map { it.serieDoc }
                    )
                )
                ddlSerie.setText(series[0].serieDoc, false)


                try {
                    txtNumero.setText(
                        documentoRepository.obtenerNumero(
                            tipoDoc, SessionManager.getCodAlmacen(), SessionManager.getCodEmpresa(),
                            series[0].serieDoc
                        )
                    )
                } catch (e: Exception) {
                    ErrorHelper.mostrar(this@NuevaCotizacionActivity, "Error obteniendo NÚMERO", e)
                }

                ddlSerie.setOnItemClickListener { _, _, position, _ ->
                    CampoError.limpiar(ddlSerie)
                    lifecycleScope.launch {
                        try {
                            txtNumero.setText(
                                documentoRepository.obtenerNumero(
                                    tipoDoc, SessionManager.getCodAlmacen(), SessionManager.getCodEmpresa(),
                                    series[position].serieDoc
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@NuevaCotizacionActivity,
                                "Error obteniendo NÚMERO: ${ErrorHelper.mensaje(e)}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                try {
                    val series = correlativoRepository.listarSeries(
                        SessionManager.getCodAlmacen(),
                        SessionManager.getCodEmpresa()
                    )


                    if (series.isNotEmpty()) {
                        ddlSerie.setAdapter(
                            ArrayAdapter(
                                this@NuevaCotizacionActivity,
                                android.R.layout.simple_spinner_dropdown_item,
                                series.map { it.serieDoc }
                            )
                        )
                        ddlSerie.setText(series[0].serieDoc, false)

                        val numero = correlativoRepository.obtenerNumero(
                            SessionManager.getCodAlmacen(),
                            SessionManager.getCodEmpresa(),
                            series[0].serieDoc
                        )
                        txtNumero.setText(numero)

                        ddlSerie.setOnItemClickListener { _, _, position, _ ->
                            lifecycleScope.launch {
                                val nuevoNumero = correlativoRepository.obtenerNumero(
                                    SessionManager.getCodAlmacen(),
                                    SessionManager.getCodEmpresa(),
                                    series[position].serieDoc
                                )
                                txtNumero.setText(nuevoNumero)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@NuevaCotizacionActivity,
                        "Error cargando serie: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val txtFecha = findViewById<TextInputEditText>(R.id.txtFechaEmision)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
        val hoy = Calendar.getInstance()

        val tipoCambioRepository = TipoCambioRepository(RetrofitClient.tipoCambioApi)

        val sdfServidor = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))

        fun cargarTipoCambio(fecha: java.util.Date) {
            lifecycleScope.launch {
                try {
                    val tc = tipoCambioRepository.obtener(sdfServidor.format(fecha))
                    txtTipoCambio.setText(tc.toPlainString())
                } catch (e: Exception) {
                    Toast.makeText(this@NuevaCotizacionActivity,
                        "Error obteniendo tipo de cambio: ${e.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }



        if (modoEdicion) {
            txtFecha.setText(CotizacionEnCurso.fechaEmision)
            txtTipoCambio.setText(CotizacionEnCurso.tipoCambio.toPlainString())
        } else {
            txtFecha.setText(sdf.format(hoy.time))
            cargarTipoCambio(hoy.time)
        }

        if(config.fechaEditable) txtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    txtFecha.setText(sdf.format(cal.time))
                    cargarTipoCambio(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.show()
        }


        val ddlDireccion = findViewById<AutoCompleteTextView>(R.id.ddlDireccion)
        ddlDireccion.setOnItemClickListener { _, _, position, _ ->
            direccionElegida = direcciones.getOrNull(position)

            CampoError.limpiar(ddlDireccion, findViewById<TextView>(R.id.txtErrorDireccion))
        }
        findViewById<View>(R.id.btnAgregarDireccion).setOnClickListener {
            DireccionesDialog(this, clienteRepository, direcciones) { sel ->
                refrescarDirecciones(sel ?: direccionElegida)
            }.mostrar()
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSiguiente)
            .setOnClickListener {
                val avisoDireccion = findViewById<TextView>(R.id.txtErrorDireccion)

                val avisoCliente = findViewById<TextView>(R.id.txtErrorCliente)

                CampoError.limpiar(txtBuscarCliente, avisoCliente)
                CampoError.limpiar(ddlSerie)
                CampoError.limpiar(txtNumero)
                CampoError.limpiar(txtFecha)
                CampoError.limpiar(ddlMoneda)
                CampoError.limpiar(txtTipoCambio)
                CampoError.limpiar(txtDistritoNuevo, findViewById<TextView>(R.id.txtErrorDistrito))
                CampoError.limpiar(ddlDireccion, avisoDireccion)

                var primerError: View? = null
                fun marcar(campo: View, mensaje: String, aviso: TextView? = null) {
                    CampoError.mostrar(campo, mensaje, aviso)
                    if (primerError == null) primerError = campo
                }

                val cliente = clienteSeleccionado
                val tc = txtTipoCambio.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO


                val direccionOk = if (direccionElegida != null) true else cliente?.direccion?.isNotBlank() == true

                if (cliente == null || cliente.ruc.isBlank() || cliente.razonSocial.isBlank() || (config.exigeDireccion && !direccionOk)) {
                    marcar(txtBuscarCliente, "SELECCIONA UN CLIENTE DE LA LISTA", avisoCliente)
                } else if (config.soloRuc && (cliente.ruc.trim().length != 11 || cliente.ruc.trim() == "11111111")) {
                    marcar(txtBuscarCliente, "LAS FACTURAS SOLO SE PUEDEN CREAR CON NRO. DE RUC", avisoCliente)
                } else if (config.soloRuc && !ValidacionHelper.rucValido(cliente.ruc)) {
                    marcar(txtBuscarCliente, "RUC INVÁLIDO", avisoCliente)
                }

                if (cliente != null && pedirDistrito && distritoElegido == null) {
                    marcar(txtDistritoNuevo, "ELIGE EL DISTRITO DEL CLIENTE",
                        findViewById<TextView>(R.id.txtErrorDistrito))
                }

                if (cliente != null && cliente.codCtaCte != "0" && direccionElegida == null) {
                    marcar(ddlDireccion, "ELIGE O AGREGA UNA DIRECCIÓN CON EL +", avisoDireccion)
                }

                if (ddlSerie.text.toString().isBlank()) {
                    marcar(ddlSerie, "SELECCIONA UNA SERIE")

                }

                if (txtNumero.text.toString().isBlank()) {
                    marcar(txtNumero, "NÚMERO NO DISPONIBLE")

                }


                if (txtFecha.text.toString().isBlank()) {
                    marcar(txtFecha, "SELECCIONA LA FECHA DE EMISIÓN")
                }


                val codMonedaSeleccionada = monedasCache.find { it.descripcion ==
                ddlMoneda.text.toString()} ?.codConcepto?.toIntOrNull() ?: 0

                if(ddlMoneda.text.toString().isBlank()){
                    marcar(ddlMoneda, "SELECCIONA UNA MONEDA")
                }else if(codMonedaSeleccionada == 0){
                    marcar(ddlMoneda, "MONEDA NO VALIDA")

                }

                if (tc <= BigDecimal.ZERO) {
                    marcar(txtTipoCambio, "TIPO DE CAMBIO INVALIDO")

                }


                val destino =primerError
                if(destino != null){
                    CampoError.irA(destino)
                    return@setOnClickListener
                }

                val d = distritoElegido
                val dir = direccionElegida
                CotizacionEnCurso.cliente = when {
                    pedirDistrito && d != null -> cliente?.copy(
                        direccion = findViewById<TextInputEditText>(R.id.txtDireccionNueva).text.toString().trim(),
                        codDepartamento = d.codDepartamento,
                        codProvincia = d.codProvincia,
                        codDistrito = d.codDistrito
                    )
                    dir != null -> cliente?.copy(
                        direccion = dir.direccion,
                        codDireccion = dir.codDireccion,
                        codDepartamento = dir.codDepartamento,
                        codProvincia = dir.codProvincia,
                        codDistrito = dir.codDistrito
                    )
                    else -> cliente
                }

                CotizacionEnCurso.direccionEtiqueta = when {
                    pedirDistrito && d != null -> listOf(
                        findViewById<TextInputEditText>(R.id.txtDireccionNueva).text.toString().trim(),
                        d.descripcion
                    ).filter { it.isNotBlank() }.joinToString(" - ")
                    dir != null -> dir.etiqueta
                    else -> ""
                }

                CotizacionEnCurso.serie = ddlSerie.text.toString()
                CotizacionEnCurso.numero = txtNumero.text.toString()
                CotizacionEnCurso.fechaEmision = txtFecha.text.toString()

                val monedaSeleccionada = monedasCache.find { it.descripcion == ddlMoneda.text.toString() }
                CotizacionEnCurso.codMoneda = monedaSeleccionada?.codConcepto?.toIntOrNull() ?: 0
                CotizacionEnCurso.moneda = monedaSeleccionada?.descripcion ?: ""

                CotizacionEnCurso.tipoCambio = tc
                startActivity(Intent(this, AgregarProductosActivity::class.java))
            }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelar)
            .setOnClickListener {
                CotizacionEnCurso.limpiar()
            finish()
            }
    }
    private fun nombreLimpio(c: Cliente): String =
        c.razonSocial.removePrefix(c.ruc).trim().removePrefix("-").trim()

    private fun mostrarPanelClienteNuevo(mostrar: Boolean) {
        distritoElegido = null
        findViewById<TextInputEditText>(R.id.txtDistritoNuevo).setText("")
        findViewById<TextInputEditText>(R.id.txtDireccionNueva).setText("")
        findViewById<View>(R.id.layoutClienteNuevo).visibility =
            if (mostrar) View.VISIBLE else View.GONE
    }

    private fun ocultarDirecciones() {
        jobDirecciones?.cancel()
        direcciones.clear()
        direccionElegida = null
        findViewById<View>(R.id.layoutDirecciones).visibility = View.GONE
        CampoError.limpiar(
            findViewById<AutoCompleteTextView>(R.id.ddlDireccion),
            findViewById<TextView>(R.id.txtErrorDireccion)
        )
    }

    private fun cargarDireccionDeApi(cliente: Cliente) {
        jobDirecciones?.cancel()
        direcciones.clear()
        val item = DireccionItem(
            codDireccion = "0",
            principal = true,
            codDepartamento = cliente.codDepartamento,
            codProvincia = cliente.codProvincia,
            codDistrito = cliente.codDistrito,
            direccion = cliente.direccion,
            distrito = cliente.distrito
        )
        direcciones.add(item)
        findViewById<View>(R.id.layoutDirecciones).visibility = View.VISIBLE
        refrescarDirecciones(item)
    }
    private fun cargarDirecciones(cliente: Cliente) {
        jobDirecciones?.cancel()
        direcciones.clear()
        direccionElegida = null
        findViewById<View>(R.id.layoutDirecciones).visibility = View.VISIBLE
        refrescarDirecciones(null)

        jobDirecciones = lifecycleScope.launch {
            try {
                val lista = clienteRepository.listarDirecciones(cliente.codCtaCte)
                direcciones.addAll(lista)
                val inicial = lista.find { it.codDireccion == cliente.codDireccion }
                    ?: lista.find { it.principal }
                    ?: lista.firstOrNull()
                refrescarDirecciones(inicial)
            } catch (e: Exception) {
                Toast.makeText(
                    this@NuevaCotizacionActivity,
                    "Error cargando direcciones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun refrescarDirecciones(preferida: DireccionItem?) {
        val ddl = findViewById<AutoCompleteTextView>(R.id.ddlDireccion)
        direccionElegida = preferida?.takeIf { direcciones.contains(it) }
            ?: direcciones.find { it.principal }
                    ?: direcciones.firstOrNull()

        ddl.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, direcciones.map { it.etiqueta })
        )
        ddl.setText(direccionElegida?.etiqueta ?: "", false)

        CampoError.limpiar(ddl, findViewById<TextView>(R.id.txtErrorDireccion))
    }

    private fun setDatoCliente(includeId: Int, label: String, valor: String) {
        val row = findViewById<android.view.View>(includeId)
        row.findViewById<TextView>(R.id.txtLabelDato).text = label
        val txtValor = row.findViewById<TextView>(R.id.txtValorDato)
        txtValor.text = valor
        txtValor.setTextColor(
            if (valor == "-") android.graphics.Color.parseColor("#B5433E")
            else android.graphics.Color.parseColor("#14213D")
        )
    }
}