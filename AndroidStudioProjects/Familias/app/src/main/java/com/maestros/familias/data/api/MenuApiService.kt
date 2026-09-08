package com.maestros.familias.data.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface MenuApiService {
    @POST("Servicios/Servicios.asmx/F_Menu_Listar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarMenu(): ListResponse
}