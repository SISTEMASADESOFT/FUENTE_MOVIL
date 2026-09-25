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

        productos.clear()
    }
}