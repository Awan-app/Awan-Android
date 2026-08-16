package com.awan.app.core.model

object ProfileConstants {
    const val UNSET_LAST_NAME = "not entered"
    const val DEFAULT_FIRST_NAME_FRIEND = "Friend"
    const val DEFAULT_BIRTH_DATE = "2000-01-01"
}

fun String?.sanitizeLastName(): String? =
    this?.trim()?.takeUnless { it.equals(ProfileConstants.UNSET_LAST_NAME, ignoreCase = true) || it.isBlank() }

fun String?.toApiLastName(): String =
    this?.trim()?.takeIf { it.isNotBlank() } ?: ProfileConstants.UNSET_LAST_NAME
