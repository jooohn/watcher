package me.jooohn.watcher.domain

import java.time.Instant

import cats.Monad
import cats.syntax.all._

import scala.concurrent.duration.FiniteDuration

final case class Watcher[F[_]: Monad: UUIDIssuer](interval: FiniteDuration, source: Source[F], sink: Sink[F]) {
  final type SubjectSnapshot = Snapshot[source.Subject]

  private val unit: F[Unit] = Monad[F].pure(())

  def check(now: Instant, prev: Option[SubjectSnapshot]): F[SubjectSnapshot] = {
    def takeCurrentSnapshot: F[Snapshot[source.Subject]] =
      for {
        id <- UUIDIssuer[F].issue
        subject <- source.observe
      } yield Snapshot(Snapshot.Id(id.toString), now, subject)

    def onSnapshotTaken(currentSnapshot: SubjectSnapshot): F[Unit] =
      prev.fold(unit) { prevSnapshot =>
        source.isChanged(prevSnapshot.subject, currentSnapshot.subject) flatMap {
          case true  => onSubjectChanged(prevSnapshot, currentSnapshot)
          case false => unit
        }
      }

    def onSubjectChanged(prev: SubjectSnapshot, current: SubjectSnapshot): F[Unit] =
      sink.run(
        SinkContext(
          prev = prev.parameterizeWith(source.parameterize),
          current = current.parameterizeWith(source.parameterize)
        ))

    for {
      currentSnapshot <- takeCurrentSnapshot
      _ <- onSnapshotTaken(currentSnapshot)
    } yield currentSnapshot
  }

}
