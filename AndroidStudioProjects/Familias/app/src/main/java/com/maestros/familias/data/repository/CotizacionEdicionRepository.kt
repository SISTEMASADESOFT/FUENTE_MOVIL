package com.maestros.familias.data.repository

import com.maestros.familias.data.api.CotizacionApiService
import com.maestros.familias.data.model.*
import java.math.BigDecimal

class CotizacionEdicionRepository(private val api: CotizacionApiService) {

    suspend fun obtener(codProforma: Int): CotizacionCargada {
        val lineas = api.obtener(CotizacionObtenerRequest(codProforma)).d

        lineas.firstOrNull { it.startsWith("ERROR:") }
            ?.let { throw Exception(it.removePrefix("ERROR:")) }

        val c = lineas.firstOrNull { it.startsWith("C|") }?.split("|")
            ?: throw Exception("No llegó la cabecera de la cotización")
        if (c.size < 27) throw Exception("Cabecera incompleta: falta agregar el total en el WebMethod")

        val cliente = Cliente(
            codCtaCte = c[9],
            razonSocial = c[11],
            direccion = c[16],
            ruc = c[10],
            codDepartamento = c[13],
            codProvincia = c[14],
            codDistrito = c[15],
            codDireccion = c[12]
        )

        val detalle = lineas.filter { it.startsWith("D|") }.mapNotNull { linea ->
            val p = linea.split("|")
            if (p.size < 5) return@mapNotNull null
            DetalleCargado(
                codProducto = p[1],
                cantidad = (p[2].toBigDecimalOrNull()?.toInt() ?: 1).coerceAtLeast(1),
                precio = p[3].toBigDecimalOrNull() ?: BigDecimal.ZERO,
                descripcion = p[4]
            )
        }
        if (detalle.isEmpty()) throw Exception("La cotización no tiene productos")

        return CotizacionCargada(
            codProforma = c[1].toIntOrNull() ?: 0,
            serie = c[2],
            numero = c[3],
            emision = c[4],
            codMoneda = c[5].toIntOrNull() ?: 0,
            tipoCambio = c[6].toBigDecimalOrNull() ?: BigDecimal.ZERO,
            codFormaPago = c[7].toIntOrNull() ?: 0,
            flagIgv = c[8].toIntOrNull() ?: 0,
            total = c[26].toBigDecimalOrNull() ?: BigDecimal.ZERO,
            cliente = cliente,
            placa = c[17],
            km = c[18],
            observacion = c[19],
            atencion = c[20],
            referencia = c[21],
            nroOperacion = c[22],
            codEmpleado = c[23].toIntOrNull() ?: 0,
            flagComisionable = c[24].toIntOrNull() ?: 0,
            flagConCodigo = c[25].toIntOrNull() ?: 0,
            detalle = detalle
        )
    }


    suspend fun editar(request: CotizacionEditarRequest): Triple<Boolean, String, Int> {
        val r = api.editar(request).d
        val p = r.split("~")
        return if (p[0] == "1")
            Triple(true, p.getOrElse(1) { "" }, p.getOrElse(2) { "0" }.toIntOrNull() ?: 0)
        else
            Triple(false, p.getOrElse(1) { r }, 0)
    }
}