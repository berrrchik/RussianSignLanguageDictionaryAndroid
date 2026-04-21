package com.rsl.dictionary.repositories.protocols

import com.rsl.dictionary.models.SyncData

sealed interface SyncFetchResult {
    data class Updated(val data: SyncData) : SyncFetchResult
    data class NotModified(val data: SyncData) : SyncFetchResult
}
