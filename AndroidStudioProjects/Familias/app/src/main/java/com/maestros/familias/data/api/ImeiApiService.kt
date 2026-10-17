package com.maestros.familias.data.api

import com.maestros.familias.data.model.ImeiRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ImeiApiService {
    @POST("Servicios/Servicios.asmx/F_Imei_Validar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun validar(@Body body: ImeiRequest): StringResponse
}