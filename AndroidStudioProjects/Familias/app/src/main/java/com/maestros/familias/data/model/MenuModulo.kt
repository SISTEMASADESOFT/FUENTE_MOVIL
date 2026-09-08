package com.maestros.familias.data.model

data class MenuModulo(
    val codigoMenu:Int,
    val dscMenu: String,
    val paginas: List<MenuPagina>
)
