package me.jooohn.watcher.domain

import java.util.UUID

import cats.{Applicative, Apply, Monad}

trait UUIDIssuer[F[_]] {

  def issue: F[UUID]

}
object UUIDIssuer {

  def apply[F[_]](implicit U: UUIDIssuer[F]): UUIDIssuer[F] = U

  def random[F[_]: Applicative]: UUIDIssuer[F] =
    new UUIDIssuer[F] {
      override def issue: F[UUID] = Applicative[F].pure(UUID.randomUUID())
    }

}
