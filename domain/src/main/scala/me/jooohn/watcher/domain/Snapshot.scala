package me.jooohn.watcher.domain

import java.time.Instant

import cats.syntax.functor._
import cats.Functor
import io.circe.{Decoder, Encoder, Json}

final case class Snapshot[A](id: Snapshot.Id, takenAt: Instant, subject: A) {

  def parameterizeWith(f: A => Json): Snapshot.Parameterized = Snapshot[Json](id, takenAt, f(subject))

}

object Snapshot {

  def issueNewId[F[_]: UUIDIssuer: Functor]: F[Id] =
    implicitly[UUIDIssuer[F]].issue.map(uuid => Id(uuid.toString))

  case class Id(value: String) {

    override def toString: String = value

  }
  object Id {

    implicit val idEncoder: Encoder[Id] = Encoder[String].contramap(_.value)
    implicit val idDecoder: Decoder[Id] = Decoder[String].map(Id.apply)

  }

  type Parameterized = Snapshot[Json]

}
