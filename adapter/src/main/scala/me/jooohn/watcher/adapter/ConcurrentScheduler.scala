package me.jooohn.watcher.adapter

import java.time.Instant
import scala.concurrent.duration.MILLISECONDS

import cats.{Functor, Monad}
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.watcher.adapter.ConcurrentScheduler._
import me.jooohn.watcher.domain.Watcher
import me.jooohn.watcher.port.Scheduler

final class ConcurrentScheduler[F[_]: Sync: Concurrent: Timer](
    tasksRef: TasksRef[F],
    logger: Logger[F],
) extends Scheduler[F] {
  implicit val classLogger: Logger[F] = logger.withModifiedString(message => s"$getClass: $message")

  override def schedule(watchers: List[Watcher[F]]): F[Unit] = tasksRef.replace(watchers.scheduleAll)
}

object ConcurrentScheduler {
  final type Task[F[_]] = Fiber[F, Unit]
  final type TasksRef[F[_]] = Ref[F, List[Task[F]]]

  implicit class WatchersOps[F[_]: Concurrent: Timer: Logger](watchers: List[Watcher[F]]) {

    def scheduleAll: F[List[Fiber[F, Unit]]] = watchers.traverse(_.schedule)

  }

  implicit class WatcherOps[F[_]: Concurrent: Timer: Logger](watcher: Watcher[F]) {

    private type LastSnapshot = Option[watcher.SubjectSnapshot]

    def schedule: F[Fiber[F, Unit]] = Concurrent[F].start(loop(None))

    private def loop(lastSnapshot: LastSnapshot): F[Unit] =
      for {
        next <- step(lastSnapshot)
        _ <- Timer[F].sleep(watcher.interval)
        _ <- loop(next)
      } yield ()

    private def step(lastSnapshot: LastSnapshot): F[LastSnapshot] =
      for {
        _ <- Logger[F].info(s"checking source")
        now <- Timer[F].nowF
        result <- watcher.check(now, lastSnapshot).attempt
        next <- result.fold[F[Option[watcher.SubjectSnapshot]]](
          e => Logger[F].error(e)(s"check failed.") map (_ => lastSnapshot),
          s => Logger[F].info(s"check succeeded.") map (_ => Some(s))
        )
      } yield next

  }

  implicit class TimerOps[F[_]: Functor](timer: Timer[F]) {

    def nowF: F[Instant] = timer.clock.monotonic(MILLISECONDS).map(Instant.ofEpochMilli)

  }

  implicit class TasksRefOps[F[_]: Logger: Monad](tasksRef: TasksRef[F]) {

    def replace(newTasksF: F[List[Task[F]]]): F[Unit] =
      for {
        _ <- tasksRef.cleanup
        _ <- Logger[F].info("scheduling new tasks.")
        nextTasks <- newTasksF
        _ <- Logger[F].info("new tasks scheduled.")
        _ <- tasksRef.set(nextTasks)
      } yield ()

    def cleanup: F[Unit] =
      tasksRef.get flatMap {
        case Nil => Monad[F].unit
        case currentTasks =>
          Logger[F].info("cleaning up running tasks.") >> currentTasks.traverse(task => task.cancel) >> Monad[F].unit
      }

  }
}
