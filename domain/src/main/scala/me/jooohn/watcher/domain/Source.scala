package me.jooohn.watcher.domain

import io.circe.Json
import me.jooohn.watcher.infrastructure.Serialized

trait Source[F[_]] {

  type Subject

  def observe: F[Subject]

  def isChanged(a: Subject, b: Subject): F[Boolean]

  def encode(subject: Subject): F[Serialized]

  def decode(serialized: Serialized): F[Subject]

  def parameterize(subject: Subject): Json

}
