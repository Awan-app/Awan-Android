package com.awan.app.core.domain.onboarding.usecase

import com.awan.app.core.domain.onboarding.repository.OnboardingRepository
import javax.inject.Inject

class HasCompletedOnboardingUseCase @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(): Boolean = onboardingRepository.hasCompletedOnboarding()
}
