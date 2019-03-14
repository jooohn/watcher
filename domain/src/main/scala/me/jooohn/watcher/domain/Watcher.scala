package me.jooohn.watcher.domain

import java.time.Instant

import cats.syntax.all._
import cats.Monad

final case class Watcher[F[_]: Monad: UUIDIssuer](source: Source[F],
                                                  sink: Sink[F]) {
  type SubjectSnapshot = Snapshot[source.Subject]

  private val unit: F[Unit] = Monad[F].pure(())

  def check(now: Instant, prev: Option[SubjectSnapshot]): F[SubjectSnapshot] = {
    for {
      currentSnapshot <- takeCurrentSnapshot(now)
      _ <- onSnapshotTaken(prev, currentSnapshot)
    } yield currentSnapshot
  }

  private def takeCurrentSnapshot(now: Instant): F[SubjectSnapshot] =
    for {
      id <- UUIDIssuer[F].issue
      subject <- source.observe
    } yield Snapshot(Snapshot.Id(id.toString), now, subject)

  private def onSnapshotTaken(maybePrevSnapshot: Option[SubjectSnapshot],
                              currentSnapshot: SubjectSnapshot): F[Unit] =
    maybePrevSnapshot.fold(unit) { prevSnapshot =>
      source.isChanged(prevSnapshot.subject, currentSnapshot.subject) flatMap {
        case true  => onSubjectChanged(prevSnapshot, currentSnapshot)
        case false => unit
      }
    }

  private def onSubjectChanged(prev: SubjectSnapshot,
                               current: SubjectSnapshot): F[Unit] =
    sink.run(
      SinkContext(
        prev = prev.parameterizeWith(source.parameterize),
        current = current.parameterizeWith(source.parameterize)
      ))

}
