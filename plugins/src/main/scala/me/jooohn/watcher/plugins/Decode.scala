package me.jooohn.watcher.plugins

import cats.data.NonEmptyList
import cats.implicits._
import io.circe.{Decoder, Json}
import me.jooohn.watcher.port.InvalidDefinition

trait Decode[From, To] {

  def decode(from: From): Either[NonEmptyList[InvalidDefinition], To]

}

object Decode {

  def apply[From, To](implicit D: Decode[From, To]): Decode[From, To] = D

  implicit def jsonDecode[To: Decoder]: Decode[Json, To] =
    from => Decoder[To].decodeJson(from).fold(
      failure => NonEmptyList.of(InvalidDefinition(failure.getMessage())).asLeft,
      Right.apply
    )

}
