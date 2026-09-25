package com.maestros.familias.data.repository

import com.maestros.familias.data.api.CorrelativoApiService
import com.maestros.familias.data.model.NumeroRequest
import com.maestros.familias.data.model.Serie
import com.maestros.familias.data.model.SerieRequest

class CorrelativoRepository(
    private val api: CorrelativoApiService
) {
    suspend fun listarSeries(codAlmacen: Int, codEmpresa: Int): List<Serie> {
        val lineas = api.listarSeries(SerieRequest(15, codAlmacen, codEmpresa)).d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 2) return@mapNotNull null
            Serie(codSerie = p[0], serieDoc = p[1])
        }
    }

    suspend fun obtenerNumero(codAlmacen: Int, codEmpresa: Int, serieDoc: String): String {
        return api.obtenerNumero(NumeroRequest(15, codAlmacen, codEmpresa, serieDoc)).d
    }
}