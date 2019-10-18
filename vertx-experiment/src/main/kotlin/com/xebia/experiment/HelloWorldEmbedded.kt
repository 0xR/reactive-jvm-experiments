package com.xebia.experiment;

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.reactivestreams.ReactiveReadStream
import io.vertx.ext.reactivestreams.ReactiveWriteStream
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.net.listenAwait
import java.net.BindException
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.Arrays
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.toJavaDuration


@ExperimentalTime
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

    tcpServer(vertx);
}

@ExperimentalTime
suspend fun tcpServer(vertx: Vertx) {
    val server = vertx.createNetServer();
    server.connectHandler { socket ->
        val writeStream = ReactiveWriteStream.writeStream<Buffer>(vertx)
        socket.pipeTo(writeStream);
        Flux.from(writeStream)
            .delayElements(1000.milliseconds.toJavaDuration())
            .subscribe({
                println("Got ${it.length()} bytes");
            })

    };
    server.listenAwait(8090, "localhost");
}
