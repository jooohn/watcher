package me.jooohn.watcher.plugins.source

import java.awt.image.BufferedImage

import cats.MonadError
import me.jooohn.watcher.infrastructure._
import io.circe.Json
import me.jooohn.watcher.infrastructure.Serialized
import me.jooohn.watcher.infrastructure.Serialized.ByteArray
import me.jooohn.watcher.plugins.{Similarity, Tolerance}

trait Subject[F[_], A] {

  def isChanged(a: A, b: A): F[Boolean]

  def encode(subject: A): F[Serialized]

  def decode(serialized: Serialized): F[A]

  def parameterize(subject: A): Json

}

object Subject {

  implicit def bufferedImageSubject[F[_]](
      implicit M: MonadError[F, Throwable],
      tolerance: Tolerance[BufferedImage]
  ): Subject[F, BufferedImage] =
    new Subject[F, BufferedImage] {

      override def isChanged(a: BufferedImage, b: BufferedImage): F[Boolean] =
        M.fromTry(a.similarityTo(b).map(Similarity).map(tolerance.isSimilar))

      override def encode(subject: BufferedImage): F[Serialized] =
        M.fromTry(subject.toByteArray.map(Serialized.ByteArray))

      override def decode(serialized: Serialized): F[BufferedImage] =
        serialized match {
          case ByteArray(value) => M.fromTry(value.toBufferedImage)
          case _ =>
            M.raiseError(
              new IllegalStateException(
                "Buffered Image should be decoded from ByteArray."))
        }

      override def parameterize(subject: BufferedImage): Json = Json.Null

    }

}
