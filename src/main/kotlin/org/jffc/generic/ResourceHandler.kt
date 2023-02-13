package org.jffc.generic

import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ResourceHandler(val resource: Resource, val subResourceFetcher: SubResourceFetcher) : HandlerFunction<ServerResponse> {


    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id")
        val seedData = resource.seed[id]
        return if (null != seedData) {
            val body = mapOf<String, Any>("id" to id).plus(seedData)
            val embedded = Flux.concat(resource.embedded.map { subResourceFetcher.find(it.service, it.request, body) })

            ServerResponse.ok().body(BodyInserters.fromValue(body + mapOf<String, Any>("_embedded" to embedded)))
        } else {
            ServerResponse.notFound().build()
        }
    }
}




