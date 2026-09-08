package com.maestros.familias.data.model

import com.google.gson.annotations.SerializedName

data class Familia(
    val idFamilia: Int = 0,
    val codFamilia: String = "",
    val dscFamilia: String = "",
    val estado: String = "",
    val codEstado: Int = 0,
    val codEmpresa: Int = 0
)

data class Combo(
    @SerializedName("CodConcepto")
    val codigo: String = "",

    @SerializedName("DscAbvConcepto")
    val descripcion: String = ""
)

data class ResultadoOperacion(
    val exito: Boolean,
    val mensaje: String
)