package org.jffc.generic

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse


@Configuration
@EnableDiscoveryClient
@EnableConfigurationProperties(Config::class)
class RouterConfiguration(private val config: Config,
                          private val discoveryClient: DiscoveryClient) {



    @Bean
    fun subResourceFetcher(): SubResourceFetcher = K8sSubResourceFetcher(discoveryClient, WebClient.builder())

    @Bean
    fun composedRoutes() =
        RouterFunctions.route(
            RequestPredicates.GET("/${config.resource.name}/{id}"),
            ResourceHandler(config.resource, subResourceFetcher())
        )
            .and(
                RouterFunctions.route(
                    RequestPredicates.GET("/${config.resource.name}")
                )
                { req ->
                    val query = req.queryParams()
                    val map = config.resource.seed
                        .filterValues { it.matchQuery(query) }
                    .entries
                        .map { mapOf("id" to it.key) + it.value }.toSet()
                    ServerResponse.ok().body(BodyInserters.fromValue(map)) }
            )

}

private fun  Map<String,Any>.matchQuery(query: MultiValueMap<String, String>): Boolean = query.isEmpty() ||
        this.entries.any {
            //only first param
            query.getFirst(it.key)?.equals(it.value) ?: false }

