package com.maestros.familias.data.repository

import com.maestros.familias.data.api.DocumentoApiService
import com.maestros.familias.data.model.NumeroDocRequest
import com.maestros.familias.data.model.SerieDocumento
import com.maestros.familias.data.model.SeriesDocRequest

class DocumentoRepository(private val api: DocumentoApiService) {

    suspend fun listarSeries(codTipoDoc: Int, codAlmacen: Int, codEmpresa: Int): List<SerieDocumento> {
        return api.seriesActivas(SeriesDocRequest(codTipoDoc, codAlmacen, codEmpresa)).d
            .mapNotNull { linea ->
                val p = linea.split(",")
                if (p.size < 2) null else SerieDocumento(p[0], p[1])
            }
    }

    suspend fun obtenerNumero(codTipoDoc: Int, codAlmacen: Int, codEmpresa: Int, serie: String): String {
        return api.numero(NumeroDocRequest(codTipoDoc, codAlmacen, codEmpresa, serie)).d
    }
}