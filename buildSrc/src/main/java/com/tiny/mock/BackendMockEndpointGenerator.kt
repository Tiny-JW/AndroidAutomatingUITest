package com.tiny.mock

import com.tiny.mock.openapiparser.Example
import com.tiny.mock.openapiparser.OpenApiParser
import java.util.*

internal object BackendMockEndpointGenerator {

    const val PACKAGE = "com.tiny.mock"
    const val FILENAME = "Endpoints.kt"
    private val resourcesFileNames = listOf("api-spec.yaml")

    fun generate(): String {
        val responses = OpenApiParser().loadApiSpecs(resourcesFileNames)
        return StringBuilder().apply {
            append("package $PACKAGE")
            append(header)
            responses.forEach { (method, endpoints) ->
                generateMethodObject(1, method, endpoints)
            }
            generateResponsesMap(1, responses)
            append(footer)
            append("\n}\n")
        }.toString()
    }

    private fun StringBuilder.generateMethodObject(
        indent: Int,
        method: String,
        endpoints: Map<String, LinkedHashMap<String, Example>>
    ) {
        indent(indent); append("object ${method.toKotlinName()} {\n")
        endpoints.forEach { (endpoint, examples) ->
            generateEndpointObject(indent + 1, method, endpoint, examples)
        }
        indent(indent); append("}\n")
    }

    private fun StringBuilder.generateEndpointObject(
        indent: Int,
        method: String,
        endpoint: String,
        examples: LinkedHashMap<String, Example>
    ) {
        indent(indent); append("object ${endpoint.toKotlinName()} {\n")
        examples.forEach { (exampleName, example) ->
            generateExampleObject(indent + 1, method, endpoint, exampleName, example)
        }
        indent(indent); append("}\n")
    }

    private fun StringBuilder.generateExampleObject(
        indent: Int,
        method: String,
        endpoint: String,
        exampleName: String,
        example: Example,
    ) {
        val name = exampleName.toKotlinName()
        indent(indent); append("val $name = BackendResponse(\n")
        indent(indent + 1); append("method = \"$method\",\n")
        indent(indent + 1); append("endpoint = \"$endpoint\",\n")
        indent(indent + 1); append("name = \"$exampleName\",\n")
        indent(indent + 1); append("response = ${example.generateMockResponse()}\n")
        indent(indent); append(")\n")
    }

    private fun StringBuilder.generateResponsesMap(
        indent: Int,
        responses: Map<String, Map<String, LinkedHashMap<String, Example>>>
    ) {
        indent(indent); append("val responses = mapOf<String, Map<String, LinkedHashMap<String, MockResponse>>>(\n")
        responses.forEach { (method, endpoints) ->
            generateMethodMap(indent + 1, method, endpoints)
        }
        indent(indent); append(")\n")
    }

    private fun StringBuilder.generateMethodMap(
        indent: Int,
        method: String,
        endpoints: Map<String, LinkedHashMap<String, Example>>
    ) {
        indent(indent); append("\"$method\" to mapOf(\n")
        endpoints.forEach { (endpoint, examples) ->
            generateEndpointMap(indent + 1, method, endpoint, examples)
        }
        indent(indent); append("),\n")
    }

    private fun StringBuilder.generateEndpointMap(
        indent: Int,
        method: String,
        endpoint: String,
        examples: LinkedHashMap<String, Example>
    ) {
        indent(indent); append("\"$endpoint\" to LinkedHashMap<String, MockResponse>().apply {\n")
        examples.forEach { (name, _) ->
            generateExampleMap(indent + 1, method, endpoint, name)
        }
        indent(indent); append("},\n")
    }

    private fun StringBuilder.generateExampleMap(
        indent: Int,
        method: String,
        endpoint: String,
        name: String,
    ) {
        indent(indent); append("put(\"$name\", ${getExampleResponse(method, endpoint, name)})\n")
    }

    private fun Example.generateMockResponse(): String =
        "MockResponse().apply { setResponseCode(${responseCode}); addHeaders(${responseHeaders.joinToString()}); setBody(\"\"\"${responseBody}\"\"\") }"

    private fun getExampleResponse(method: String, endpoint: String, name: String): String =
        "${method.toKotlinName()}.${endpoint.toKotlinName()}.${name.toKotlinName()}.response"

    @Suppress("DefaultLocale")
    private fun String.toKotlinName(): String {
        val name = if (startsWith("/")) substring(1) else this
        val items = name.split('/', '-', '.', ' ')
        val stringBuilder = StringBuilder()
        items.forEach {
            stringBuilder.append(it.toLowerCase().capitalize())
        }
        return stringBuilder.toString()
    }

    private fun StringBuilder.indent(count: Int) =
        repeat(count) { append("    ") }

    private fun List<Pair<String, String>>.joinToString() =
        this.joinToString(
            prefix = "listOf(",
            postfix = ")"
        ) {
            "\"${it.first}\" to \"${it.second}\""
        }

    private val header = """

        import okhttp3.mockwebserver.MockResponse

        object Endpoints {

    """.trimIndent()

    private const val footer = """
    private fun MockResponse.addHeaders(headers: List<Pair<String, String>>) {
        headers.forEach { addHeader(it.first, it.second) }
    }

    data class BackendResponse(
        val method: String,
        val endpoint: String,
        val name: String,
        val response: MockResponse
    )
    """
}