package org.jffc.generic

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient

@ContextConfiguration(classes = [RouterConfiguration::class])
@WebFluxTest
class KubernetesTestAppApplicationTests {

    private lateinit var webTestClient: WebTestClient
    private lateinit var wireMockServer: WireMockServer

    @Autowired
    private lateinit var applicationContest: ApplicationContext

    private val reader = ObjectMapper().readerFor(Map::class.java)
    private val setReader = ObjectMapper().readerFor(Set::class.java)

    @MockBean
    private lateinit var discoveryClient: DiscoveryClient

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient
            .bindToApplicationContext(applicationContest)
            .build()

        wireMockServer = WireMockServer(8090)
        wireMockServer.start()
    }

    @AfterEach
    fun tearDown() = wireMockServer.stop()

    @Test
    fun `should load resource by id without sub resources`() {
        webTestClient.get()
            .uri("/countries/NC")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = reader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(mapOf("id" to "NC", "description" to "No Country"))
            }
    }

    @Test
    fun `should load resource by id wit sub resources`() {
        whenever(discoveryClient.getInstances(eq("coins"))).thenReturn(listOf(mockServiceInstance("coins")))
        whenever(discoveryClient.getInstances(eq("languages"))).thenReturn(listOf(mockServiceInstance("languages")))

        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo("/coins")).withQueryParam("country", equalTo("AC"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("content-type", "application/json")
                        .withBody(
                            """
                    [{
                     "id": "BTC",
                     "description": "Bitcoin"
                    }]
                            """.trimIndent()
                        )

                )
        )
        wireMockServer.stubFor(
            WireMock.get(urlPathEqualTo("/languages")).withQueryParam("country", equalTo("AC")).willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("content-type", "application/json")
                    .withBody(
                        """
                    [{
                     "id": "ESP",
                     "description": "Esperanto"
                    }]
                        """.trimIndent()
                    )

            )
        )

        webTestClient.get()
            .uri("/countries/AC")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = reader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(
                    mapOf(
                        "id" to "AC",
                        "description" to "A Country",
                        "_embedded" to mapOf<String, Any>(
                            "coins" to listOf(mapOf("description" to "Bitcoin", "id" to "BTC")),
                            "languages" to listOf(mapOf<String, Any>("description" to "Esperanto", "id" to "ESP"))
                        )
                    )
                )
            }
    }

    @Test
    fun `should list resources`() {
        webTestClient.get()
            .uri("/countries")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = setReader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(
                    setOf(
                        mapOf("id" to "AC", "description" to "A Country"),
                        mapOf("id" to "ANC", "description" to "Another country with meta"),
                        mapOf("id" to "NC", "description" to "No Country")
                    )
                )
            }
    }

    @Test
    fun `should list resources with query param`() {
        webTestClient.get()
            .uri {
                    uri ->
                uri.path("/countries")
                    .queryParam("description", "A Country")
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = setReader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(
                    setOf(
                        mapOf("id" to "AC", "description" to "A Country")
                    )
                )
            }
    }

    @Test
    fun `should list resources with query param (meta)`() {
        webTestClient.get()
            .uri {
                    uri ->
                uri.path("/countries")
                    .queryParam("coin", "BTC")
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = setReader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(
                    setOf(
                        mapOf("id" to "ANC", "description" to "Another country with meta")
                    )
                )
            }
    }

    @Test
    fun `should not load resource not configured`() {
        webTestClient.get()
            .uri("/countries/NR")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `should load resource by id with sub resources`() {
        webTestClient.get()
            .uri("/countries/AC")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .consumeWith { result ->
                val result = reader.readValue<Any>(result.responseBody)
                assertThat(result).isEqualTo(mapOf("id" to "AC", "description" to "A Country"))
            }
    }

    fun mockServiceInstance(serviceName: String): ServiceInstance = DefaultServiceInstance(
        "${System.currentTimeMillis()}",
        serviceName,
        "localhost",
        8090,
        false
    )

    inline fun <T> whenever(methodCall: T): OngoingStubbing<T> {
        return Mockito.`when`(methodCall)!!
    }
}
