package org.jffc.generic

import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class DetailResourceHandler(val resource: Resource, val subResourceFetcher: SubResourceFetcher) :
    HandlerFunction<ServerResponse> {


    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        val seedData = resource.seed[id]
        return if (null != seedData) {
            val body = mapOf<String, Any>("id" to id).plus(seedData)


            Flux.fromIterable(resource.embedded)
                .parallel()
                .flatMap { subResourceFetcher.find(it.service, it.request, body) }
                .sequential()
                .collectList()
                .map {
                    body + (it
                        .takeIf { it.isNotEmpty() }
                    ?.let { mapOf<String, Any>("_embedded" to it.reduce{ t, u -> t + u}) }
                        ?: emptyMap<String,Any>())
                }
                .flatMap {  ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(it)) }

        } else {
            ServerResponse.notFound().build()
        }
    }
}






