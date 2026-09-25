package com.maestros.familias.data.model

import java.math.BigDecimal

data class Producto(
    val codigoInterno: String,
    val descripcion: String,
    val stock: String,
    val precio: java.math.BigDecimal,
    val codProducto: String
)

data class ProductoSeleccionado(
    val producto: Producto,
    var cantidad: Int,
    var descripcion: String = producto.descripcion,
    var precioUnitario: BigDecimal = producto.precio
) {
    val subtotal: BigDecimal
        get() = precioUnitario.multiply(BigDecimal(cantidad))
}