package me.jooohn.watcher.plugins.source.screenshot

import java.awt.image.BufferedImage

import cats.syntax.all._
import cats.MonadError
import io.chrisdavenport.log4cats.Logger
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.plugins.source.Sources
import me.jooohn.watcher.plugins.{Decode, PluginName, SourcePlugin, Tolerance}
import me.jooohn.watcher.port.BuildResult

trait Screenshot[F[_], A] {

  final def screenshot(
      implicit M: MonadError[F, Throwable],
      L: Logger[F],
      Decode: Decode[A, ScreenshotAttributes],
      screenshotTaker: ScreenshotTaker[F]): SourcePlugin[F, A] =
    new SourcePlugin[F, A] {

      val logger: Logger[F] = L.withModifiedString(s => s"Screenshot: $s")

      override def name: PluginName = PluginName("screenshot")

      override def load(definition: A): BuildResult[Source[F]] =
        Decode.decode(definition) map { attributes =>
          implicit val tolerance: Tolerance[BufferedImage] =
            attributes.tolerance
          Sources.fromObserve {
            for {
              _ <- logger.info(s"take screenshot for url (${attributes.uri})")
              subject <- screenshotTaker.take(
                ScreenshotRequest(attributes.uri, attributes.selector))
            } yield subject
          }
        }
    }

}
