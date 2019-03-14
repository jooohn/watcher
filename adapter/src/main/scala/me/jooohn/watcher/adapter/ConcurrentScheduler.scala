package me.jooohn.watcher.adapter

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.watcher.domain.Watcher
import me.jooohn.watcher.port.Scheduler

import scala.concurrent.duration.FiniteDuration

final class ConcurrentScheduler[F[_]: Sync: Concurrent: Timer: Logger](tasksRef: Ref[F, List[Fiber[F, Unit]]])
    extends Scheduler[F] {

  val logger: Logger[F] =
    Logger[F].withModifiedString(message => s"$getClass: $message")

  val duration: FiniteDuration = FiniteDuration(15, TimeUnit.SECONDS)

  override def schedule(watchers: List[Watcher[F]]): F[Unit] = {
    def watch(watcher: Watcher[F]) = {
      def currentInstant: F[Instant] =
        for {
          millis <- Timer[F].clock.monotonic(TimeUnit.MILLISECONDS)
          instant <- Sync[F].delay(Instant.ofEpochMilli(millis))
        } yield instant

      def attempt(prev: Option[watcher.SubjectSnapshot])
        : F[Either[Throwable, watcher.SubjectSnapshot]] =
        (for {
          now <- currentInstant
          currentSnapshot <- watcher.check(now, prev)
        } yield currentSnapshot).attempt

      def loop(prev: Option[watcher.SubjectSnapshot]): F[Unit] =
        for {
          _ <- logger.info(s"going to check source")
          result <- attempt(prev)
          next <- result match {
            case Left(e) =>
              logger.error(e)(s"failed to check") >> Sync[F].pure(prev)
            case Right(s) =>
              logger.info(s"successfully checked source") >> Sync[F].pure(
                Some(s))
          }
          _ <- Timer[F].sleep(duration)
          _ <- loop(next)
        } yield ()
      Concurrent[F].start(loop(None))
    }

    for {
      tasks <- tasksRef.get
      _ <- tasks.traverse(task => task.cancel: F[Unit])
      _ <- logger.info("schedule")
      fibers <- watchers.traverse(watch)
      _ <- tasksRef.set(fibers)
    } yield ()
  }

}
