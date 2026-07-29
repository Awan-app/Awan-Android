package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserDataUseCase @Inject constructor(
    private val repository: UserDataRepository
) {
    operator fun invoke(): Flow<UserData> = repository.userData
}
