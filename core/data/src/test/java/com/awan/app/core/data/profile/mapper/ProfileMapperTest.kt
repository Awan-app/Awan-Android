package com.awan.app.core.data.profile.mapper

import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.network.dto.profile.ProfileResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileMapperTest {

    @Test
    fun `toDomain sanitizes sentinel not entered last name to null`() {
        val response = ProfileResponse(
            id = "user-1",
            email = "user@test.com",
            firstName = "John",
            lastName = "not entered",
        )

        val domain = response.toDomain()

        assertEquals("John", domain.firstName)
        assertNull(domain.lastName)
    }

    @Test
    fun `toDomain preserves valid last name`() {
        val response = ProfileResponse(
            id = "user-1",
            email = "user@test.com",
            firstName = "John",
            lastName = "Doe",
        )

        val domain = response.toDomain()

        assertEquals("John", domain.firstName)
        assertEquals("Doe", domain.lastName)
    }

    @Test
    fun `asExternalModel sanitizes sentinel not entered last name to null`() {
        val userWithPrefs = UserWithPreferences(
            user = UserEntity(
                id = "user-1",
                email = "user@test.com",
                firstName = "John",
                lastName = "not entered",
                birthDate = null,
                points = 0,
                streak = 0,
                maxStreak = 0,
                profilePictureUrl = null,
                isNew = false,
            ),
            preferences = null,
        )

        val domain = userWithPrefs.asExternalModel()

        assertEquals("John", domain.firstName)
        assertNull(domain.lastName)
    }

    @Test
    fun `asEntity sanitizes sentinel not entered last name to null`() {
        val profile = Profile(
            id = "user-1",
            email = "user@test.com",
            firstName = "John",
            lastName = "not entered",
            birthDate = null,
            points = 0,
            streak = 0,
            maxStreak = 0,
            profilePictureUrl = null,
            isNew = false,
            preferences = null,
        )

        val entity = profile.asEntity()

        assertEquals("John", entity.firstName)
        assertNull(entity.lastName)
    }
}
