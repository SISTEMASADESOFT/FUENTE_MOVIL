package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ComprobanteApiService
import com.maestros.familias.data.model.Comprobante
import com.maestros.familias.data.model.ComprobanteAnularRequest
import com.maestros.familias.data.model.ComprobanteListarRequest
import com.maestros.familias.data.model.ComprobantePdfRequest

class ComprobanteRepository(
    private val api: ComprobanteApiService
) {
    suspend fun listarCotizaciones(
        codAlmacen: Int,
        codEmpresa: Int,
        texto: String = "",
        usarFecha: Boolean = false,
        desde: String = "",
        hasta: String = ""
    ): List<Comprobante> {
        val lineas = api.listarCotizaciones(
            ComprobanteListarRequest(codAlmacen, codEmpresa, texto, usarFecha, desde, hasta)
        ).d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 7) return@mapNotNull null
            Comprobante(
                codigo = p[0], numero = p[1], cliente = p[2],
                fechaEmision = p[3], total = p[4], estado = p[5], moneda = p[6]
            )
        }
    }

    //devuelve el PDF en Base64
    suspend fun obtenerPdf(codProforma: Int, codEmpresa: Int, codAlmacen: Int, serieDoc: String): String {
        val r = api.obtenerPdf(ComprobantePdfRequest(codProforma, codEmpresa, codAlmacen, serieDoc)).d
        if (r.startsWith("ERROR:")) throw Exception(r.removePrefix("ERROR:"))
        return r
    }


    suspend fun anular(codProforma: Int, codUsuario: Int, observacion: String): String {
        val r = api.anular(ComprobanteAnularRequest(codProforma, codUsuario, observacion)).d
        if (r.startsWith("ERROR:")) throw Exception(r.removePrefix("ERROR:"))
        return r
    }
}