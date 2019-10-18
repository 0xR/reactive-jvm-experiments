import reactor.netty.tcp.TcpServer
import java.nio.charset.Charset

fun main(args: Array<String>) {
    val server = TcpServer.create()
        .host("localhost")
        .port(8080)
        .handle { inbound, _ -> inbound
            .receive()
            .map({ it.toString(Charset.defaultCharset()) })
            .log()
            .then()
        }
        .bindNow()

    server.onDispose()
        .block();
}
