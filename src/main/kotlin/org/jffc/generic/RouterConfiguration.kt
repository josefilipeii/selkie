package org.jffc.generic

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunctions

@Configuration
@EnableDiscoveryClient
@EnableConfigurationProperties(Config::class)
class RouterConfiguration(
    private val config: Config,
    private val discoveryClient: DiscoveryClient
) {

    @Bean
    fun subResourceFetcher(): SubResourceFetcher = K8sSubResourceFetcher(discoveryClient, WebClient.builder())

    @Bean
    fun composedRoutes() =
        RouterFunctions.route(
            RequestPredicates.GET("/${config.resource.name}/{id}"),
            DetailResourceHandler(config.resource, subResourceFetcher())
        ).and(
            RouterFunctions.route(
                RequestPredicates.GET("/${config.resource.name}"),
                ListResourceHandler(config)
            )
        )
}
