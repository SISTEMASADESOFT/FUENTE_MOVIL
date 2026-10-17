package com.maestros.familias.data.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import retrofit2.HttpException

object ErrorHelper {

    // Texto completo del error. Si el servidor devuelve una página HTML, quita las etiquetas.
    fun detalle(e: Exception): String {
        if (e !is HttpException) return e.message ?: "error desconocido"

        val cuerpo = try { e.response()?.errorBody()?.string() } catch (x: Exception) { null }
        if (cuerpo.isNullOrBlank()) return "HTTP ${e.code()}"

        // Error de ASMX en JSON
        if (cuerpo.trimStart().startsWith("{")) {
            val m = Regex("\"Message\"\\s*:\\s*\"(.*?)\"").find(cuerpo)?.groupValues?.get(1)
            return "HTTP ${e.code()}\n" + (m ?: cuerpo.take(1500))
        }

        // Página HTML de error (ASP.NET, IIS o ngrok)
        val opciones = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        val titulo = Regex("<title>(.*?)</title>", opciones).find(cuerpo)?.groupValues?.get(1)?.trim()
        val texto = cuerpo
            .replace(Regex("<(script|style)[^>]*>.*?</\\1>", opciones), " ")
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return "HTTP ${e.code()}\n" +
                (if (!titulo.isNullOrBlank()) "Título: $titulo\n\n" else "") +
                texto.take(1500)
    }

    fun mensaje(e: Exception): String = detalle(e).replace("\n", " ").take(300)

    // Muestra el error en una ventana con scroll (el Toast se corta)
    fun mostrar(contexto: Context, titulo: String, e: Exception) {
        AlertDialog.Builder(contexto)
            .setTitle(titulo)
            .setMessage(detalle(e))
            .setPositiveButton("Cerrar", null)
            .show()
    }
}