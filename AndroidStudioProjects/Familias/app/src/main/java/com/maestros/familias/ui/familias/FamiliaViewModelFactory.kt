package com.maestros.familias.ui.familias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.maestros.familias.data.repository.FamiliaRepository

class FamiliaViewModelFactory(
    private val repository: FamiliaRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamiliaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FamiliaViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}