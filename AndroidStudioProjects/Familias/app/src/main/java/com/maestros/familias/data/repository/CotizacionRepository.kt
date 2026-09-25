package com.maestros.familias.data.repository

import com.maestros.familias.data.api.CotizacionApiService
import com.maestros.familias.data.model.CotizacionGrabarRequest
import com.maestros.familias.data.model.PdfRequest

class CotizacionRepository(
    private val api: CotizacionApiService
) {
    suspend fun grabar(request: CotizacionGrabarRequest): Triple<Boolean, String, Int> {
        val resultado = api.grabar(request).d
        val partes = resultado.split("~")
        val exito = partes.getOrNull(0) == "1"
        val mensaje = partes.getOrNull(1) ?: ""
        val codigo = partes.getOrNull(2)?.toIntOrNull() ?: 0
        return Triple(exito, mensaje, codigo)
    }

    suspend fun obtenerPdfBase64(codProforma: Int, codAlmacen: Int, serieDoc: String): String {
        return api.obtenerPdf(PdfRequest(codProforma, codAlmacen, serieDoc)).d
    }

}