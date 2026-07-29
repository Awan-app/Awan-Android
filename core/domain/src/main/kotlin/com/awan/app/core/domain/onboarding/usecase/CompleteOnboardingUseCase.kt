package com.awan.app.core.domain.onboarding.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.domain.onboarding.repository.OnboardingRepository
import javax.inject.Inject

class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(data: OnboardingData): Result<Unit> =
        onboardingRepository.completeOnboarding(data)
}
