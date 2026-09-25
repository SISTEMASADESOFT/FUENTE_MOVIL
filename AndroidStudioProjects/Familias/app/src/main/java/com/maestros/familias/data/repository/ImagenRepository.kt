package com.maestros.familias.data.repository

import com.maestros.familias.data.api.ImagenApiService
import com.maestros.familias.data.model.CodProductoRequest

class ImagenRepository(
    private val api: ImagenApiService
) {
    suspend fun obtenerImagenesBase64(codProducto: Int): List<String> {
        return api.obtenerImagenes(CodProductoRequest(codProducto)).d
    }
}