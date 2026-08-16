package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.repository.UserDataRepository
import com.awan.app.core.model.DarkThemeConfig
import javax.inject.Inject

class SetDarkThemeUseCase @Inject constructor(
    private val repository: UserDataRepository
) {
    suspend operator fun invoke(config: DarkThemeConfig) = repository.setDarkThemeConfig(config)
}
