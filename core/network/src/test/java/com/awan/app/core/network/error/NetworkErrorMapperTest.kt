package com.awan.app.core.network.error

import com.awan.app.core.common.error.AppError
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkErrorMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `toAppError maps 401 to AppError Unauthorized even with errorCode`() {
        val errorJson = """{"errorCode":"TOKEN_EXPIRED","message":"Unauthorized token"}"""
        val response = Response.error<Any>(401, errorJson.toResponseBody())
        val exception = HttpException(response)

        val appError = exception.toAppError(json)

        assertEquals(AppError.Unauthorized, appError)
    }

    @Test
    fun `toAppError maps 500 to AppError Server even with errorCode`() {
        val errorJson = """{"errorCode":"INTERNAL_SERVER_ERROR","message":"Server error"}"""
        val response = Response.error<Any>(500, errorJson.toResponseBody())
        val exception = HttpException(response)

        val appError = exception.toAppError(json)

        assertTrue(appError is AppError.Server)
        assertEquals(500, (appError as AppError.Server).code)
    }

    @Test
    fun `toAppError maps 400 with errorCode to AppError Api`() {
        val errorJson = """{"errorCode":"INVALID_INPUT","message":"Bad input"}"""
        val response = Response.error<Any>(400, errorJson.toResponseBody())
        val exception = HttpException(response)

        val appError = exception.toAppError(json)

        assertTrue(appError is AppError.Api)
        val apiError = appError as AppError.Api
        assertEquals(400, apiError.code)
        assertEquals("INVALID_INPUT", apiError.errorCode)
        assertEquals("Bad input", apiError.body)
    }
}
