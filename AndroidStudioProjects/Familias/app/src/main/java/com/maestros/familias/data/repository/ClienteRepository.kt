package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ClienteApiService
import com.maestros.familias.data.model.Cliente
import com.maestros.familias.data.model.ClienteBuscarRequest

class ClienteRepository(
    private val api: ClienteApiService
) {

    suspend fun buscar(termino: String): List<Cliente> {
        val esRuc = termino.length in 8..11 && termino.all { it.isDigit() }

        val request = if (esRuc) {
            ClienteBuscarRequest(NroRuc = termino, RazonSocial = "")
        } else {
            ClienteBuscarRequest(NroRuc = "", RazonSocial = termino)
        }

        val lineas = api.buscarClientes(request).d

        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 15) return@mapNotNull null

            Cliente(
                codCtaCte = p[0],
                razonSocial = p[1],
                direccion = p[2],
                ruc = p[8],
                codDepartamento = p[5],
                codProvincia = p[6],
                codDistrito = p[7],
                codDireccion = p[14]
            )
        }
    }
}