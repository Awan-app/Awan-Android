package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.repository.UserDataRepository
import javax.inject.Inject

class SetDarkThemeUseCase @Inject constructor(
    private val repository: UserDataRepository
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setDarkThemeEnabled(enabled)
}
