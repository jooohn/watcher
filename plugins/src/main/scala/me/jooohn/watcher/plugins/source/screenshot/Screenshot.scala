package me.jooohn.watcher.plugins.source.screenshot

import cats.MonadError
import io.chrisdavenport.log4cats.Logger
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.plugins.{Decode, PluginName, SourcePlugin}
import me.jooohn.watcher.port.BuildResult

trait Screenshot[F[_], A] {

  final def screenshot(
      implicit M: MonadError[F, Throwable],
      L: Logger[F],
      Decode: Decode[A, ScreenshotParams],
      screenshotTaker: ScreenshotTaker[F]): SourcePlugin[F, A] =
    new SourcePlugin[F, A] {

      val logger: Logger[F] = L.withModifiedString(s => s"Screenshot: $s")

      override def name: PluginName = PluginName("screenshot")

      override def load(definition: A): BuildResult[Source[F]] =
        Decode.decode(definition) map { attributes =>
          new ScreenshotSource(
            screenshotTaker = screenshotTaker,
            attributes = attributes,
          )
        }
    }

}
