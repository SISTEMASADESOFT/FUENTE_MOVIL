package com.maestros.familias.data.repository

import com.maestros.familias.data.api.FormaPagoApiService
import com.maestros.familias.data.model.FormaPago
import com.maestros.familias.data.model.Moneda

class FormaPagoRepository(
    private val api: FormaPagoApiService
) {
    suspend fun listar(): List<FormaPago> {
        val lineas = api.listar().d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 2) return@mapNotNull null
            FormaPago(codConcepto = p[0], descripcion = p[1])
        }
    }

    suspend fun listarMonedas(): List<Moneda> {
        val lineas = api.listarMonedas().d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 2) return@mapNotNull null
            Moneda(codConcepto = p[0], descripcion = p[1])
        }
    }
}