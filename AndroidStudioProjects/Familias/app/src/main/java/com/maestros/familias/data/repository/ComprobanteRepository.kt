package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ComprobanteApiService
import com.maestros.familias.data.model.Comprobante
import com.maestros.familias.data.model.ComprobanteListarRequest

class ComprobanteRepository(
    private val api: ComprobanteApiService
) {
    suspend fun listarCotizaciones(codAlmacen: Int, codEmpresa: Int, texto: String = ""): List<Comprobante> {
        val lineas = api.listarCotizaciones(ComprobanteListarRequest(codAlmacen, codEmpresa, texto)).d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 7) return@mapNotNull null
            Comprobante(
                codigo = p[0], numero = p[1], cliente = p[2],
                fechaEmision = p[3], total = p[4], estado = p[5], moneda = p[6]
            )
        }
    }
}