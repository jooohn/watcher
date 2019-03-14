package me.jooohn.watcher.plugins.source.screenshot

import java.awt.image.BufferedImage

import cats.effect.ConcurrentEffect
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.circe.generic.auto._
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.syntax._
import me.jooohn.watcher.infrastructure._
import org.http4s.circe._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.{Method, Request, Uri}

import scala.concurrent.ExecutionContext

class ScreenshotTaker[F[_]: Logger](baseUri: Uri)(implicit ex: ExecutionContext,
                                                  F: ConcurrentEffect[F])
    extends Http4sClientDsl[F] {

  val logger: Logger[F] =
    Logger[F].withModifiedString(message => s"ScreenshotTaker: $message")

  def take(request: ScreenshotRequest): F[BufferedImage] = {

    def buildRequest(uri: Uri, selector: Option[String]): F[Request[F]] =
      selector match {
        case None =>
          Method.GET.apply(uri = uri)
        case Some(s) =>
          Method.POST.apply(body = RendertronOptions(s).asJson, uri = uri)
      }

    val uri: Uri = baseUri / "screenshot" / request.uri.toString()
    BlazeClientBuilder[F](ex).resource.use { client =>
      for {
        req <- buildRequest(uri, request.selector)
        _ <- logger.info(s"going to take a screenshot for ${request.uri} via ${uri.toString()}")
        data <- client.expect[Array[Byte]](req)
        bufferedImage <- F.fromEither(data.toBufferedImage.toEither)
        _ <- logger.info(
          s"the screenshot for ${request.uri} was successfully taken")
      } yield bufferedImage
    }
  }

}

private case class RendertronOptions(selector: String)
