package me.jooohn.watcher.plugins.source.html

import cats.Monad
import io.circe.generic.semiauto.deriveDecoder
import cats.effect.Sync
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.circe.{Decoder, Json}
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.plugins.{Decode, PluginName, SourcePlugin}
import me.jooohn.watcher.port.BuildResult
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import org.http4s.Uri

trait Html[F[_], A] {

  final def html(
      implicit S: Sync[F],
      L: Logger[F],
      Decode: Decode[A, HtmlParams]): SourcePlugin[F, A] =
    new SourcePlugin[F, A] {

      override def name: PluginName = PluginName("html")

      override def load(definition: A): BuildResult[Source[F]] =
        Decode.decode(definition) map { attributes =>
          new HtmlSource(
            attributes = attributes
          )
        }

    }

}

class HtmlSource[F[_]: Sync: Logger](
    attributes: HtmlParams
) extends Source[F] {
  import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
  import net.ruippeixotog.scalascraper.dsl.DSL._

  override type Subject = String

  val browser: Browser = JsoupBrowser()

  override def observe: F[String] =
    for {
      doc <- Sync[F].delay(browser.get(attributes.uri.toString()))
      t = doc >> text(attributes.selector.getOrElse(""))
      _ <- Logger[F].info(s"url: ${attributes.uri
        .toString()}, selector: ${attributes.selector.getOrElse("")}, text: $t")
    } yield t

  override def isChanged(a: String, b: String): F[Boolean] =
    Monad[F].pure(a != b)

  override def parameterize(subject: Subject): Json =
    Json.fromString(subject)
}

case class HtmlParams(
    uri: Uri,
    selector: Option[String],
)
object HtmlParams {
  import me.jooohn.watcher.plugins._
  implicit def jsonDecode: Decoder[HtmlParams] =
    deriveDecoder[HtmlParams]
}
