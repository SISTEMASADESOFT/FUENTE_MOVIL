package com.maestros.familias.data.util

import com.maestros.familias.data.session.CotizacionEnCurso
import java.text.NumberFormat
import java.util.Locale

object CotizacionFormatter {

    private val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "PE"))

    private fun calcularTotales(): Triple<java.math.BigDecimal, java.math.BigDecimal, java.math.BigDecimal> {
        val total = CotizacionEnCurso.productos.fold(java.math.BigDecimal.ZERO) { acc, item -> acc.add(item.subtotal) }
        val divisor = java.math.BigDecimal.ONE.add(CotizacionEnCurso.igvPorcentaje)
        val subtotal = total.divide(divisor, 2, java.math.RoundingMode.HALF_UP)
        val igv = total.subtract(subtotal)
        return Triple(subtotal, igv, total)
    }

    fun generarTextoWhatsApp(): String {
        val cliente = CotizacionEnCurso.cliente
        val (subtotal, igv, total) = calcularTotales()

        val sb = StringBuilder()
        sb.append("*COTIZACIÓN ${CotizacionEnCurso.serie}-${CotizacionEnCurso.numero}*\n\n")
        sb.append("Cliente: ${cliente?.razonSocial ?: "-"}\n")
        sb.append("RUC: ${cliente?.ruc ?: "-"}\n")
        sb.append("Fecha: ${CotizacionEnCurso.fechaEmision}\n\n")
        sb.append("*Productos:*\n")

        CotizacionEnCurso.productos.forEachIndexed { index, item ->
            sb.append("${index + 1}. ${item.descripcion}\n")
            sb.append("   ${item.cantidad} x ${formatoMoneda.format(item.precioUnitario)} = ${formatoMoneda.format(item.subtotal)}\n\n")
        }

        sb.append("*Subtotal:* ${formatoMoneda.format(subtotal)}\n")
        sb.append("*IGV (18%):* ${formatoMoneda.format(igv)}\n")
        sb.append("*Total:* ${formatoMoneda.format(total)}\n\n")


        return sb.toString()
    }

    fun generarCuerpoCorreo(): String {
        val cliente = CotizacionEnCurso.cliente
        val (subtotal, igv, total) = calcularTotales()

        val sb = StringBuilder()
        sb.append("Estimado(a) ${cliente?.razonSocial ?: "cliente"},\n\n")
        sb.append("Adjuntamos el detalle de su cotización ${CotizacionEnCurso.serie}-${CotizacionEnCurso.numero} de fecha ${CotizacionEnCurso.fechaEmision}:\n\n")
        sb.append("Productos:\n")

        CotizacionEnCurso.productos.forEachIndexed { index, item ->
            sb.append("${index + 1}. ${item.descripcion}\n")
            sb.append("   Cantidad: ${item.cantidad} | Precio unitario: ${formatoMoneda.format(item.precioUnitario)} | Subtotal: ${formatoMoneda.format(item.subtotal)}\n\n")
        }

        sb.append("Subtotal: ${formatoMoneda.format(subtotal)}\n")
        sb.append("IGV (18%): ${formatoMoneda.format(igv)}\n")
        sb.append("Total: ${formatoMoneda.format(total)}\n\n")
        sb.append("Quedamos atentos a cualquier consulta.\n\n")
        sb.append("Saludos cordiales.")

        return sb.toString()
    }

    fun asuntoCorreo(): String = "Cotización ${CotizacionEnCurso.serie}-${CotizacionEnCurso.numero}"
}