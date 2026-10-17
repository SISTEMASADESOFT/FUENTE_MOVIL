package com.maestros.familias.data.model


data class DireccionItem(
    val codDireccion: String,
    val principal: Boolean,
    val codDepartamento: String,
    val codProvincia: String,
    val codDistrito: String,
    val direccion: String,
    val distrito: String
) {
    val nueva: Boolean get() = codDireccion == "0"

    val etiqueta: String
        get() = listOf(direccion.trim(), distrito.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" - ")
}