package com.xebia.experiment;

import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import java.net.BindException


suspend fun main() {
    val vertx = Vertx.vertx()

    val router = Router.router(vertx);

    router.route().path("/").handler { routingContext ->
        // This handler will be called for every request
        val response = routingContext.response();
        response.putHeader("content-type", "text/plain");

        // Write to the response and end it
        response.end("Hello World from vertx");
    };

    val port = 8080
    try {
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listenAwait(port)

        println("Listening http://localhost:$port/")
    } catch (e: BindException) {
        System.err.println("Failed to listen on port $port")
        vertx.closeAwait()
    }
}
