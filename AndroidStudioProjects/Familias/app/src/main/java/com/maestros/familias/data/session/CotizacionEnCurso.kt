package com.maestros.familias.data.session

import com.maestros.familias.data.model.Cliente
import com.maestros.familias.data.model.ProductoSeleccionado
import java.math.BigDecimal

object CotizacionEnCurso {
    var cliente: Cliente? = null
    var serie: String = ""
    var numero: String = ""
    var fechaEmision: String = ""
    var moneda: String = "Soles (PEN)"

    var codProformaGrabada: Int = 0
    var tipoCambio: BigDecimal = BigDecimal.ZERO

    var codMoneda: Int = 1
    var igvPorcentaje: BigDecimal = BigDecimal.ZERO
    var codFormaPago: String = ""
    var codTasa: String = ""
    var formaPago: String = ""
    val productos: MutableList<ProductoSeleccionado> = mutableListOf()

    var direccionEtiqueta: String = ""

    var codProformaEdicion: Int = 0
    var codEmpleado: Int = 0
    var placa: String = ""
    var km: String = ""
    var observacion: String = ""
    var atencion: String = ""
    var referencia: String = ""
    var nroOperacion: String = ""
    var flagComisionable: Int = 0
    var flagConCodigo: Int = 0

    var fechaVencimiento: String = ""

    var guiaEmitida: String = ""


    //factura
    var tipoDoc: Int = 15   // 15 = cotización, 1 = factura
    val esFactura: Boolean get() = tipoDoc == 1
    val esEdicion: Boolean get() = codProformaEdicion > 0

    fun limpiar() {
        cliente = null
        serie = ""
        numero = ""
        fechaEmision = ""
        moneda = "Soles (PEN)"
        tipoCambio = BigDecimal.ZERO
        igvPorcentaje = BigDecimal.ZERO
        codFormaPago = ""
        codTasa = ""
        formaPago = ""
        direccionEtiqueta = ""
        productos.clear()


        codProformaEdicion = 0
        codEmpleado = 0
        placa = ""
        km = ""
        observacion = ""
        atencion = ""
        referencia = ""
        nroOperacion = ""
        flagComisionable = 0
        flagConCodigo = 0
        tipoDoc = 15
        fechaVencimiento = ""
        guiaEmitida = ""
    }
}