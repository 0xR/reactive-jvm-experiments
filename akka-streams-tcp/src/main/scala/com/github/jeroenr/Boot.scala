package com.github.jeroenr

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream.scaladsl.{Flow, Framing, Sink, Source, Tcp}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success}


object Boot extends App {
  implicit val system = ActorSystem("akka-streams-tcp")
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  private val outgoing = Tcp().outgoingConnection(
    "localhost", 8080
  )

  val connections: Source[IncomingConnection, Future[ServerBinding]] =
    Tcp().bind("0.0.0.0", 1337)
  connections.runForeach { connection =>
    println(s"New connection from: ${connection.remoteAddress}")

    val pipeFlow = Flow[ByteString]
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
      .map(_.utf8String)
      .alsoTo(Sink.foreach(println))
        .map(ByteString.apply)
        .alsoTo(outgoing.to(Sink.ignore))
    connection.handleWith(pipeFlow)
  }.onComplete {
    case Success(_) => println("Done!")
    case Failure(t) => println(s"Boom! $t")
  }


}
