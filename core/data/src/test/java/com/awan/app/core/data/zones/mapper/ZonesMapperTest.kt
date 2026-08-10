package com.awan.app.core.data.zones.mapper

import com.awan.app.core.network.di.NetworkModule
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.zone.ZoneDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZonesMapperTest {

    private fun zoneDto(category: CategoryDto? = null, categoryId: String? = null) = ZoneDto(
        id = "zone-1",
        name = "Work",
        startTime = "09:00:00",
        endTime = "12:00:00",
        color = "#4CAF50",
        templateId = "template-1",
        category = category,
        categoryId = categoryId,
    )

    /**
     * The bulk zone endpoints replace-all, so every save round-trips the server's zones back through
     * these two mappers. Losing the id here is how a whole template silently 422s.
     */
    @Test
    fun `category id survives a response-to-request round trip`() {
        val dto = zoneDto(category = CategoryDto(id = "cat-1", name = "Work"))

        val sentBack = dto.toDomain().toDto()

        assertEquals("cat-1", dto.toDomain().categoryId)
        assertEquals("cat-1", sentBack.categoryId)
    }

    @Test
    fun `an explicit category id wins over the nested category`() {
        val dto = zoneDto(category = CategoryDto(id = "nested", name = "Work"), categoryId = "explicit")

        assertEquals("explicit", dto.toDomain().categoryId)
    }

    @Test
    fun `a zone with no category maps to a null id rather than a blank one`() {
        assertNull(zoneDto().toDomain().categoryId)
        assertNull(zoneDto().toDomain().toDto().categoryId)
    }

    /**
     * One type carries both the response's nested `category` and the request's `categoryId`, so what
     * lands in the body is decided by the app's Json config — which is why this encodes with the very
     * instance Hilt provides rather than a copy of it. An earlier copy here claimed
     * `explicitNulls = false`; production has always set it to `true`, so the assertion that the
     * nested `category` is omitted was never true of a real request.
     */
    @Test
    fun `a written zone carries its categoryId`() {
        val body = NetworkModule.providesNetworkJson()
            .encodeToString(zoneDto(category = CategoryDto("cat-1", "Work")).toDomain().toDto())

        assertTrue(body, body.contains("\"categoryId\":\"cat-1\""))
    }
}
