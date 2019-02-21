package apptentive.com.android.network

import apptentive.com.android.convert.Deserializer
import apptentive.com.android.convert.JsonSerializer
import apptentive.com.android.convert.Serializer
import apptentive.com.android.core.TimeInterval
import java.io.IOException

/**
 * Creates mock string HTTP-request.
 */
internal fun createMockHttpRequest(
    tag: String? = null,
    method: HttpMethod = HttpMethod.GET,
    url: String? = null,
    statusCode: Int = 200,
    response: String? = null,
    responseHeaders: HttpHeaders? = null,
    exceptionOnSend: Boolean = false,
    exceptionOnReceive: Boolean = false,
    retryPolicy: HttpRequestRetryPolicy? = null
): HttpRequest<String> {
    val overrideMethod = if (exceptionOnSend) HttpMethod.POST else method
    val content = response?.toByteArray() ?: ByteArray(0)
    val includesBody = overrideMethod == HttpMethod.POST || overrideMethod == HttpMethod.PUT

    val requestSerializer = if (includesBody) object : Serializer {
        override fun serialize(): ByteArray {
            if (exceptionOnSend) {
                throw IOException("failed to send")
            }

            return content
        }
    } else null

    val responseDeserializer = object : Deserializer<String> {
        override fun deserialize(bytes: ByteArray): String {
            if (exceptionOnReceive) {
                throw IOException("failed to receive")
            }

            return String(bytes)
        }
    }

    return createMockHttpRequest(
        responseDeserializer = responseDeserializer,
        tag = tag,
        method = overrideMethod,
        url = url,
        content = content,
        statusCode = statusCode,
        requestSerializer = requestSerializer,
        responseHeaders = responseHeaders,
        retryPolicy = retryPolicy
    )
}

internal fun createMockHttpRequest(
    responses: Array<HttpResponseBody>,
    tag: String? = null,
    method: HttpMethod = HttpMethod.GET,
    url: String? = null,
    requestSerializer: Serializer? = null,
    retryPolicy: HttpRequestRetryPolicy? = null
): HttpRequest<String> {
    val deserializer = object : Deserializer<String> {
        override fun deserialize(bytes: ByteArray): String {
            return String(bytes)
        }
    }

    return createMockHttpRequest(
        responses = responses,
        responseDeserializer = deserializer,
        tag = tag,
        method = method,
        url = url,
        requestSerializer = requestSerializer,
        retryPolicy = retryPolicy
    )
}

/**
 * Creates a generic mock HTTP-request.
 */
internal fun <T> createMockHttpRequest(
    responseDeserializer: Deserializer<T>,
    tag: String? = null,
    method: HttpMethod = HttpMethod.GET,
    url: String? = null,
    statusCode: Int = 200,
    content: ByteArray? = null,
    requestSerializer: Serializer? = null,
    responseHeaders: HttpHeaders? = null,
    retryPolicy: HttpRequestRetryPolicy? = null
): HttpRequest<T> {
    val response = createNetworkResponse(
        statusCode = statusCode,
        content = content,
        responseHeaders = responseHeaders
    )
    return createMockHttpRequest(
        responses = arrayOf(response),
        responseDeserializer = responseDeserializer,
        requestSerializer = requestSerializer,
        url = url,
        method = method,
        retryPolicy = retryPolicy,
        tag = tag
    )
}

/**
 * Creates a generic mock HTTP-request with a sequence of responses.
 */
internal fun <T> createMockHttpRequest(
    responses: Array<HttpResponseBody>,
    responseDeserializer: Deserializer<T>,
    tag: String? = null,
    method: HttpMethod = HttpMethod.GET,
    url: String? = null,
    requestSerializer: Serializer? = null,
    retryPolicy: HttpRequestRetryPolicy? = null
): HttpRequest<T> {
    return HttpRequest(
        tag = tag,
        method = method,
        url = url ?: "https://example.com",
        requestSerializer = requestSerializer,
        responseDeserializer = responseDeserializer,
        retryPolicy = retryPolicy,
        userData = HttpResponseBodyQueue(responses)
    )
}

internal inline fun <reified T : Any> createMockJsonRequest(
    responseObject: T,
    requestObject: Any? = null,
    tag: String? = null,
    method: HttpMethod = HttpMethod.GET,
    url: String? = null
): HttpRequest<T> {
    val userData = createNetworkResponses(
        content = JsonSerializer(responseObject).serialize()
    )
    val overrideMethod = if (requestObject != null) HttpMethod.POST else method
    return createHttpJsonRequest(
        method = overrideMethod,
        url = url ?: "https://example.com",
        requestObject = requestObject,
        tag = tag,
        userData = userData
    )
}


internal fun createNetworkResponses(vararg responses: HttpResponseBody): HttpResponseBodyQueue {
    return HttpResponseBodyQueue(responses)
}

/**
 * Helper function for creating mock network responses.
 */
internal fun createNetworkResponses(
    statusCode: Int = 200,
    content: ByteArray? = null,
    responseHeaders: HttpHeaders? = null,
    duration: TimeInterval = 0.0
): HttpResponseBodyQueue {
    return createNetworkResponses(
        createNetworkResponse(
            statusCode = statusCode,
            content = content,
            responseHeaders = responseHeaders,
            duration = duration
        )
    )
}

/**
 * Helper function for creating mock network responses.
 */
internal fun createNetworkResponse(
    statusCode: Int = 200,
    content: ByteArray? = null,
    responseHeaders: HttpHeaders? = null,
    duration: TimeInterval = 0.0
): HttpResponseBody {
    return HttpResponseBody(
        statusCode = statusCode,
        statusMessage = getStatusMessage(statusCode),
        content = content ?: ByteArray(0),
        headers = responseHeaders ?: MutableHttpHeaders(),
        duration = duration
    )
}

private fun getStatusMessage(statusCode: Int): String {
    return when (statusCode) {
        200 -> "OK"
        204 -> "No Content"
        401 -> "Unauthorized"
        500 -> "Internal Server Error"
        else -> "Unknown"
    }
}