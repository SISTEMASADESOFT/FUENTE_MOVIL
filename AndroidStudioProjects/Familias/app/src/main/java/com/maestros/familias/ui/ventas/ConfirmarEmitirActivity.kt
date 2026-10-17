package com.maestros.familias.ui.ventas

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.CotizacionEditarRequest
import com.maestros.familias.data.model.CotizacionGrabarRequest
import com.maestros.familias.data.model.FacturaGrabarRequest
import com.maestros.familias.data.model.FormaPago
import com.maestros.familias.data.model.GuiaDatos
import com.maestros.familias.data.repository.CotizacionEdicionRepository
import com.maestros.familias.data.repository.CotizacionRepository
import com.maestros.familias.data.repository.FormaPagoRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.DocumentoConfig
import com.maestros.familias.data.session.Documentos
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.CampoError
import com.maestros.familias.data.util.MonedaHelper
import com.maestros.familias.data.util.VencimientoHelper
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ConfirmarEmitirActivity : AppCompatActivity() {

    private val igvPorcentaje = CotizacionEnCurso.igvPorcentaje

    private var formasPagoCache: List<FormaPago> = emptyList()

    private lateinit var config: DocumentoConfig
    private lateinit var guia:GuiaFormulario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmar_emitir)

        config = Documentos.porTipo(CotizacionEnCurso.tipoDoc)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            cancelarCotizacion()
        }

        val cliente = CotizacionEnCurso.cliente
        val etiquetaDoc = if ((cliente?.ruc?.length ?: 0) == 8) "DNI" else "RUC"

        findViewById<TextView>(R.id.txtRazonSocialConfirm).text = cliente?.razonSocial ?: "-"
        findViewById<TextView>(R.id.txtRucConfirm).text = "$etiquetaDoc: ${cliente?.ruc ?: "-"}"
        findViewById<TextView>(R.id.txtDireccionConfirm).text =
            CotizacionEnCurso.direccionEtiqueta.ifBlank {
                cliente?.direccion?.ifBlank { "(sin direccion)" } ?: "-"
            }

        setDato(R.id.rowTipoComprobante, "Tipo Documento", config.nombre)
        setDato(R.id.rowSerieNumero,
            "Serie - Número:", "${CotizacionEnCurso.serie} - ${CotizacionEnCurso.numero}")
        setDato(R.id.rowFechaEmisionConfirm, "Emisión:", CotizacionEnCurso.fechaEmision)
        setDato(R.id.rowMonedaConfirm, "Moneda:", CotizacionEnCurso.moneda)

        findViewById<View>(R.id.rowVencimientoConfirm).visibility =
            if (config.esVenta) View.VISIBLE else View.GONE
        if (config.esVenta) setDato(R.id.rowVencimientoConfirm, "Vencimiento:", CotizacionEnCurso.fechaEmision)

        val rv = findViewById<RecyclerView>(R.id.rvProductosConfirmar)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ProductoSeleccionadoAdapter(
            CotizacionEnCurso.productos,
            CotizacionEnCurso.codMoneda,
            onEliminar = {},
            onEditar = {},
            soloLectura = true
        )
        val ddlFormaPago = findViewById<AutoCompleteTextView>(R.id.ddlFormaPago)

        lifecycleScope.launch {
            try {
                var formasPago = FormaPagoRepository(RetrofitClient.formaPagoApi).listar()

                formasPago = formasPago.filter { it.codConcepto !in config.formasPagoExcluidas }

                formasPagoCache = formasPago

                if (formasPago.isNotEmpty()) {
                    ddlFormaPago.setAdapter(
                        ArrayAdapter(
                            this@ConfirmarEmitirActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            formasPago.map { it.descripcion }
                        )
                    )
                    val porDefecto = when {
                        CotizacionEnCurso.codFormaPago.isNotBlank() -> CotizacionEnCurso.codFormaPago
                        else -> config.formaPagoPorDefecto
                    }
                    val inicial = formasPago.find { it.codConcepto == porDefecto } ?: formasPago[0]
                    ddlFormaPago.setText(inicial.descripcion, false)
                    aplicarFormaPago(inicial)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ConfirmarEmitirActivity,
                    "Error cargando forma de pago: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        ddlFormaPago.setOnItemClickListener { _, _, position, _ ->
            CampoError.limpiar(ddlFormaPago)

            formasPagoCache.getOrNull(position)?.let { aplicarFormaPago(it) }
        }


        val raizGuia = findViewById<View>(R.id.layoutGuia)
        raizGuia.visibility = if (config.usaGuia) View.VISIBLE else View.GONE
        guia = GuiaFormulario(this, raizGuia)
        if (config.usaGuia) guia.iniciar()


        findViewById<MaterialButton>(R.id.btnEditarCliente).setOnClickListener {
            val intent = Intent(this, NuevaCotizacionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("TIPO_DOC", CotizacionEnCurso.tipoDoc)
                putExtra("EDITAR", CotizacionEnCurso.esEdicion)
            }
            startActivity(intent)
        }

        val total = CotizacionEnCurso.productos.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
        val divisor = BigDecimal.ONE.add(igvPorcentaje)
        val subtotal = total.divide(divisor, 2, java.math.RoundingMode.HALF_UP)
        val igv = total.subtract(subtotal)

        findViewById<TextView>(R.id.txtSubtotalConfirm).text =
            MonedaHelper.formatear(subtotal, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtIgvConfirm).text =
            MonedaHelper.formatear(igv, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtTotalConfirm).text =
            MonedaHelper.formatear(total, CotizacionEnCurso.codMoneda)

        findViewById<MaterialButton>(R.id.btnAtrasConfirmar).setOnClickListener { finish() }

        val btnGrabar = findViewById<MaterialButton>(R.id.btnGrabar)
        if (CotizacionEnCurso.esEdicion) btnGrabar.text = "Guardar cambios"
        else config.textoEmitir?.let { btnGrabar.text = it }

        btnGrabar.setOnClickListener {
            val cliente = CotizacionEnCurso.cliente

            if (cliente == null) {
                Toast.makeText(this, "SELECCIONA UN CLIENTE", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (CotizacionEnCurso.productos.isEmpty()) {
                Toast.makeText(this, "NO HAY PRODUCTOS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ddlFormaPagoSel = findViewById<AutoCompleteTextView>(R.id.ddlFormaPago)
            val formaPagoSeleccionada =
                formasPagoCache.find { it.descripcion == ddlFormaPagoSel.text.toString() }

            CampoError.limpiar(ddlFormaPagoSel)
            if (formaPagoSeleccionada == null) {
                CampoError.mostrar(ddlFormaPagoSel, "SELECCIONA UNA FORMA DE PAGO")
                return@setOnClickListener
            }
            val codFormaPago = formaPagoSeleccionada.codConcepto.toIntOrNull() ?: 0

            var datosGuia: GuiaDatos? = null

            if (config.esVenta) {
                val txtNroOperacion = findViewById<TextInputEditText>(R.id.txtNroOperacion)
                CampoError.limpiar(txtNroOperacion)

                if (codFormaPago == 6 && txtNroOperacion.text.toString().trim().isEmpty()) {
                    CampoError.mostrar(txtNroOperacion, "INGRESA EL NÚMERO DE OPERACIÓN")
                    return@setOnClickListener
                }
                CotizacionEnCurso.nroOperacion = txtNroOperacion.text.toString().trim()

                // Por ahora solo clientes que ya existen (cliente nuevo = siguiente etapa)
                if ((cliente.codCtaCte.toIntOrNull() ?: 0) == 0) {
                    Toast.makeText(
                        this,
                        "POR AHORA SOLO SE PUEDE EMITIR A CLIENTES EXISTENTES",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                if (config.usaGuia && guia.activa) {
                    if (!guia.validar()) return@setOnClickListener
                    datosGuia = guia.datos()
                }
            }

            val detalle = CotizacionEnCurso.productos.map {
                mapOf(
                    "CodProducto" to it.producto.codProducto,
                    "Cantidad" to it.cantidad,
                    "Precio" to it.precioUnitario.toPlainString(),
                    "Descripcion" to it.descripcion
                )
            }
            val detalleJson = com.google.gson.Gson().toJson(detalle)

            val total =
                CotizacionEnCurso.productos.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
            val divisor = BigDecimal.ONE.add(igvPorcentaje)
            val subtotal = total.divide(divisor, 2, java.math.RoundingMode.HALF_UP)
            val igv = total.subtract(subtotal)
            val tasaIgv = BigDecimal.ONE.add(igvPorcentaje)

            lifecycleScope.launch {
                btnGrabar.isEnabled = false
                try {
                    var exito = false
                    var mensaje = ""
                    var codigo = 0

                    if (config.esVenta) {
                        val g = datosGuia
                        val requestFactura = FacturaGrabarRequest(
                            DetalleJson = detalleJson,
                            CodCliente = cliente.codCtaCte.toIntOrNull() ?: 0,
                            SerieDoc = CotizacionEnCurso.serie,
                            NumeroDoc = CotizacionEnCurso.numero,
                            FechaEmision = convertirFechaParaServidor(CotizacionEnCurso.fechaEmision),
                            CodMoneda = CotizacionEnCurso.codMoneda,
                            TipoCambio = CotizacionEnCurso.tipoCambio,
                            CodTasa = CotizacionEnCurso.codTasa.toIntOrNull() ?: 0,
                            TasaIgv = tasaIgv,
                            CodFormaPago = codFormaPago,
                            SubTotal = subtotal,
                            Igv = igv,
                            Total = total,
                            CodAlmacen = SessionManager.getCodAlmacen(),
                            CodEmpresa = SessionManager.getCodEmpresa(),
                            CodUsuario = SessionManager.getCodUsuario(),
                            CodEmpleado = SessionManager.getCodEmpleado(),
                            NroRuc = cliente.ruc,
                            RazonSocial = cliente.razonSocial,
                            NroOperacion = CotizacionEnCurso.nroOperacion,
                            // ---- guía
                            FlagGuia = if (g != null) 1 else 0,
                            SerieGuia = g?.serie ?: "",
                            NumeroGuia = g?.numero ?: "",
                            CodTipoTransportista = g?.codTipoTransportista ?: 0,
                            FechaTraslado = if (g != null) convertirFechaParaServidor(g.fechaTraslado) else "",
                            CodTransportista = g?.transportista?.codCtaCte ?: 0,
                            NroRucTransportista = g?.transportista?.ruc ?: "",
                            RazonSocialTransportista = g?.transportista?.nombre ?: "",
                            PlacaTraslado = g?.placa ?: "",
                            MarcaVehiculo = g?.marca ?: "",
                            Licencia = g?.licencia ?: "",
                            NroBultos = g?.bultos ?: 0,
                            Peso = g?.peso ?: BigDecimal.ZERO,
                            CodUnidadPeso = g?.codUnidadPeso ?: 0,
                            CodConductor = g?.conductor?.codCtaCte ?: 0,
                            ObservacionGuia = g?.observacion ?: ""
                        )
                        val (ok, msg, cod) = CotizacionRepository(RetrofitClient.cotizacionApi)
                            .grabarFactura(requestFactura)
                        exito = ok; mensaje = msg; codigo = cod


                        if (ok) CotizacionEnCurso.guiaEmitida =
                            if (g != null) "${g.serie} - ${g.numero}" else ""
                    } else if (CotizacionEnCurso.esEdicion) {
                        val codEmpleado = if (CotizacionEnCurso.codEmpleado > 0)
                            CotizacionEnCurso.codEmpleado else SessionManager.getCodEmpleado()

                        val requestEdicion = CotizacionEditarRequest(
                            DetalleJson = detalleJson,
                            CodProformaAnterior = CotizacionEnCurso.codProformaEdicion,
                            CodCliente = cliente.codCtaCte.toIntOrNull() ?: 0,
                            SerieDoc = CotizacionEnCurso.serie,
                            NumeroDoc = CotizacionEnCurso.numero,
                            FechaEmision = convertirFechaParaServidor(CotizacionEnCurso.fechaEmision),
                            CodMoneda = CotizacionEnCurso.codMoneda,
                            TipoCambio = CotizacionEnCurso.tipoCambio,
                            CodTasa = CotizacionEnCurso.codTasa.toIntOrNull() ?: 0,
                            TasaIgv = tasaIgv,
                            CodFormaPago = codFormaPago,
                            SubTotal = subtotal,
                            Igv = igv,
                            Total = total,
                            CodAlmacen = SessionManager.getCodAlmacen(),
                            CodEmpresa = SessionManager.getCodEmpresa(),
                            CodUsuario = SessionManager.getCodUsuario(),
                            CodEmpleado = codEmpleado,
                            NroRuc = cliente.ruc,
                            RazonSocial = cliente.razonSocial,
                            Direccion = cliente.direccion,
                            CodDepartamento = cliente.codDepartamento.toIntOrNull() ?: 0,
                            CodProvincia = cliente.codProvincia.toIntOrNull() ?: 0,
                            CodDistrito = cliente.codDistrito.toIntOrNull() ?: 0,
                            CodDireccion = cliente.codDireccion.toIntOrNull() ?: 0,
                            Placa = CotizacionEnCurso.placa,
                            KM = CotizacionEnCurso.km,
                            Observacion = CotizacionEnCurso.observacion,
                            Atencion = CotizacionEnCurso.atencion,
                            Referencia = CotizacionEnCurso.referencia,
                            NroOperacion = CotizacionEnCurso.nroOperacion,
                            FlagComisionable = CotizacionEnCurso.flagComisionable,
                            FlagConCodigo = CotizacionEnCurso.flagConCodigo
                        )
                        val (ok, msg, cod) = CotizacionEdicionRepository(RetrofitClient.cotizacionApi)
                            .editar(requestEdicion)
                        exito = ok; mensaje = msg; codigo = cod
                    } else {
                        val request = CotizacionGrabarRequest(
                            DetalleJson = detalleJson,
                            CodCliente = cliente.codCtaCte.toIntOrNull() ?: 0,
                            SerieDoc = CotizacionEnCurso.serie,
                            NumeroDoc = CotizacionEnCurso.numero,
                            FechaEmision = convertirFechaParaServidor(CotizacionEnCurso.fechaEmision),
                            CodMoneda = CotizacionEnCurso.codMoneda,
                            TipoCambio = CotizacionEnCurso.tipoCambio,
                            CodTasa = CotizacionEnCurso.codTasa.toIntOrNull() ?: 0,
                            TasaIgv = tasaIgv,
                            CodFormaPago = codFormaPago,
                            SubTotal = subtotal,
                            Igv = igv,
                            Total = total,
                            CodAlmacen = SessionManager.getCodAlmacen(),
                            CodEmpresa = SessionManager.getCodEmpresa(),
                            CodUsuario = SessionManager.getCodUsuario(),
                            CodEmpleado = SessionManager.getCodEmpleado(),
                            NroRuc = cliente.ruc,
                            RazonSocial = cliente.razonSocial,
                            Direccion = cliente.direccion,
                            CodDepartamento = cliente.codDepartamento.toIntOrNull() ?: 0,
                            CodProvincia = cliente.codProvincia.toIntOrNull() ?: 0,
                            CodDistrito = cliente.codDistrito.toIntOrNull() ?: 0,
                            CodDireccion = cliente.codDireccion.toIntOrNull() ?: 0
                        )
                        val (ok, msg, cod) = CotizacionRepository(RetrofitClient.cotizacionApi).grabar(
                            request
                        )
                        exito = ok; mensaje = msg; codigo = cod
                    }

                    if (exito) {

                        CotizacionEnCurso.codProformaGrabada = codigo
                        startActivity(
                            Intent(
                                this@ConfirmarEmitirActivity,
                                ComprobanteEmitidoActivity::class.java
                            )
                        )
                        finish()
                    } else {
                        btnGrabar.isEnabled = true
                        Toast.makeText(
                            this@ConfirmarEmitirActivity,
                            "Error: $mensaje",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    btnGrabar.isEnabled = true
                    Toast.makeText(
                        this@ConfirmarEmitirActivity,
                        "Error al grabar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun aplicarFormaPago(fp: FormaPago) {
        CotizacionEnCurso.codFormaPago = fp.codConcepto
        CotizacionEnCurso.formaPago = fp.descripcion
        val cod = fp.codConcepto.toIntOrNull() ?: 0

        val venc = VencimientoHelper.calcular(CotizacionEnCurso.fechaEmision, cod)
        CotizacionEnCurso.fechaVencimiento = venc
        if (config.esVenta) setDato(R.id.rowVencimientoConfirm, "Vencimiento:", venc)

        val pideOperacion = config.esVenta && cod == 6
        findViewById<TextInputLayout>(R.id.tilNroOperacion).visibility =
            if (pideOperacion) View.VISIBLE else View.GONE
        if (!pideOperacion) CampoError.limpiar(findViewById<TextInputEditText>(R.id.txtNroOperacion))
    }

    private fun mostrarDocumentoEmitido(nombre: String, numero: String) {
        AlertDialog.Builder(this)
            .setTitle("$nombre emitida")
            .setMessage("Se emitió: $numero")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ ->
                CotizacionEnCurso.limpiar()
                val intent = Intent(this, InicioVentasActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun cancelarCotizacion() {
        AlertDialog.Builder(this)
            .setTitle("Cancelar ${config.nombre}")
            .setMessage(
                "¿Deseas cancelar este documento? " +
                        "Se perderán los datos y productos ingresados."
            )
            .setNegativeButton("No", null)
            .setPositiveButton("Sí, cancelar") { _, _ ->

                CotizacionEnCurso.limpiar()

                val intent = Intent(this, InicioVentasActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun convertirFechaParaServidor(fechaDdMmYyyy: String): String {
        val partes = fechaDdMmYyyy.split("/")

        if (partes.size != 3) return fechaDdMmYyyy

        return "${partes[2]}-${partes[1]}-${partes[0]}"
    }

    private fun setDato(includeId: Int, label: String, valor: String) {
        val row = findViewById<View>(includeId)
        row.findViewById<TextView>(R.id.txtLabelDato).text = label
        row.findViewById<TextView>(R.id.txtValorDato).text = valor
    }
}