package me.jooohn.watcher.plugins.source.now

import cats.MonadError
import cats.effect.Clock
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.circe.Json
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.plugins.{PluginName, SourcePlugin}
import me.jooohn.watcher.port.{BuildResult, InvalidDefinition}

// This SourcePlugin is convenient for debugging.
trait Now[F[_], A] {

  final def now(
      implicit L: Logger[F],
      M: MonadError[F, Throwable],
      C: Clock[F]): SourcePlugin[F, A] =
    new SourcePlugin[F, A] {

      val logger: Logger[F] = L.withModifiedString(s => s"Now: $s")

      override def name: PluginName = PluginName("now")

      override def load(definition: A): BuildResult[Source[F]] =
        definition match {
          case Json.Null =>
            Right(new NowSource[F])
          case _ =>
            InvalidDefinition("""plugin "now" does not take parameters.""").leftNel
        }
    }

}
