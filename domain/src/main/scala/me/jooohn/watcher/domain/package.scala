package me.jooohn.watcher

import java.time.Instant

import cats.data.Kleisli
import io.circe.Encoder
import io.circe.generic.extras.Configuration

package object domain {

  type Sink[F[_]] = Kleisli[F, SinkContext, Unit]
  val Sink = Kleisli

  implicit val instantEncoder: Encoder[Instant] = Encoder[String].contramap(_.toString)
  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

}
