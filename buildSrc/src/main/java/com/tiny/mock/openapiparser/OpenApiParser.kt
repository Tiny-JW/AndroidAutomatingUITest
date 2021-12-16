package com.tiny.mock.openapiparser

import org.json.JSONObject

internal class Example(val responseCode: Int) {
    var responseBody: String = ""
    val responseHeaders = mutableListOf<Pair<String, String>>()
}

@Suppress("UNCHECKED_CAST")
internal class OpenApiParser {

    fun loadApiSpecs(resourceFileNames: List<String>): Map<String, Map<String, LinkedHashMap<String, Example>>> {
        val responses = mutableMapOf<String, Map<String, LinkedHashMap<String, Example>>>()
        for (resourceFileName in resourceFileNames) {
            val result = loadApiSpec(resourceFileName)
            result.forEach {
                if (responses.containsKey(it.key) && responses[it.key] != null) {
                    responses[it.key] = responses[it.key]!!.plus(it.value)
                } else {
                    responses[it.key] = it.value
                }
            }
        }
        return responses
    }

    private fun loadApiSpec(resourceFileName: String): Map<String, Map<String, LinkedHashMap<String, Example>>> {
        val result = mapOf<String, MutableMap<String, LinkedHashMap<String, Example>>>(
            "GET" to mutableMapOf(),
            "POST" to mutableMapOf(),
            "PUT" to mutableMapOf(),
            "PATCH" to mutableMapOf(),
        )
        val spec = resourceFileName.parseYamlResource()
        (spec["paths"] as? Map<String, Any>)?.forEach {
            loadPath(result, spec, it.key, it.value as Map<String, Any>)
        }
        return result
    }

    private fun loadPath(
        result: Map<String, MutableMap<String, LinkedHashMap<String, Example>>>,
        spec: Map<String, Any>,
        path: String,
        pathValues: Map<String, Any>
    ) {
        val pathParameters = (pathValues["parameters"] as? List<Any>)?.let {
            loadPathParameters(spec, it)
        } ?: emptyMap()

        for (value in pathValues) {
            val method = value.key.toUpperCase()
            val responses = (value.value as? Map<String, Any>)?.get("responses") ?: continue
            val hasErrorResponse = mutableSetOf<Int>()
            (responses as? Map<String, Any>)?.forEach {
                val code = it.key.toInt()
                val isErrorResponse = (code in 100 until 300).not()
                if (isErrorResponse) hasErrorResponse.add(code)
                loadSuccessResponse(
                    result = result,
                    path = path,
                    method = method,
                    code = code,
                    response = it.value as Map<String, Any>,
                    pathParameters = pathParameters,
                    defaultResponsePrefix = if (isErrorResponse) "error" else "ok"
                )
            }
            listOf(404, 500).forEach {
                if (!hasErrorResponse.contains(it)) {
                    addDefaultErrorResponse(
                        code = it,
                        result = result,
                        path = path,
                        method = method,
                        pathParameters = pathParameters,
                    )
                }
            }
        }
    }

    private fun loadSuccessResponse(
        result: Map<String, MutableMap<String, LinkedHashMap<String, Example>>>,
        path: String,
        method: String,
        code: Int,
        response: Map<String, Any>,
        pathParameters: Map<String, List<String>>,
        defaultResponsePrefix: String
    ) {
        val content = (response as? Map<String, Any>)?.get("content")

        var examples = mutableMapOf<String, String>()
        (content as? Map<String, Any>)?.forEach {
            when (it.key) {
                "application/json" -> examples = loadExampleSuccessJsonContent(it.value as Map<String, Any>)
            }
        }
        if (content == null) {
            examples["$defaultResponsePrefix-$code"] = ""
        }

        if (examples.isNullOrEmpty()) {
            println("OpenApiParser: No example responses found for method=$method path=$path code=$code")
            return
        }

        val paths = generatePaths(path, pathParameters)
        result[method]?.let { map ->
            paths.forEach { path ->
                createMockResponsesForPath(map, path.toLowerCase(), examples, code, response)
            }
        }
    }

    private fun addDefaultErrorResponse(
        result: Map<String, MutableMap<String, LinkedHashMap<String, Example>>>,
        path: String,
        method: String,
        pathParameters: Map<String, List<String>>,
        code: Int
    ) {
        val examples = mapOf("error-default-$code" to "")
        val paths = generatePaths(path, pathParameters)
        result[method]?.let { map ->
            paths.forEach { path ->
                createMockResponsesForPath(map, path.toLowerCase(), examples, code, null)
            }
        }
    }

    private fun createMockResponsesForPath(
        map: MutableMap<String, LinkedHashMap<String, Example>>,
        path: String,
        examples: Map<String, String>?,
        code: Int,
        response: Map<String, Any>?
    ) {
        if (map[path] == null) map[path] = LinkedHashMap()
        val temp = map[path]!!

        examples?.forEach { (name, body) ->
            val mockResponse = Example(code).apply {
                response?.let {
                    for (header in loadHeaderForResponse(response)) {
                        responseHeaders.add(header.key to header.value)
                    }
                }
                responseBody = body
            }
            var idx = 1
            var newName = name
            while (temp[newName] != null) {
                newName = "$name${idx++}"
            }
            temp[newName] = mockResponse
        }
    }

    private fun loadHeaderForResponse(response: Map<String, Any>): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        (response["headers"] as? Map<String, Any>)?.forEach {
            headers[it.key] = (it.value as? Map<String, String>)?.get("example") ?: ""
        }
        return headers
    }

    private fun loadExampleSuccessJsonContent(jsonMimeType: Map<String, Any>): MutableMap<String, String> {
        val result = mutableMapOf<String, String>()
        (jsonMimeType["examples"] as? Map<String, Any>)?.forEach { (name, example) ->
            val value = (example as? Map<String, Any>)?.get("value")
            val json = (value as? Map<String, String>)?.let { JSONObject(it).toString() } ?: (value as? List<Map<String, Any>>)?.let { jsonArray(it) } ?: "".apply {
                println("OpenApiParser: Don't know how to parse: $value")
            }
            result[name] = json
        }
        return result
    }

    private fun loadPathParameters(
        spec: Map<String, Any>,
        parameters: List<Any>
    ): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        for (parameter in parameters) {
            (parameter as? Map<String, Any>) ?: continue
            val inPath = parameter["in"] == "path"
            if (inPath) {
                val name = parameter["name"] as? String ?: continue
                val schema = (parameter["schema"] as? Map<String, String>) ?: continue
                schema["\$ref"]?.let {
                    result[name] = getParametersForSchema(spec, it)
                }
            }
        }
        return result
    }

    private fun getParametersForSchema(
        spec: Map<String, Any>,
        schema: String
    ): List<String> {
        val schemaPath = schema.split('/')
        if (schemaPath.size < 4) return emptyList()
        if (schemaPath[0] != "#") return emptyList() // TODO

        val components = spec[schemaPath[1]] as? Map<String, Any> ?: return emptyList()
        val schemas = components[schemaPath[2]] as? Map<String, Any> ?: return emptyList()
        val values = schemas[schemaPath[3]] as? Map<String, Any> ?: return emptyList()
        val value = values["enum"] as? List<String> ?: return emptyList()
        return value
    }

    private fun generatePaths(
        templatePath: String,
        parameters: Map<String, List<String>>
    ): List<String> {
        val paths = mutableListOf<String>()
        val startIndex = templatePath.indexOf('{', 0)
        if (startIndex < 0) {
            paths.add(templatePath)
            return paths
        }

        val endIdx = templatePath.indexOf('}', startIndex)

        val templateName = templatePath.subSequence(startIndex + 1, endIdx).toString()
        parameters[templateName]?.forEach { template ->
            val newTemplatePath = templatePath.replace("{$templateName}", template)
            paths.addAll(generatePaths(newTemplatePath, parameters))
        }

        return paths
    }

    private fun jsonArray(array: List<Map<String, Any>>): String {
        return array.joinToString(prefix = "[", postfix = "]", separator = ",") {
            JSONObject(it).toString()
        }
    }
}
