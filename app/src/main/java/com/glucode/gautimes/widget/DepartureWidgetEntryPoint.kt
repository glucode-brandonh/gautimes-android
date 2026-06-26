package com.glucode.gautimes.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DepartureWidgetEntryPoint {
    fun getDepartureWidgetStateUseCase(): GetDepartureWidgetStateUseCase
}
