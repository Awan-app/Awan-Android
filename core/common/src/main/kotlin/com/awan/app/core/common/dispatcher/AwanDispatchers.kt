package com.awan.app.core.common.dispatcher

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val awanDispatcher: AwanDispatchers)
enum class AwanDispatchers {
    IO,
    Default,
}
