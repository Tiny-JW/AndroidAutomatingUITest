package com.tiny.mock.openapiparser

import org.yaml.snakeyaml.Yaml
import java.io.IOException

internal fun String.parseYamlResource(): Map<String, Any> {
    @Suppress("UNCHECKED_CAST")
    return Yaml().load(this.getResourceAsString()) as? Map<String, Any> ?: throw IOException("Yaml parse error")
}

private fun String.getResourceAsString(): String {
    val loader = Thread.currentThread().contextClassLoader!!
    return loader.getResource(this)?.readText()
        ?: throw IOException("File does not exist: $this")
}