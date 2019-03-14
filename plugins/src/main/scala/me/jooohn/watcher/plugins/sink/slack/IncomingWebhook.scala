package me.jooohn.watcher.plugins.sink.slack

import cats.effect.ConcurrentEffect
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Method, Request, Uri}

import scala.concurrent.ExecutionContext

class IncomingWebhook[F[_]: Logger: ConcurrentEffect](
    implicit ex: ExecutionContext)
    extends Http4sClientDsl[F] {

  val logger: Logger[F] =
    Logger[F].withModifiedString(message => s"$getClass: $message")

  def post(uri: Uri, payload: Payload): F[Unit] = {
    def buildRequest(uri: Uri): F[Request[F]] = {
      for {
        _ <- logger.info(s"request body: ${payload.asJson.noSpaces}")
        req <- Method.POST.apply(body = payload.asJson, uri = uri)
      } yield req
    }
    BlazeClientBuilder[F](ex).resource.use { client =>
      for {
        req <- buildRequest(uri)
        _ <- logger.info(s"going to send slack incoming webhook")
        response <- client.expect[String](req)
        _ <- logger.info(s"response: $response")
      } yield ()
    }
  }

}

case class Payload(
  channel: Option[String],
  username: Option[String],
  text: String,
  iconEmoji: Option[String],
  iconUrl: Option[String]
)
