package com.maestros.familias.data.repository

import com.maestros.familias.data.api.TipoCambioApiService
import com.maestros.familias.data.model.Igv
import com.maestros.familias.data.model.TipoCambioRequest
import java.math.BigDecimal

class TipoCambioRepository(
    private val api: TipoCambioApiService
) {
    suspend fun obtener(fechaYyyyMmDd: String): BigDecimal {
        val resultado = api.obtener(TipoCambioRequest(fechaYyyyMmDd)).d
        return resultado.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    suspend fun obtenerIgv(): Igv {
        val resultado = api.obtenerIgv().d
        val p = resultado.split(",")
        val codTasa = p.getOrNull(0) ?: "0"
        val valor = p.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        return Igv(codTasa, valor)
    }
}