package com.tiny.mock

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class BackendMock {

    companion object {
        private val mockWebServerDispatcher = MockWebServerDispatcher()

        private var isServerStarted = false
        private val server: MockWebServer = MockWebServer().apply {
            dispatcher = mockWebServerDispatcher
        }
    }

    fun startServer(port: Int) = synchronized(server) {
        if (!isServerStarted) {
            isServerStarted = true
            server.start(port)
        }
    }

    fun shutdownServer() = synchronized(server) {
        if (isServerStarted) {
            isServerStarted = false
            server.shutdown()
            clearSelectedResponses()
            clearEnqueuedResponses()
        }
    }

    fun getExampleResponse(
        method: String,
        endpoint: String,
        exampleName: String
    ) = mockWebServerDispatcher.getExampleResponse(method, endpoint, exampleName)

    fun enqueueResponse(method: String, endpoint: String, response: MockResponse) =
        mockWebServerDispatcher.enqueueResponse(method, endpoint, response)

    fun selectResponse(
        method: String,
        endpoint: String,
        name: String
    ) = mockWebServerDispatcher.selectResponse(method, endpoint, name)

    fun clearSelectedResponses() = mockWebServerDispatcher.clearSelectedResponses()

    fun clearEnqueuedResponses() = mockWebServerDispatcher.clearEnqueuedResponses()

    var useDefaultResponse: Boolean
        get() = mockWebServerDispatcher.useDefaultResponses
        set(value) {
            mockWebServerDispatcher.useDefaultResponses = value
        }
}