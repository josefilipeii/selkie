package org.jffc.generic

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "data-bin")
data class Config(var resource: Resource = Resource())

data class Resource(
    val name: String = "",
    val seed: Map<String, Map<String, Any>> = emptyMap(),
    val meta: Map<String, Map<String, List<Any>>> = emptyMap(),
    val embedded: Set<Embedded> = emptySet()
)

data class Embedded(
    val name: String = "",
    val service: String = "",
    val request: Request = Request()
)

data class Request(val query: Set<FieldDescriptor> = emptySet())

data class FieldDescriptor(val name: String, val field: String)
