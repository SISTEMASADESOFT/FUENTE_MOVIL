package com.maestros.familias.data.api

import com.maestros.familias.data.model.ProductoBuscarRequest
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ProductoApiService {
    @POST("Servicios/Servicios.asmx/F_Cotizacion_Productos_Buscar_App")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun buscarProductos(@Body body: ProductoBuscarRequest): ListResponse
}