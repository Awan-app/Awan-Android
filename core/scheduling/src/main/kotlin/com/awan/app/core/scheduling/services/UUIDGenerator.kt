package com.awan.app.core.scheduling.services

import java.util.UUID

interface UUIDGenerating {
    fun makeUUID(): UUID
}

class SystemUUIDGenerator : UUIDGenerating {
    override fun makeUUID(): UUID = UUID.randomUUID()
}
