package com.maestros.familias.data.api

import com.maestros.familias.data.model.Combo
import com.maestros.familias.data.model.Familia
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FamiliaApiService {
    @POST("Servicios/Servicios.asmx/F_LGFamilias_Listar")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarFamilias(@Body body: Map<String, String>): ListResponse

    @POST("Servicios/Servicios.asmx/F_LGFamilias_Grabar")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun insertarFamilia(@Body body: Map<String, String>): MsgResponse

    @POST("Servicios/Servicios.asmx/F_LGFamilias_Editar")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun actualizarFamilia(@Body body: Map<String, String>): MsgResponse

    @POST("Servicios/Servicios.asmx/F_LGFamilias_Eliminar")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun eliminarFamilia(@Body body: Map<String, String>): MsgResponse

    @POST("Servicios/Servicios.asmx/F_LGFamilias_Estados")
    @Headers("Content-Type: application/json; charset=utf-8")
    suspend fun listarEstados(): EstadosResponse


}


data class EstadosResponse(
    val d: List<Combo> = emptyList()
)
data class ListResponse(
    val d: List<String> = emptyList()
)

data class MsgResponse(
    val d: String = ""
)