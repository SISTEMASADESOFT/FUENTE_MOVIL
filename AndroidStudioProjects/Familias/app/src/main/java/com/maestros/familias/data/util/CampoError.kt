package com.maestros.familias.data.util

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import java.util.WeakHashMap

object CampoError {

    private const val ROJO = "#B5433E"
    private val fondos = WeakHashMap<View, Drawable?>()
    private val avisos = WeakHashMap<View, TextView>()

    private fun tilDe(v: View): TextInputLayout? {
        var p = v.parent
        while (p != null) {
            if (p is TextInputLayout) return p
            p = p.parent
        }
        return null
    }


    fun mostrar(campo: View, mensaje: String, aviso: TextView? = null) {
        val texto = mensaje.uppercase()

        val til = tilDe(campo)
        if (til != null) {
            til.error = texto
            return
        }


        val cont = campo.parent as? View ?: campo
        val d = cont.resources.displayMetrics.density
        val rojo = Color.parseColor(ROJO)

        if (!fondos.containsKey(cont)) fondos[cont] = cont.background
        cont.background = GradientDrawable().apply {
            cornerRadius = 25 * d
            setColor(Color.WHITE)
            setStroke((2 * d).toInt(), rojo)
        }

        if (aviso != null) {
            aviso.text = texto
            aviso.setTextColor(rojo)
            aviso.visibility = View.VISIBLE
            return
        }

        val padre = cont.parent as? LinearLayout
        if (padre != null && padre.orientation == LinearLayout.VERTICAL) {
            val tv = avisos[cont] ?: TextView(cont.context).apply {
                textSize = 12f
                setPadding((4 * d).toInt(), (4 * d).toInt(), 0, 0)
                padre.addView(this, padre.indexOfChild(cont) + 1)
                avisos[cont] = this
            }
            tv.text = texto
            tv.setTextColor(rojo)
        } else {
            Toast.makeText(cont.context, texto, Toast.LENGTH_SHORT).show()
        }
    }

    fun limpiar(campo: View, aviso: TextView? = null) {
        val til = tilDe(campo)
        if (til != null) {
            til.error = null
            return
        }
        val cont = campo.parent as? View ?: return
        if (fondos.containsKey(cont)) cont.background = fondos.remove(cont)
        aviso?.visibility = View.GONE
        avisos.remove(cont)?.let { (it.parent as? ViewGroup)?.removeView(it) }
    }


    fun irA(campo: View) {
        val destino = tilDe(campo) ?: (campo.parent as? View) ?: campo
        destino.post {
            val extra = (40 * destino.resources.displayMetrics.density).toInt()
            destino.requestRectangleOnScreen(
                Rect(0, 0, destino.width, destino.height + extra),
                false
            )
        }
    }
}