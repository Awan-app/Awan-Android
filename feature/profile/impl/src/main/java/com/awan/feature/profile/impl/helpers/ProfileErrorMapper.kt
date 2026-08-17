package com.awan.feature.profile.impl.helpers

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.text.UiText
import com.awan.feature.profile.impl.R

object ProfileErrorMapper {
    fun mapToUiText(error: AppError): UiText = when {
        error is AppError.Api && error.errorCode == "ZONE_OVERLAP" ->
            UiText.StringResource(R.string.profile_daily_zones_error_overlap)
        error is AppError.Api && error.errorCode == "DAY_ALREADY_ASSIGNED" ->
            UiText.StringResource(R.string.profile_daily_zones_error_day_assigned)
        error is AppError.Network ->
            UiText.StringResource(R.string.profile_daily_zones_error_network)
        error is AppError.Api ->
            UiText.StringResource(R.string.profile_daily_zones_error_generic)
        else -> error.toUiText()
    }
}
