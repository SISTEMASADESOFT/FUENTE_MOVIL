package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ClienteApiService
import com.maestros.familias.data.model.Cliente
import com.maestros.familias.data.model.ClienteBuscarRequest
import com.maestros.familias.data.model.ConsultaDocumento
import com.maestros.familias.data.model.DireccionItem
import com.maestros.familias.data.model.DistritoBuscarRequest
import com.maestros.familias.data.model.DistritoItem
import com.maestros.familias.data.model.DocumentoConsultarRequest

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

    suspend fun buscarDistritos(texto: String): List<DistritoItem> {
        val lineas = api.listarDistritos(DistritoBuscarRequest(texto)).d
        return lineas.mapNotNull { linea ->
            val p = linea.split(",", limit = 4)
            if (p.size < 4) return@mapNotNull null
            DistritoItem(p[0], p[1], p[2], p[3])
        }
    }
    suspend fun consultarDocumento(numero: String, codEmpresa: Int): ConsultaDocumento {
        val r = api.consultarDocumento(DocumentoConsultarRequest(numero, codEmpresa)).d
        val p = r.split("~")
        return when (p[0]) {
            "1" -> ConsultaDocumento.Nuevo(
                Cliente(
                    codCtaCte = "0",
                    razonSocial = p.getOrElse(1) { "" },
                    direccion = p.getOrElse(2) { "" },
                    ruc = numero,
                    codDepartamento = p.getOrElse(3) { "0" },
                    codProvincia = p.getOrElse(4) { "0" },
                    codDistrito = p.getOrElse(5) { "0" },
                    codDireccion = "0",
                    distrito = p.getOrElse(6) { "" }
                )
            )
            "2" -> ConsultaDocumento.Existe(
                Cliente(
                    codCtaCte = p.getOrElse(1) { "0" },
                    razonSocial = p.getOrElse(2) { "" },
                    direccion = "",
                    ruc = numero,
                    codDepartamento = "0",
                    codProvincia = "0",
                    codDistrito = "0",
                    codDireccion = "0"
            )
            )
            else -> throw Exception(p.getOrElse(1) { "ERROR AL CONSULTAR" })
        }
    }

    suspend fun listarDirecciones(codCtaCte: String): List<DireccionItem> {
        val r = api.listarDirecciones(mapOf("CodCtaCte" to (codCtaCte.toIntOrNull() ?: 0)))
        return r.d.mapNotNull { fila ->
            val c = fila.split(",")
            if (c.size < 7) null
            else DireccionItem(
                codDireccion = c[0].trim(),
                principal = c[1].trim() == "1",
                codDepartamento = c[2].trim(),
                codProvincia = c[3].trim(),
                codDistrito = c[4].trim(),
                direccion = c[5].trim(),
                distrito = c[6].trim()
            )
        }
    }

}