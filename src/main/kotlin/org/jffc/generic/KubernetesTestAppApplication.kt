package org.jffc.generic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KubernetesTestAppApplication

fun main(args: Array<String>) {
    runApplication<KubernetesTestAppApplication>(*args)
}
