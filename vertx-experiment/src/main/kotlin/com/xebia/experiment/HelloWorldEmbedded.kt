package com.xebia.experiment;

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.ReadStream
import io.vertx.core.streams.WriteStream
import io.vertx.ext.reactivestreams.ReactiveReadStream
import io.vertx.ext.reactivestreams.ReactiveWriteStream
import io.vertx.kotlin.core.net.listenAwait
import reactor.core.publisher.Flux
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration
import io.vertx.kotlin.core.Vertx as VertxKt


@ExperimentalTime
suspend fun main() {
    val vertx = VertxKt.clusteredVertxAwait(VertxOptions());

    val eventBus = vertx.eventBus()

    val publishAdress = "publish.all"
    eventBus.consumer<Buffer>(publishAdress).handler {
        val timeString = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        println("[$timeString] EventBus handler got buffer length: ${it.body().length()}")
    }
    val eventBusPublisher = eventBus.publisher<Buffer>(publishAdress)

    val server = vertx.createNetServer();
    server.connectHandler { socket ->
        processWithReactor(vertx, socket, eventBusPublisher) { flux ->
            val shouldDelayFlux = Flux.interval(10.seconds.toJavaDuration()).map {
                it % 2 == 0L
            }.startWith(false)

            shouldDelayFlux.switchMap { shouldDelay ->
                if (shouldDelay) flux.delayElements(1000.milliseconds.toJavaDuration()) else flux
            }
        }
    };

    val netServer = server.listenAwait(8090, "localhost")
    println("Listening on tcp://localhost:${netServer.actualPort()}")
}


fun <Input, Output> processWithReactor(
    vertx: Vertx,
    inputSteam: ReadStream<Input>,
    outputStream: WriteStream<Output>,
    processor: (Flux<Input>) -> Flux<Output>
) {
    val reactiveWriteStream = ReactiveWriteStream.writeStream<Input>(vertx)
    inputSteam.pipeTo(reactiveWriteStream)

    val writeStreamFlux = Flux.from(reactiveWriteStream)
    val processedFlux = processor(writeStreamFlux)

    val reactiveReadStream = ReactiveReadStream.readStream<Output>()
    processedFlux.subscribe(reactiveReadStream)

    reactiveReadStream.pipeTo(outputStream)
}
