package com.maestros.familias.data.repository

import com.maestros.familias.data.api.MenuApiService
import com.maestros.familias.data.model.MenuModulo
import com.maestros.familias.data.model.MenuPagina

class MenuRepository(
    private val api: MenuApiService
) {

    suspend fun listarMenu(): List<MenuModulo>{
        val lineas = api.listarMenu().d

        val paginasPorModulo = mutableMapOf<Int, MutableList<MenuPagina>>()
        val nombreModulo = mutableMapOf<Int, String>()

        for (linea in lineas){
            val p = linea.split(",")
            if(p.size < 6) continue

            val codigoMenu = p[0].toIntOrNull() ?: continue
            val dscMenu = p[1]
            val codigoPagina = p[2].toIntOrNull() ?: continue
            val codigoInterno = p[3].toIntOrNull() ?: 0
            val dscPagina = p[4]
            val nivel = p[5].toIntOrNull() ?: 0

            nombreModulo[codigoMenu] = dscMenu
            paginasPorModulo.getOrPut(codigoMenu) {mutableListOf() }
                .add(MenuPagina(codigoPagina, codigoInterno, dscPagina, nivel))
        }

        return paginasPorModulo.map { (codigoMenu, paginas) ->
            MenuModulo(codigoMenu, nombreModulo[codigoMenu] ?: "", paginas)

        }.sortedBy { it.codigoMenu }
    }
}