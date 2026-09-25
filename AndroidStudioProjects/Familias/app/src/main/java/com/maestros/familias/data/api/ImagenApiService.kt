package com.maestros.familias.data.api

import com.maestros.familias.data.model.CodProductoRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ImagenApiService {
    @POST("Servicios/Servicios.asmx/F_Producto_Imagenes_Obtener_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun obtenerImagenes(@Body body: CodProductoRequest): ListResponse
}