package com.maestros.familias.data.util

import java.math.BigDecimal
import java.math.RoundingMode

object MonedaHelper {
    fun simbolo(codMoneda: Int): String = if (codMoneda == 2) "US$" else "S/"

    fun formatear(monto: BigDecimal, codMoneda: Int): String {
        return "${simbolo(codMoneda)} ${monto.setScale(2, java.math.RoundingMode.HALF_UP)}"
    }

    fun convertirPrecio(precio: BigDecimal, codMonedaNueva: Int, tipoCambio: BigDecimal): BigDecimal {
        if (tipoCambio <= BigDecimal.ZERO) return precio
        return if (codMonedaNueva == 1) {
            precio.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP)
        } else {
            precio.divide(tipoCambio, 2, RoundingMode.HALF_UP)
        }
    }

}