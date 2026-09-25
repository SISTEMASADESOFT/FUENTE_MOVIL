package com.maestros.familias.data.model

data class Cliente(
    val codCtaCte: String,
    val razonSocial: String,
    val direccion: String,
    val ruc: String,
    val codDepartamento: String = "0",
    val codProvincia: String = "0",
    val codDistrito: String = "0",
    val codDireccion: String = "0"
)