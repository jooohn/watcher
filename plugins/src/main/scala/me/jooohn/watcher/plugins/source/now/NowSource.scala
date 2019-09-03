package me.jooohn.watcher.plugins.source.now

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

import cats.effect.Clock
import cats.syntax.all._
import cats.{Monad, MonadError}
import io.chrisdavenport.log4cats.Logger
import io.circe.Json
import me.jooohn.watcher.domain.Source

class NowSource[F[_]: MonadError[?[_], Throwable]: Clock: Logger]
    extends Source[F] {

  override type Subject = Instant

  override def observe: F[Subject] =
    for {
      milli <- Clock[F].realTime(TimeUnit.MILLISECONDS)
      instant = Instant.ofEpochMilli(milli)
      _ <- Logger[F].info(s"It's ${instant.toString} now.")
    } yield instant

  override def isChanged(a: Subject, b: Subject): F[Boolean] =
    Monad[F].pure(a.compareTo(b) != 0)

  override def parameterize(subject: Instant): Json =
    Json.obj(
      "iso" -> Json.fromString(DateTimeFormatter.ISO_INSTANT.format(subject)),
    )
}
