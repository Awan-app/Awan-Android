package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.datastore.UserPreferencesDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserPreferencesUseCase @Inject constructor(
    private val userDataRepository: UserPreferencesDataSource
) {
    operator fun invoke(): Flow<UserPreferencesData> = userDataRepository.userPreferences
}
