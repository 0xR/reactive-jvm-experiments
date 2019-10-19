package com.xebia.experiment;

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.ext.reactivestreams.ReactiveWriteStream
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.closeAwait
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.core.net.listenAwait
import reactor.core.publisher.Flux
import java.net.BindException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds
import kotlin.time.toJavaDuration
import io.vertx.kotlin.core.Vertx as VertxKt


@ExperimentalTime
suspend fun main() {
    val vertx = VertxKt.clusteredVertxAwait(VertxOptions());

    val server = vertx.createNetServer();
    server.connectHandler { socket ->
        val writeStream = ReactiveWriteStream.writeStream<Buffer>(vertx)
        socket.pipeTo(writeStream)
        val writeStreamFlux = Flux.from(writeStream)

        val shouldDelayFlux = Flux.interval(3.seconds.toJavaDuration()).map {
            it % 2 == 0L
        }.startWith(false)

        val subscription = shouldDelayFlux.switchMap { shouldDelay ->
            if (shouldDelay) writeStreamFlux.delayElements(1000.milliseconds.toJavaDuration()) else writeStreamFlux
        }
            .subscribe {
                val timeString = LocalDateTime.now().format(ISO_LOCAL_TIME)
                println("[$timeString] Got ${it.length()} bytes ")
            }

        socket.closeHandler {
            subscription.dispose()
        }
    };

    val netServer = server.listenAwait(0, "localhost")
    println("Listening on tcp://localhost:${netServer.actualPort()}")


    val eventBus = vertx.eventBus()

    val publishAdress = "publish.all"
    eventBus.consumer<String>(publishAdress).handler {
        println("EventBus handler got: ${it.body()}")
    }
    eventBus.publish(publishAdress, "Hi all!")
//
//    eventBus.consumer<String>()
//
//    ReactiveWriteStream
//    ReadStream
//
//    Flux.interval(1.seconds.toJavaDuration())
//
//    Pump.pump
}
