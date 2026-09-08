package com.maestros.familias.data.repository

import com.maestros.familias.data.api.FamiliaApiService

import com.maestros.familias.data.model.Combo
import com.maestros.familias.data.model.Familia
import com.maestros.familias.data.model.ResultadoOperacion

class FamiliaRepository(
    private val api: FamiliaApiService
) {

    suspend fun listar(descripcion: String): List<Familia> {
        val response = api.listarFamilias(mapOf("DscFamilia" to descripcion))
        return response.d.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 6) return@mapNotNull null
            Familia(
                idFamilia = p[0].toIntOrNull() ?: 0,
                codFamilia = p[1],
                dscFamilia = p[2],
                estado = p[3],
                codEstado = p[4].toIntOrNull() ?: 0,
                codEmpresa = p[5].toIntOrNull() ?: 0
            )
        }
    }

    suspend fun listarEstados(): List<Combo> {
        val response = api.listarEstados()
        return response.d
    }

    //insertar
    suspend fun insertar(
        codEmpresa:Int, dscFamilia:String, codEstado:Int, codUsuario:Int): ResultadoOperacion {
        val body = mapOf(
            "CodEmpresa" to codEmpresa.toString(),
            "DscFamilia" to dscFamilia,
            "CodEstado" to codEstado.toString(),
            "CodUsuario" to codUsuario.toString()
        )
        val response = api.insertarFamilia(body)
        val exito = response.d == "SE GRABO CORRECTAMENTE"
        return ResultadoOperacion(exito, response.d)
    }

    //actualizar
    suspend fun actualizar(
        idFamilia: Int,
        codEmpresa: Int,
        dscFamilia: String,
        codEstado: Int,
        codUsuario: Int
    ): ResultadoOperacion {

        val body = mapOf(
            "IDFamilia" to idFamilia.toString(),
            "CodEmpresa" to codEmpresa.toString(),
            "DscFamilia" to dscFamilia,
            "CodEstado" to codEstado.toString(),
            "CodUsuario" to codUsuario.toString()
        )
        val response = api.actualizarFamilia(body)
        val exito = response.d == "SE ACTUALIZO CORRECTAMENTE"
        return ResultadoOperacion(exito,response.d)
    }

    //eliminar
    suspend fun eliminar(
        idFamilia: Int
    ): ResultadoOperacion {

        val body=mapOf("IDFamilia" to idFamilia.toString())
        val response = api.eliminarFamilia(body)
        return ResultadoOperacion(true, response.d)
    }

}