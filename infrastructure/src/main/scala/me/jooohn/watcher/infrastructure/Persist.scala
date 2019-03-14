package me.jooohn.watcher.infrastructure

import java.awt.image.BufferedImage

import cats.syntax.functor._
import cats.{Functor, Invariant, MonadError}

import scala.util.Failure

trait Persist[F[_], A] {

  def serialize(value: A): F[Serialized]

  def deserialize(serialized: Serialized): F[A]

}

object Persist {

  def apply[F[_], A](implicit P: Persist[F, A]): Persist[F, A] = P

  implicit def serializeInvariant[F[_]: Functor]
    : Invariant[λ[x => Persist[F, x]]] =
    new Invariant[λ[x => Persist[F, x]]] {
      override def imap[A, B](fa: Persist[F, A])(f: A => B)(
          g: B => A): Persist[F, B] =
        new Persist[F, B] {

          override def serialize(value: B): F[Serialized] =
            fa.serialize(g(value))

          override def deserialize(serialized: Serialized): F[B] =
            fa.deserialize(serialized).map(f)
        }
    }

  implicit def bufferedImageSerialize[F[_]](
      implicit M: MonadError[F, Throwable]): Persist[F, BufferedImage] =
    new Persist[F, BufferedImage] {

      override def serialize(value: BufferedImage): F[Serialized] =
        M.fromTry(value.toByteArray.map(Serialized.ByteArray))

      override def deserialize(serialized: Serialized): F[BufferedImage] =
        M.fromTry(
          serialized match {
            case Serialized.ByteArray(byteArray) => byteArray.toBufferedImage
            case _                               => Failure(new Exception("Failed to read image"))
          }
        )

    }

}
