
package com.maestros.familias.data.repository

import com.maestros.familias.data.api.CotizacionApiService
import com.maestros.familias.data.model.GuiaBuscarRequest
import com.maestros.familias.data.model.GuiaCatalogos
import com.maestros.familias.data.model.GuiaCatalogosRequest
import com.maestros.familias.data.model.ItemCatalogo
import com.maestros.familias.data.model.PersonaGuia

class GuiaRepository(
    private val api: CotizacionApiService
) {


    suspend fun catalogos(codEmpresa: Int, codAlmacen: Int): GuiaCatalogos {
        val resultado = api.guiaCatalogos(GuiaCatalogosRequest(codEmpresa, codAlmacen)).d
        val partes = resultado.split("~")
        if (partes.getOrNull(0) != "1") {
            throw Exception(partes.getOrNull(1) ?: "Error cargando catálogos de la guía")
        }
        return GuiaCatalogos(
            tipos = parsear(partes.getOrNull(1)),
            unidades = parsear(partes.getOrNull(2))
        )
    }

    suspend fun buscar(texto: String, tipo: Int): List<PersonaGuia> {
        return api.guiaBuscar(GuiaBuscarRequest(texto, tipo)).d.mapNotNull { fila ->
            val c = fila.split("|")
            val cod = c.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            PersonaGuia(
                codCtaCte = cod,
                ruc = c.getOrElse(1) { "" }.trim(),
                razonSocial = c.getOrElse(2) { "" }.trim(),
                nombres = c.getOrElse(3) { "" }.trim(),
                placa = c.getOrElse(4) { "" }.trim(),
                licencia = c.getOrElse(5) { "" }.trim()
            )
        }
    }

    private fun parsear(bloque: String?): List<ItemCatalogo> {
        if (bloque.isNullOrBlank()) return emptyList()
        return bloque.split("|").mapNotNull { fila ->
            val c = fila.split(",", limit = 2)
            val cod = c.getOrNull(0)?.trim()?.toIntOrNull() ?: return@mapNotNull null
            ItemCatalogo(cod, c.getOrElse(1) { "" }.trim())
        }
    }
}