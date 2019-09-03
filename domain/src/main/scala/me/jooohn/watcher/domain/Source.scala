package me.jooohn.watcher.domain

import io.circe.Json

trait Source[F[_]] {

  type Subject

  def observe: F[Subject]

  def isChanged(a: Subject, b: Subject): F[Boolean]

  def parameterize(subject: Subject): Json

}
