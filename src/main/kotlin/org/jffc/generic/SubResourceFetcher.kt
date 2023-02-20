package org.jffc.generic

import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface SubResourceFetcher {
    fun find(service: String, request: Request, parent: Map<String, Any>): Mono<Map<String, List<Any>>>
}

class K8sSubResourceFetcher(
    private val discoveryClient: DiscoveryClient,
    private val webClientBuilder: WebClient.Builder,
) : SubResourceFetcher {
    override fun find(service: String, request: Request, parent: Map<String, Any>): Mono<Map<String, List<Any>>> {
        val instances = discoveryClient.getInstances(service)
        return if (instances.isNotEmpty()) {
            instances
                .first().let {
                    webClientBuilder.baseUrl(it.uri.toString()).build()
                }.get()
                .applyRequest(service, parent, request)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List::class.java)
                .map { it as List<Any> }
                .map { mapOf(service to it) }
        } else {
            Mono.empty()
        }

    }


}


private fun WebClient.RequestHeadersUriSpec<*>.applyRequest(
    service: String,
    parent: Map<String, Any>,
    request: Request
) = this.uri { builder ->
    val query: Map<String, List<Any>> = request.query.map { Pair(it.name, parent[it.field]) }
        .associateBy({ it.first }, { pair -> pair.second?.let { listOf(it) } ?: emptyList() })
        .filterValues { it.isNotEmpty() }
    builder.queryParams(LinkedMultiValueMap(query as MutableMap<String, MutableList<String>>))
        .path(service)
        .build()
}
