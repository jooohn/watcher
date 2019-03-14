package me.jooohn.watcher.plugins.helper

import cats.effect.ConcurrentEffect
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.circe.Json
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Method, Uri}

import scala.concurrent.ExecutionContext

class HandlebarsRenderer[F[_]: ConcurrentEffect: Logger](uri: Uri)(
    implicit ex: ExecutionContext)
    extends Renderer[F]
    with Http4sClientDsl[F] {

  override def render(template: String, data: Json): F[String] = {
    BlazeClientBuilder[F](ex).resource.use { client =>
      for {
        request <- Method.POST.apply(
          body =
            Json.obj("template" -> Json.fromString(template), "data" -> data),
          uri = uri,
        )
        rendered <- client.expect[String](request)
      } yield rendered
    }
  }

}
