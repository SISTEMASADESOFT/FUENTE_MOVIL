package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ProductoApiService
import com.maestros.familias.data.model.Moneda
import com.maestros.familias.data.model.Producto
import com.maestros.familias.data.model.ProductoBuscarRequest
import java.math.BigDecimal

class ProductoRepository(
    private val api: ProductoApiService
) {

    suspend fun buscar(descripcion: String, codAlmacen: String, codMoneda: Int): List<Producto> {
        val lineas = api.buscarProductos(ProductoBuscarRequest(descripcion, codAlmacen, codMoneda)).d

        

        return lineas.mapNotNull { linea ->
            val p = linea.split(",")
            if (p.size < 5) return@mapNotNull null

            Producto(
                codigoInterno = p[1],
                descripcion = p[2],
                stock = p[3],
                precio = p[4].toBigDecimalOrNull() ?: BigDecimal.ZERO,
                codProducto = p[0]
            )
        }
    }
}