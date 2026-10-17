package com.maestros.familias.data.repository

import android.util.Log
import com.maestros.familias.data.api.ImeiApiService
import com.maestros.familias.data.model.ImeiRequest

class ImeiRepository(private val api: ImeiApiService) {
    suspend fun estaAutorizado(imei: String): Boolean {
        val respuesta = api.validar(ImeiRequest(imei))

        Log.d("IMEI_TEST", "Enviado: $imei")
        Log.d("IMEI_TEST", "Respuesta d: ${respuesta.d}")

        return respuesta.d == "1"
    }
}