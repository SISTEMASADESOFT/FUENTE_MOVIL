package com.maestros.familias.data.api

import retrofit2.http.Headers
import retrofit2.http.POST

interface FormaPagoApiService {
    @POST("Servicios/Servicios.asmx/F_FormaPago_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listar(): ListResponse

    @POST("Servicios/Servicios.asmx/F_Moneda_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarMonedas(): ListResponse
}