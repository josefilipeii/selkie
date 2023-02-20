package org.jffc.generic

import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class ListResourceHandler(val config: Config) : HandlerFunction<ServerResponse> {
    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val query = request.queryParams()
        val valuesMatching = config.resource.seed
            .filterValues { it.matchQuery(query) }
        val metaMatchingKeys = config.resource.meta
            .filter { it.matchQuery(query) }
            .map { it.key }
        val metaMatching = config.resource.seed
            .filterKeys { metaMatchingKeys.contains(it) } ?: emptyMap()

        val map = (valuesMatching + metaMatching)
            .map { mapOf("id" to it.key) + it.value }.toSet()
        return ServerResponse.ok().body(BodyInserters.fromValue(map))
    }
}

private fun Map.Entry<String, Map<String, List<Any>>>.matchQuery(query: MultiValueMap<String, String>) =
    this.value.filter { met -> query.get(met.key)
        ?.let { (it.toSet() intersect met.value.toSet())
            .isNotEmpty() } ?: false }.isNotEmpty()

private fun Map<String, Any>.matchQuery(query: MultiValueMap<String, String>): Boolean = query.isEmpty() ||
        this.entries.any {
            //only first param
            query.getFirst(it.key)?.equals(it.value) ?: false
        }

