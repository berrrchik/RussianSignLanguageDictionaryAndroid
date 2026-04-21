package com.rsl.dictionary.models

enum class ConnectivityStatus {
    Connected,
    NoInternet
}

enum class DataStatusReason {
    NoInternet,
    ServerUnavailable
}

sealed interface RepositoryDataStatus {
    data object Idle : RepositoryDataStatus
    data object Updated : RepositoryDataStatus
    data object UpToDate : RepositoryDataStatus
    data class UsingCachedData(val reason: DataStatusReason) : RepositoryDataStatus
    data class NoData(val reason: DataStatusReason) : RepositoryDataStatus
}

sealed interface OfflineIndicatorStatus {
    data object NoInternet : OfflineIndicatorStatus
    data class UsingCachedData(val reason: DataStatusReason) : OfflineIndicatorStatus
    data class NoData(val reason: DataStatusReason) : OfflineIndicatorStatus
}

sealed interface ScreenDataStatus {
    data object Loaded : ScreenDataStatus
    data object Updated : ScreenDataStatus
    data object UpToDate : ScreenDataStatus
    data class LoadedWithCachedWarning(val reason: DataStatusReason) : ScreenDataStatus
    data class Error(val reason: DataStatusReason) : ScreenDataStatus
}
