package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.repository.UserDataRepository
import javax.inject.Inject

class SetLocaleUseCase @Inject constructor(
    private val repository: UserDataRepository
) {
    suspend operator fun invoke(locale: String) = repository.setLocale(locale)
}
