package com.junwu.androidautomatinguitest

import com.tiny.mock.BackendMock
import com.tiny.mock.Endpoints
import org.junit.rules.ExternalResource

private const val MOCK_SERVER_PORT = 8080

class MockWebServerTestRule : ExternalResource() {

    private val backedMock = BackendMock().apply {
        useDefaultResponse = false
    }

    override fun before() {
        super.before()
        backedMock.startServer(MOCK_SERVER_PORT)
    }

    override fun after() {
        backedMock.shutdownServer()
        super.after()
    }

    fun enqueue(backendResponse: Endpoints.BackendResponse) = with(backendResponse) {
        backedMock.enqueueResponse(
            method,
            endpoint,
            backedMock.getExampleResponse(method, endpoint, name)
        )
    }
}
