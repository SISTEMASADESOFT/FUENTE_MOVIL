package com.maestros.familias.ui.ventas


import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.maestros.familias.R
import com.maestros.familias.data.api.RetrofitClient
import com.maestros.familias.data.model.GuiaDatos
import com.maestros.familias.data.model.ItemCatalogo
import com.maestros.familias.data.model.PersonaGuia
import com.maestros.familias.data.repository.DocumentoRepository
import com.maestros.familias.data.repository.GuiaRepository
import com.maestros.familias.data.session.CotizacionEnCurso
import com.maestros.familias.data.session.SessionManager
import com.maestros.familias.data.util.CampoError
import com.maestros.familias.data.util.ErrorHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GuiaFormulario(
    private val activity: AppCompatActivity,
    private val raiz: View
) {

    private val guiaRepository = GuiaRepository(RetrofitClient.cotizacionApi)
    private val documentoRepository = DocumentoRepository(RetrofitClient.documentoApi)

    private val chkGuia = raiz.findViewById<CheckBox>(R.id.chkGuia)
    private val camposGuia = raiz.findViewById<View>(R.id.layoutGuiaCampos)
    private val ddlSerie = raiz.findViewById<AutoCompleteTextView>(R.id.ddlSerieGuia)
    private val txtNumero = raiz.findViewById<TextInputEditText>(R.id.txtNumeroGuia)
    private val txtFecha = raiz.findViewById<TextInputEditText>(R.id.txtFechaTraslado)
    private val ddlTipo = raiz.findViewById<AutoCompleteTextView>(R.id.ddlTipoTransportista)
    private val layoutTransportista = raiz.findViewById<View>(R.id.layoutTransportista)
    private val txtBuscarTransportista = raiz.findViewById<TextInputEditText>(R.id.txtBuscarTransportista)
    private val layoutConductor = raiz.findViewById<View>(R.id.layoutConductor)
    private val txtBuscarConductor = raiz.findViewById<TextInputEditText>(R.id.txtBuscarConductor)
    private val txtBultos = raiz.findViewById<TextInputEditText>(R.id.txtBultos)
    private val txtPeso = raiz.findViewById<TextInputEditText>(R.id.txtPeso)
    private val ddlUnidad = raiz.findViewById<AutoCompleteTextView>(R.id.ddlUnidadPeso)
    private val txtPlaca = raiz.findViewById<TextInputEditText>(R.id.txtPlaca)
    private val txtMarca = raiz.findViewById<TextInputEditText>(R.id.txtMarca)
    private val txtLicencia = raiz.findViewById<TextInputEditText>(R.id.txtLicencia)
    private val txtObservacion = raiz.findViewById<TextInputEditText>(R.id.txtObservacionGuia)

    private var tipos: List<ItemCatalogo> = emptyList()
    private var unidades: List<ItemCatalogo> = emptyList()
    private var series: List<String> = emptyList()
    private var tipoSel: ItemCatalogo? = null
    private var unidadSel: ItemCatalogo? = null
    private var transportistaSel: PersonaGuia? = null
    private var conductorSel: PersonaGuia? = null
    private var cargado = false

    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))

    // true cuando el usuario marcó "Emitir guía de remisión"
    val activa: Boolean get() = chkGuia.isChecked

    fun iniciar() {
        setDato(R.id.rowDestinoGuia, "Destino", "-")
        setDato(R.id.rowRucTransportista, "RUC", "-")
        setDato(R.id.rowDniConductor, "DNI", "-")

        // Igual que la web: el check activa (muestra) los datos de la guía
        chkGuia.setOnCheckedChangeListener { _, marcado ->
            camposGuia.visibility = if (marcado) View.VISIBLE else View.GONE
            if (marcado) prepararCampos() else limpiarErrores()
        }

        ddlSerie.setOnItemClickListener { _, _, position, _ ->
            CampoError.limpiar(ddlSerie)
            series.getOrNull(position)?.let { cargarNumero(it) }
        }
        ddlTipo.setOnItemClickListener { _, _, position, _ ->
            CampoError.limpiar(ddlTipo)
            tipos.getOrNull(position)?.let { seleccionarTipo(it) }
        }
        ddlUnidad.setOnItemClickListener { _, _, position, _ ->
            unidadSel = unidades.getOrNull(position)
        }

        txtFecha.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(
                activity,
                { _, year, month, day ->
                    cal.set(year, month, day)
                    txtFecha.setText(formatoFecha.format(cal.time))
                    CampoError.limpiar(txtFecha)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.minDate = System.currentTimeMillis() - 1000
            dialog.show()
        }

        // Buscadores (mínimo 3 letras), como en la web
        configurarBuscador(
            campo = txtBuscarTransportista,
            tipo = 1,
            nombreSeleccionado = { transportistaSel?.nombre }
        ) { p ->
            transportistaSel = p
            if (p != null) {
                txtBuscarTransportista.setText(p.nombre)
                CampoError.limpiar(txtBuscarTransportista)
            }
            setDato(R.id.rowRucTransportista, "RUC", p?.ruc?.ifBlank { "-" } ?: "-")
        }

        configurarBuscador(
            campo = txtBuscarConductor,
            tipo = 2,
            nombreSeleccionado = { conductorSel?.nombre }
        ) { p ->
            conductorSel = p
            if (p != null) {
                txtBuscarConductor.setText(p.nombre)
                CampoError.limpiar(txtBuscarConductor)
                // igual que la web: trae la placa y la licencia del conductor
                if (p.placa.isNotBlank()) txtPlaca.setText(p.placa)
                if (p.licencia.isNotBlank()) txtLicencia.setText(p.licencia)
            }
            setDato(R.id.rowDniConductor, "DNI", p?.ruc?.ifBlank { "-" } ?: "-")
        }
    }

    private fun prepararCampos() {
        // el destino siempre es la dirección de la factura (puede haber cambiado el cliente)
        setDato(
            R.id.rowDestinoGuia, "Destino",
            CotizacionEnCurso.cliente?.direccion?.ifBlank { "-" } ?: "-"
        )
        if (txtFecha.text.isNullOrBlank()) {
            txtFecha.setText(formatoFecha.format(Calendar.getInstance().time))
        }
        if (!cargado) {
            cargarCatalogos()
        } else {
            val serie = ddlSerie.text.toString()
            if (serie.isNotBlank()) cargarNumero(serie)   // el número pudo cambiar
        }
    }

    private fun cargarCatalogos() {
        activity.lifecycleScope.launch {
            try {
                val codEmpresa = SessionManager.getCodEmpresa()
                val codAlmacen = SessionManager.getCodAlmacen()

                val cat = guiaRepository.catalogos(codEmpresa, codAlmacen)
                tipos = cat.tipos
                unidades = cat.unidades

                ddlTipo.setAdapter(
                    ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
                        tipos.map { it.descripcion })
                )
                ddlUnidad.setAdapter(
                    ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item,
                        unidades.map { it.descripcion })
                )
                tipos.firstOrNull()?.let { seleccionarTipo(it) }
                unidades.firstOrNull()?.let {
                    unidadSel = it
                    ddlUnidad.setText(it.descripcion, false)
                }

                val lista = documentoRepository.listarSeries(COD_DOC_GUIA, codAlmacen, codEmpresa)
                series = lista.map { it.serieDoc }
                if (series.isEmpty()) {
                    CampoError.mostrar(ddlSerie, "NO HAY SERIES DE GUÍA ACTIVAS")
                } else {
                    ddlSerie.setAdapter(
                        ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, series)
                    )
                    ddlSerie.setText(series[0], false)
                    cargarNumero(series[0])
                }
                cargado = true
            } catch (e: Exception) {
                ErrorHelper.mostrar(activity, "Error cargando datos de la guía", e)
            }
        }
    }

    private fun cargarNumero(serie: String) {
        activity.lifecycleScope.launch {
            try {
                txtNumero.setText(
                    documentoRepository.obtenerNumero(
                        COD_DOC_GUIA,
                        SessionManager.getCodAlmacen(),
                        SessionManager.getCodEmpresa(),
                        serie
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    "Error obteniendo NÚMERO de guía: ${ErrorHelper.mensaje(e)}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Tipo 1 = con transportista (RUC). Tipo 2 = sin transportista (conductor propio).
    // Es la misma regla de BloqueoTipoTransportista de la web.
    private fun seleccionarTipo(t: ItemCatalogo) {
        tipoSel = t
        ddlTipo.setText(t.descripcion, false)
        val conTransportista = t.codigo == 1
        layoutTransportista.visibility = if (conTransportista) View.VISIBLE else View.GONE
        layoutConductor.visibility = if (conTransportista) View.GONE else View.VISIBLE
        if (conTransportista) {
            conductorSel = null
            txtBuscarConductor.setText("")
            setDato(R.id.rowDniConductor, "DNI", "-")
        } else {
            transportistaSel = null
            txtBuscarTransportista.setText("")
            setDato(R.id.rowRucTransportista, "RUC", "-")
        }
    }

    private fun configurarBuscador(
        campo: TextInputEditText,
        tipo: Int,
        nombreSeleccionado: () -> String?,
        onResultado: (PersonaGuia?) -> Unit
    ) {
        var resultados: List<PersonaGuia> = emptyList()
        var job: Job? = null

        val popup = ListPopupWindow(activity).apply {
            anchorView = campo
            setOnItemClickListener { _, _, position, _ ->
                val p = resultados.getOrNull(position) ?: return@setOnItemClickListener
                onResultado(p)
                dismiss()
            }
        }

        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val texto = s?.toString()?.trim() ?: ""

                // acaba de elegir de la lista: no volver a buscar
                if (texto == nombreSeleccionado()) return

                // si editó el texto, la selección anterior ya no vale
                onResultado(null)

                job?.cancel()
                if (texto.length < 3) {
                    popup.dismiss()
                    return
                }

                job = activity.lifecycleScope.launch {
                    delay(400)
                    try {
                        resultados = guiaRepository.buscar(texto, tipo)
                        if (resultados.isEmpty()) {
                            popup.dismiss()
                        } else {
                            popup.setAdapter(
                                ArrayAdapter(
                                    activity,
                                    android.R.layout.simple_list_item_1,
                                    resultados.map { "${it.nombre} (${it.ruc})" }
                                )
                            )
                            popup.show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            activity,
                            "Error buscando: ${ErrorHelper.mensaje(e)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    // Mismas reglas que Validacionguia de la web (la serie T es guía electrónica).
    // Marca en rojo los campos y lleva al primero con error.
    fun validar(): Boolean {
        limpiarErrores()

        var primerError: View? = null
        fun marcar(campo: View, mensaje: String) {
            CampoError.mostrar(campo, mensaje)
            if (primerError == null) primerError = campo
        }

        val serie = ddlSerie.text.toString().trim()
        val numero = txtNumero.text.toString().trim()
        val placa = txtPlaca.text.toString().trim()
        val licencia = txtLicencia.text.toString().trim()
        val bultos = txtBultos.text.toString().trim().toIntOrNull() ?: 0
        val peso = txtPeso.text.toString().trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
        val tipo = tipoSel?.codigo ?: 0
        val electronica = serie.startsWith("T")

        if (serie.isEmpty()) marcar(ddlSerie, "SELECCIONA LA SERIE DE LA GUÍA")
        if (numero.isEmpty()) marcar(txtNumero, "NÚMERO DE GUÍA NO DISPONIBLE")
        if (txtFecha.text.toString().isBlank()) marcar(txtFecha, "SELECCIONA LA FECHA DE TRASLADO")
        if (tipo == 0) marcar(ddlTipo, "SELECCIONA EL TIPO DE TRANSPORTISTA")

        if (electronica) {
            if (bultos <= 0) marcar(txtBultos, "INGRESA EL NRO. DE BULTOS")
            if (peso <= BigDecimal.ZERO) marcar(txtPeso, "INGRESA EL PESO")

            if (tipo == 1) {
                if (transportistaSel == null) {
                    marcar(txtBuscarTransportista, "SELECCIONA UN TRANSPORTISTA DE LA LISTA")
                }
            } else if (tipo == 2) {
                if (conductorSel == null) {
                    marcar(txtBuscarConductor, "SELECCIONA UN CONDUCTOR DE LA LISTA")
                }
                if (placa.isEmpty()) marcar(txtPlaca, "INGRESA LA PLACA")
                else if (placa.length !in 6..7) marcar(txtPlaca, "PLACA INVÁLIDA")

                if (licencia.isEmpty()) marcar(txtLicencia, "INGRESA LA LICENCIA")
                else if (licencia.length !in 9..10) marcar(txtLicencia, "LICENCIA INVÁLIDA")
            }
        }

        val destino = primerError
        if (destino != null) {
            CampoError.irA(destino)
            return false
        }
        return true
    }

    // Llamar solo después de validar() == true
    fun datos(): GuiaDatos {
        val tipo = tipoSel?.codigo ?: 0
        return GuiaDatos(
            serie = ddlSerie.text.toString().trim(),
            numero = txtNumero.text.toString().trim(),
            fechaTraslado = txtFecha.text.toString().trim(),
            codTipoTransportista = tipo,
            transportista = if (tipo == 1) transportistaSel else null,
            conductor = if (tipo == 2) conductorSel else null,
            placa = txtPlaca.text.toString().trim(),
            marca = txtMarca.text.toString().trim(),
            licencia = txtLicencia.text.toString().trim(),
            bultos = txtBultos.text.toString().trim().toIntOrNull() ?: 0,
            peso = txtPeso.text.toString().trim().toBigDecimalOrNull() ?: BigDecimal.ZERO,
            codUnidadPeso = unidadSel?.codigo ?: 0,
            observacion = txtObservacion.text.toString().trim()
        )
    }

    private fun limpiarErrores() {
        CampoError.limpiar(ddlSerie)
        CampoError.limpiar(txtNumero)
        CampoError.limpiar(txtFecha)
        CampoError.limpiar(ddlTipo)
        CampoError.limpiar(txtBuscarTransportista)
        CampoError.limpiar(txtBuscarConductor)
        CampoError.limpiar(txtBultos)
        CampoError.limpiar(txtPeso)
        CampoError.limpiar(txtPlaca)
        CampoError.limpiar(txtLicencia)
    }

    private fun setDato(includeId: Int, label: String, valor: String) {
        val row = raiz.findViewById<View>(includeId)
        row.findViewById<TextView>(R.id.txtLabelDato).text = label
        row.findViewById<TextView>(R.id.txtValorDato).text = valor
    }

    companion object {
        private const val COD_DOC_GUIA = 10
    }
}