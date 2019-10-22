package com.xebia.experiment;

import io.netty.buffer.Unpooled.EMPTY_BUFFER
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.ext.reactivestreams.ReactiveReadStream
import io.vertx.ext.reactivestreams.ReactiveWriteStream
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.net.listenAwait
import reactor.core.publisher.Flux
import io.vertx.kotlin.core.Vertx as VertxKt


suspend fun main(arguments: Array<String>) {
    val vertx = VertxKt.clusteredVertxAwait(VertxOptions());
    runTcpserver(vertx)
    runWebserver(vertx)
}

suspend fun runTcpserver(vertx: Vertx) {
    val eventBus = vertx.eventBus()

    val publishAdress = "publish.all"
    val eventBusPublisher = eventBus.publisher<Buffer>(publishAdress)

    val server = vertx.createNetServer();
    server.connectHandler { socket ->
        socket.pipeTo(eventBusPublisher)
//        processWithReactor(
//            vertx,
//            inputSteam = socket,
//            outputStream = eventBusPublisher
//        ) { socketFlux ->
//            val shouldDelayFlux = Flux.interval(ofSeconds(5)).map {
//                it % 2 == 0L
//            }.startWith(false)
//                .takeUntilOther(socketFlux.takeLast(1))
//
//            shouldDelayFlux.switchMap { shouldDelay ->
//                if (shouldDelay) socketFlux.delayElements(ofSeconds(1)) else socketFlux
//            }
//        }
    };

    val netServer = server.listenAwait(8090, "localhost")
    println("Listening on tcp://localhost:${netServer.actualPort()}!")

}

suspend fun runWebserver(vertx: Vertx) {
    val eventBus = vertx.eventBus()

    val publishAdress = "publish.all"

    val router = Router.router(vertx)

    router.route(GET, "/stream").handler { routingContext ->
        val eventbusReadStream = eventBus.consumer<Buffer>(publishAdress)

        val response = routingContext.response().apply {
            headers().add("content-type", "text/event-stream")
            setStatusCode(200)
            setChunked(true)
            // Flush headers
            write(Buffer.buffer(EMPTY_BUFFER))
        }

        eventbusReadStream.bodyStream().pipeTo(response)
    }

    router.route(GET, "/").handler(StaticHandler.create().apply {
        setCachingEnabled(false)
    })

    val server = vertx
        .createHttpServer()
        .requestHandler(router)
        .listenAwait(8080)

    println("Listening on http://localhost:${server.actualPort()}/")
}

fun <Input, Output> processWithReactor(
    vertx: Vertx,
    inputSteam: ReadStream<Input>,
    outputStream: WriteStream<Output>,
    processor: (Flux<Input>) -> Flux<Output>
) {
    val publisher = ReactiveWriteStream.writeStream<Input>(vertx)
    inputSteam.pipeTo(publisher)

    val writeStreamFlux = Flux.from(publisher)
    val processedFlux = processor(writeStreamFlux)

    val subscriber = ReactiveReadStream.readStream<Output>()
    processedFlux.subscribe(subscriber)

    subscriber.pipeTo(outputStream)
}
