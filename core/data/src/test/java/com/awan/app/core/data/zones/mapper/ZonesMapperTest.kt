package com.awan.app.core.data.zones.mapper

import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
     * One type carries both the response's nested `category` and the request's `categoryId`, which
     * only works because the app's Json drops nulls. If that config ever changes, every zone write
     * starts posting a `category: null` the backend does not accept — fail here, not in the field.
     */
    @Test
    fun `a written zone carries categoryId and omits the read-only nested category`() {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            encodeDefaults = true
            coerceInputValues = true
        }

        val body = json.encodeToString(zoneDto(category = CategoryDto("cat-1", "Work")).toDomain().toDto())

        assertTrue(body, body.contains("\"categoryId\":\"cat-1\""))
        assertFalse(body, body.contains("\"category\""))
    }
}
