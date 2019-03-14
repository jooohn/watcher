package me.jooohn.watcher.plugins.source

import io.circe.Json
import me.jooohn.watcher.domain.Source
import me.jooohn.watcher.infrastructure.Serialized
import me.jooohn.watcher.plugins.source.screenshot.Screenshot

trait Sources[F[_], A]
  extends Screenshot[F, A]

object Sources {

  def fromObserve[F[_], A](observe: => F[A])(implicit S: Subject[F, A]): Source[F] = {
    lazy val ob = observe
    new Source[F] {

      override type Subject = A

      override def observe: F[Subject] = ob

      override def isChanged(a: Subject, b: Subject): F[Boolean] = S.isChanged(a, b)

      override def encode(subject: Subject): F[Serialized] = S.encode(subject)

      override def decode(serialized: Serialized): F[Subject] = S.decode(serialized)

      override def parameterize(subject: Subject): Json = S.parameterize(subject)

    }
  }

}
