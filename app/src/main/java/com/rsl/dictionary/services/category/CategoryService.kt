package com.rsl.dictionary.services.category

import com.rsl.dictionary.models.Category
import com.rsl.dictionary.repositories.protocols.SignRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CategoryService @Inject constructor(
    private val signRepository: SignRepository
) {
    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    suspend fun getCategories(): List<Category> {
        val categories = signRepository.loadDataWithSync().categories.sortedBy { it.order }
        _categoriesFlow.value = categories
        return categories
    }

    suspend fun getCategory(byId: String): Category? {
        return getCategories().firstOrNull { it.id == byId }
    }
}
