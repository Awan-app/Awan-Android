package com.awan.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    // ── TemplateEntity ────────────────────────────────────────────────────────

    @Upsert
    suspend fun upsertTemplate(template: TemplateEntity)

    @Upsert
    suspend fun upsertTemplates(templates: List<TemplateEntity>)

    @Query("SELECT * FROM templates")
    fun observeAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :templateId")
    fun observeTemplate(templateId: String): Flow<TemplateEntity?>

    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplate(templateId: String): TemplateEntity?

    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: String)

    /**
     * Clears the table. `template_days_of_week` and template-owned zones CASCADE away with it — as
     * would any future entity holding an FK to `templates`. For `replaceAll` only.
     */
    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()

    @Query("SELECT MIN(expiryTime) FROM templates")
    suspend fun getMinExpiryTime(): Long?

    // ── TemplateDayOfWeekEntity ───────────────────────────────────────────────

    @Upsert
    suspend fun upsertDays(days: List<TemplateDayOfWeekEntity>)

    @Query("SELECT * FROM template_days_of_week WHERE templateId = :templateId")
    fun observeDaysForTemplate(templateId: String): Flow<List<TemplateDayOfWeekEntity>>

    /** Returns the row for [dayOfWeek] (e.g. `MONDAY`), or null if no template owns it. */
    @Query("SELECT * FROM template_days_of_week WHERE dayOfWeek = :dayOfWeek")
    suspend fun getDayAssignment(dayOfWeek: String): TemplateDayOfWeekEntity?

    @Query("DELETE FROM template_days_of_week WHERE templateId = :templateId")
    suspend fun deleteDaysForTemplate(templateId: String)

    // ── Combined ──────────────────────────────────────────────────────────────

    /**
     * Atomically persist a template and its day assignments.
     * Replaces existing days so a PUT response (full replacement) is safe to call.
     */
    @Transaction
    suspend fun upsertTemplateWithDays(
        template: TemplateEntity,
        days: List<TemplateDayOfWeekEntity>,
    ) {
        upsertTemplate(template)
        deleteDaysForTemplate(template.id)
        upsertDays(days)
    }
}
