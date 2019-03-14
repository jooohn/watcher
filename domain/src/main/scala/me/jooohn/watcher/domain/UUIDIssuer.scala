package me.jooohn.watcher.domain

import java.util.UUID

trait UUIDIssuer[F[_]] {

  def issue: F[UUID]

}
object UUIDIssuer {

  def apply[F[_]](implicit U: UUIDIssuer[F]): UUIDIssuer[F] = U

}
