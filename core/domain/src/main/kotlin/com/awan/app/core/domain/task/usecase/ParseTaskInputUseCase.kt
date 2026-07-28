package com.awan.app.core.domain.task.usecase

import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.parser.TaskInputParser
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

class ParseTaskInputUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(input: String): ParsedTaskInput =
        TaskInputParser.parse(input, LocalDateTime.now(clock))
}
