import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers.elastic
import reactor.netty.tcp.TcpServer
import java.nio.charset.Charset
import java.time.Duration
import kotlin.random.Random

fun main(args: Array<String>) {
    val server = TcpServer.create()
        .host("localhost")
        .port(8080)
        .handle { inbound, outbound ->
            inbound
                .receive()
                .map { it.toString(Charset.defaultCharset()) }
                .zipWith(Flux
                    .interval(Duration.ofMillis(200L))
                    .map { Random.nextInt(10) })
                .groupBy { it.t2 }
                .flatMap { it ->
                    outbound.sendString(it
                        .take(Duration.ofSeconds(1L))
                        .count()
                        .map { "Processed $it elements/sec\n" }
                    ).then()
                }
                .subscribeOn(elastic())
                .then()
        }
        .bindNow()

    server.onDispose()
        .block();
}
