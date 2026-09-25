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
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.CotizacionGrabarRequest
import com.maestros.familias.data.model.FormaPago
import com.maestros.familias.data.repository.CotizacionRepository
import com.maestros.familias.data.repository.FormaPagoRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.MonedaHelper
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class ConfirmarEmitirActivity : AppCompatActivity() {


    private val igvPorcentaje = CotizacionEnCurso.igvPorcentaje

    private var formasPagoCache: List<FormaPago> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmar_emitir)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            cancelarCotizacion()
        }

        val cliente = CotizacionEnCurso.cliente
        findViewById<TextView>(R.id.txtRazonSocialConfirm).text = cliente?.razonSocial ?: "-"
        findViewById<TextView>(R.id.txtRucConfirm).text = "RUC: ${cliente?.ruc ?: "-"}"
        findViewById<TextView>(R.id.txtDireccionConfirm).text = cliente?.direccion ?: "-"

        setDato(R.id.rowTipoComprobante, "Tipo Documento", "Cotización")
        setDato(R.id.rowSerieNumero, "Serie - Número:", "${CotizacionEnCurso.serie} - ${CotizacionEnCurso.numero}")
        setDato(R.id.rowFechaEmisionConfirm, "Emisión:", CotizacionEnCurso.fechaEmision)
        setDato(R.id.rowMonedaConfirm, "Moneda:", CotizacionEnCurso.moneda)

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
                val formasPago = FormaPagoRepository(RetrofitClient.formaPagoApi).listar()

                formasPagoCache=formasPago

                if (formasPago.isNotEmpty()) {
                    ddlFormaPago.setAdapter(
                        ArrayAdapter(
                            this@ConfirmarEmitirActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            formasPago.map { it.descripcion }
                        )
                    )
                    val yaElegida = formasPago.find { it.codConcepto == CotizacionEnCurso.codFormaPago }
                    ddlFormaPago.setText(yaElegida?.descripcion?: formasPago[0].descripcion, false)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ConfirmarEmitirActivity,
                    "Error cargando forma de pago: ${e.message}",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }

        ddlFormaPago.setOnItemClickListener { _, _, position, _ ->
            val elegida = formasPagoCache.getOrNull(position)
            if (elegida != null) {
                CotizacionEnCurso.codFormaPago = elegida.codConcepto
                CotizacionEnCurso.formaPago = elegida.descripcion
            }
        }


        findViewById<MaterialButton>(R.id.btnEditarCliente).setOnClickListener {
            val intent = Intent(this, NuevaCotizacionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        val total = CotizacionEnCurso.productos.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
        val divisor = BigDecimal.ONE.add(igvPorcentaje)
        val subtotal = total.divide(divisor, 2, java.math.RoundingMode.HALF_UP)
        val igv = total.subtract(subtotal)

        findViewById<TextView>(R.id.txtSubtotalConfirm).text = MonedaHelper.formatear(subtotal, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtIgvConfirm).text = MonedaHelper.formatear(igv, CotizacionEnCurso.codMoneda)
        findViewById<TextView>(R.id.txtTotalConfirm).text = MonedaHelper.formatear(total, CotizacionEnCurso.codMoneda)

        findViewById<MaterialButton>(R.id.btnAtrasConfirmar).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnGrabar).setOnClickListener {
            val cliente = CotizacionEnCurso.cliente

            if (cliente == null) {
                Toast.makeText(this, "Selecciona un cliente", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (CotizacionEnCurso.productos.isEmpty()) {
                Toast.makeText(this, "No hay productos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ddlFormaPago = findViewById<AutoCompleteTextView>(R.id.ddlFormaPago)

            val formaPagoSeleccionada =
                formasPagoCache.find {
                    it.descripcion == ddlFormaPago.text.toString()
                }

            val codFormaPago =
                formaPagoSeleccionada?.codConcepto?.toIntOrNull() ?: 0

            val detalle = CotizacionEnCurso.productos.map {
                mapOf(
                    "CodProducto" to it.producto.codProducto,
                    "Cantidad" to it.cantidad,
                    "Precio" to it.precioUnitario.toPlainString(),
                    "Descripcion" to it.descripcion
                )
            }

            val detalleJson = com.google.gson.Gson().toJson(detalle)

            val total = CotizacionEnCurso.productos.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
            val divisor = BigDecimal.ONE.add(igvPorcentaje)
            val subtotal = total.divide(divisor, 2, java.math.RoundingMode.HALF_UP)
            val igv = total.subtract(subtotal)
            val tasaIgv = BigDecimal.ONE.add(igvPorcentaje)

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

            lifecycleScope.launch {
                try {
                    val (exito, mensaje, codigo) =
                        CotizacionRepository(RetrofitClient.cotizacionApi).grabar(request)

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
                        Toast.makeText(
                            this@ConfirmarEmitirActivity,
                            "Error: $mensaje",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ConfirmarEmitirActivity,
                        "Error al grabar: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


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