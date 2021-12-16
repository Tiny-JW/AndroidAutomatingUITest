package com.tiny.mock

import android.net.Uri
import android.util.Log
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

internal class MockWebServerDispatcher : Dispatcher() {

    private data class EnqueuedResponse(
        val method: String,
        val endpoint: String,
        val response: MockResponse
    )

    private val responses = Endpoints.responses
    private val selectedResponses = mutableMapOf<String, MutableMap<String, MockResponse>>()
    private val enqueuedResponses: Queue<EnqueuedResponse> = LinkedList()
    private val defaultErrorResponse = MockResponse().setResponseCode(404)

    override fun dispatch(request: RecordedRequest): MockResponse {
        val method = request.method ?: return defaultErrorResponse
        val endpoint =
            Uri.parse(request.path).path?.lowercase(Locale.US) ?: return defaultErrorResponse

        enqueuedResponses.peek()?.let {
            return if (method != it.method || !endpoint.equals(it.endpoint, ignoreCase = true)) {
                logW("-- Invalid method or path enqueued expected [method: '${it.method}'; endpoint='${it.endpoint}'] got [method: '$method'; endpoint='$endpoint'] returning 404")
                defaultErrorResponse
            } else {
                logI("-- Using enqueued response for [method: '${it.method}'; endpoint='${it.endpoint}]'")
                enqueuedResponses.poll()
                it.response
            }
        }

        return overriddenResponse(method = method, endpoint = endpoint)
            ?: defaultResponse(method = method, endpoint = endpoint)
            ?: defaultErrorResponse
    }

    var useDefaultResponses = true

    private fun overriddenResponse(
        method: String,
        endpoint: String
    ): MockResponse? {
        val result = selectedResponses[method]?.get(endpoint.lowercase(Locale.US))
        if (result != null) {
            logI("-- Using selected response for [method: '$method'; endpoint='$endpoint']")
        }
        return result
    }

    private fun defaultResponse(
        method: String,
        endpoint: String
    ): MockResponse? {
        val result =
            if (useDefaultResponses) getExamples(method, endpoint)?.values?.firstOrNull() else null
        if (result == null) {
            logW("-- No default response for [method: '$method'; endpoint='$endpoint']")
        }
        return result
    }

    private fun getExamples(
        method: String,
        endpoint: String
    ) = responses[method]?.get(endpoint.lowercase(Locale.US))

    fun getExampleResponse(
        method: String,
        endpoint: String,
        exampleName: String
    ): MockResponse = getExamples(method, endpoint)?.get(exampleName)
        ?: error("Response for [method: '$method'; endpoint='$endpoint' exampleName='$exampleName'] not found")

    fun enqueueResponse(
        method: String,
        endpoint: String,
        response: MockResponse
    ) {
        enqueuedResponses.offer(
            EnqueuedResponse(
                method = method,
                endpoint = endpoint.lowercase(Locale.US),
                response = response
            )
        )
    }

    fun selectResponse(
        method: String,
        endpoint: String,
        name: String
    ) {
        val lowerCaseEndpoint = endpoint.lowercase(Locale.US)
        val response = getExampleResponse(method, lowerCaseEndpoint, name)
        val body = response.getBody().toString()
        logI("-- Selected response for [method: '$method'; endpoint='$endpoint' body='$body']")
        var endpoints = selectedResponses[method]
        if (endpoints == null) {
            endpoints = mutableMapOf()
            selectedResponses[method] = endpoints
        }
        endpoints[lowerCaseEndpoint] = response
    }

    fun clearSelectedResponses() = selectedResponses.clear()

    fun clearEnqueuedResponses() = enqueuedResponses.clear()

    private fun logI(message: String) {
        Log.i(javaClass.simpleName, message)
    }

    private fun logW(message: String) {
        Log.w(javaClass.simpleName, message)
    }
}
