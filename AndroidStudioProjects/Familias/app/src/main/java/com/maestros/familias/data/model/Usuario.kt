package com.maestros.familias.data.model

data class Usuario(
    val codUsuario: Int = 0,
    val nombreUsuario: String = "",
    val codAlmacen: Int = 0,
    val perfil: String = "",
    val apellidos: String = "",
    val nombre: String = "",
    val codEmpresa: Int = 0,
    val empresa: String = "",
    val pagina: String = "",
    val almacen: String = "",
    val msgError: String = ""
)
