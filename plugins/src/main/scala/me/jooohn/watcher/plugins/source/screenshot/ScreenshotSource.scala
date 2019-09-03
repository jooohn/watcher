package me.jooohn.watcher.plugins.source.screenshot

import java.awt.image.BufferedImage

import cats.MonadError
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.circe.Json
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.infrastructure._
import me.jooohn.watcher.plugins.Similarity

final class ScreenshotSource[F[_]: Logger: MonadError[?[_], Throwable]](
    screenshotTaker: ScreenshotTaker[F],
    attributes: ScreenshotParams,
) extends Source[F] {

  override type Subject = BufferedImage

  override def observe: F[Subject] =
    for {
      _ <- Logger[F].info(s"take screenshot for url (${attributes.uri})")
      subject <- screenshotTaker.take(
        ScreenshotRequest(attributes.uri, attributes.selector))
    } yield subject

  override def isChanged(a: Subject, b: Subject): F[Boolean] =
    MonadError[F, Throwable].fromTry(
      a.similarityTo(b).map(Similarity).map(attributes.tolerance.isSimilar))

  override def parameterize(subject: Subject): Json =
    Json.Null
}
