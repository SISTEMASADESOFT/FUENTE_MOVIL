package com.maestros.familias.data.model

data class DistritoBuscarRequest(val Descripcion: String)

data class DistritoItem(
    val codDepartamento: String,
    val codProvincia: String,
    val codDistrito: String,
    val descripcion: String
)
