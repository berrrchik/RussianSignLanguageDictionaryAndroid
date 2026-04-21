package com.rsl.dictionary.services.category

import com.rsl.dictionary.models.Category
import com.rsl.dictionary.repositories.protocols.SignRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class CategoryService @Inject constructor(
    private val signRepository: SignRepository
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    val categoriesFlow: StateFlow<List<Category>> = _categoriesFlow.asStateFlow()

    init {
        serviceScope.launch {
            signRepository.syncData.filterNotNull().collect { syncData ->
                _categoriesFlow.value = syncData.categories.sortedBy { it.order }
            }
        }
    }

    suspend fun getCategories(): List<Category> {
        val categories = signRepository.loadDataWithSync().categories.sortedBy { it.order }
        _categoriesFlow.value = categories
        return categories
    }

    suspend fun getCategory(byId: String): Category? {
        return getCategories().firstOrNull { it.id == byId }
    }
}
