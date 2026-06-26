package com.glucode.gautimes.domain

import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

interface Clock {
    fun now(): OffsetDateTime
}

class SystemClock @Inject constructor() : Clock {
    override fun now(): OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
}
