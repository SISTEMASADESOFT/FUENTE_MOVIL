package com.maestros.familias.ui.ventas

import android.app.AlertDialog
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
import com.maestros.familias.data.model.Moneda
import com.maestros.familias.data.repository.ClienteRepository
import com.maestros.familias.data.repository.CorrelativoRepository
import com.maestros.familias.data.repository.FormaPagoRepository
import com.maestros.familias.data.repository.TipoCambioRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.MonedaHelper
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_cotizacion)

        setDatoCliente(R.id.rowRuc, "RUC", "-")
        setDatoCliente(R.id.rowRazonSocial, "Razon Social", "-")
        setDatoCliente(R.id.rowDireccion, "Direccion", "-")

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val txtBuscarCliente = findViewById<TextInputEditText>(R.id.txtBuscarCliente)
        val listPopup = android.widget.ListPopupWindow(this).apply {
            anchorView = txtBuscarCliente
            setOnItemClickListener { _, _, position, _ ->
                val cliente = ultimosResultados.getOrNull(position) ?: return@setOnItemClickListener
                clienteSeleccionado = cliente
                setDatoCliente(R.id.rowRuc, "RUC", cliente.ruc)
                setDatoCliente(R.id.rowRazonSocial, "Razón social", cliente.razonSocial)
                setDatoCliente(R.id.rowDireccion, "Dirección", cliente.direccion)
                txtBuscarCliente.setText(cliente.razonSocial)
                dismiss()
            }
        }

        txtBuscarCliente.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                //si el usuario selecciona un cliente y luego edita el texto, invalidamos la selección
                if (clienteSeleccionado != null && s?.toString() != clienteSeleccionado?.razonSocial) {
                    clienteSeleccionado = null
                    setDatoCliente(R.id.rowRuc, "RUC", "-")
                    setDatoCliente(R.id.rowRazonSocial, "Razón social", "-")
                    setDatoCliente(R.id.rowDireccion, "Dirección", "-")
                }

                val texto = s?.toString()?.trim() ?: ""
                jobBusqueda?.cancel()

                if (texto.length < 3) {
                    listPopup.dismiss()
                    return
                }

                jobBusqueda = lifecycleScope.launch {
                    kotlinx.coroutines.delay(400) // debounce
                    try {
                        val resultados = clienteRepository.buscar(texto)
                        ultimosResultados = resultados

                        //solo se muestran clientes que YA existen
                        if (resultados.isEmpty()) {
                            listPopup.dismiss()
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
                        android.widget.Toast.makeText(
                            this@NuevaCotizacionActivity,
                            "Error buscando cliente: ${e.message}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
                    ddlMoneda.setText(monedasCache[0].descripcion, false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@NuevaCotizacionActivity,
                    "Error cargando moneda: ${e.message}",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }

        ddlMoneda.setOnItemClickListener { _, _, position, _ ->
            val nuevaMoneda = monedasCache.getOrNull(position)
            val nuevoCodMoneda = nuevaMoneda?.codConcepto?.toIntOrNull() ?: 0

            if (CotizacionEnCurso.productos.isNotEmpty() && nuevoCodMoneda != CotizacionEnCurso.codMoneda) {

                val monedaAnteriorTexto = if (CotizacionEnCurso.codMoneda == 2) "Dólares" else "Soles"
                val monedaNuevaTexto = if (nuevoCodMoneda == 2) "Dólares" else "Soles"

                val vibrator = getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
                if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                }else{
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }

                val dialogView = layoutInflater.inflate(R.layout.dialog_advertencia_moneda, null)
                dialogView.findViewById<TextView>(R.id.txtMensajeAdvertencia).text =
                    "Ya agregaste ${CotizacionEnCurso.productos.size} producto(s) con precios" +
                            " en $monedaAnteriorTexto.\n\nAl continuar los precios " +
                            "se recalcularan automaticamente a $monedaNuevaTexto usando" +
                            "el tipo de cambio actual (${txtTipoCambio.text}). Revisa los montos" +
                            "en la siguiente pantalla."


                AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton("Entendido, actualizar precios") { _, _ ->
                        val tc = txtTipoCambio.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO
                        CotizacionEnCurso.productos.forEach { item ->
                            item.precioUnitario = MonedaHelper.convertirPrecio(item.precioUnitario, nuevoCodMoneda, tc)
                        }
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        val monedaAnterior = monedasCache.find { it.codConcepto.toIntOrNull() == CotizacionEnCurso.codMoneda }
                        ddlMoneda.setText(monedaAnterior?.descripcion, false)
                    }
                    .show()
            }
        }

        val correlativoRepository = CorrelativoRepository(RetrofitClient.correlativoApi)
        val ddlSerie = findViewById<AutoCompleteTextView>(R.id.ddlSerie)
        val txtNumero = findViewById<TextInputEditText>(R.id.txtNumero)



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
                Toast.makeText(this@NuevaCotizacionActivity,
                    "Error cargando serie: ${e.message}",
                    Toast.LENGTH_SHORT).show()
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



        //fecha inicial
        txtFecha.setText(sdf.format(hoy.time))

        txtFecha.setOnClickListener {
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

        //tipo de cambio inicial, para hoy
        cargarTipoCambio(hoy.time)




        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSiguiente)
            .setOnClickListener {
                val tilSerie = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilSerie)
                val tilNumero = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilNumero)
                val tilMoneda = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMoneda)
                val tilTipoCambio = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilTipoCambio)
                val txtErrorCliente = findViewById<TextView>(R.id.txtErrorCliente)

                //limpiar errores previos
                tilSerie.error = null
                tilNumero.error = null
                tilMoneda.error = null
                tilTipoCambio.error = null
                txtErrorCliente.visibility = View.GONE

                var valido = true
                val cliente = clienteSeleccionado
                val tc = txtTipoCambio.text.toString().toBigDecimalOrNull() ?: BigDecimal.ZERO

                if (ddlSerie.text.toString().isBlank()) {
                    tilSerie.error = "Selecciona una serie"
                    valido = false
                }

                if (txtNumero.text.toString().isBlank()) {
                    tilNumero.error = "Número no disponible"
                    valido = false
                }

                if (ddlMoneda.text.toString().isBlank()) {
                    tilMoneda.error = "Selecciona una moneda"
                    valido = false
                }

                val codMonedaSeleccionada = monedasCache.find { it.descripcion ==
                ddlMoneda.text.toString()} ?.codConcepto?.toIntOrNull() ?: 0
                if(codMonedaSeleccionada == 0){
                    tilMoneda.error = "Moneda no valida"
                    valido = false
                }

                if (tc <= BigDecimal.ZERO) {
                    tilTipoCambio.error = "Tipo de cambio inválido"
                    valido = false
                }

                if (cliente == null || cliente.ruc.isBlank() || cliente.razonSocial.isBlank() || cliente.direccion.isBlank()) {
                    txtErrorCliente.visibility = View.VISIBLE
                    valido = false
                }

                if (!valido) return@setOnClickListener

                CotizacionEnCurso.cliente = cliente
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