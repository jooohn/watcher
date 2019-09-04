package me.jooohn.watcher.adapter

import java.time.Instant
import java.util.concurrent.TimeUnit

import cats.Monad
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import me.jooohn.watcher.adapter.ConcurrentScheduler._
import me.jooohn.watcher.domain.Watcher
import me.jooohn.watcher.port.Scheduler

final class ConcurrentScheduler[F[_]: Sync: Concurrent: Timer: Logger](tasksRef: TasksRef[F]) extends Scheduler[F] {

  val timer: Timer[F] = Timer[F]

  val logger: Logger[F] =
    Logger[F].withModifiedString(message => s"$getClass: $message")

  override def schedule(watchers: List[Watcher[F]]): F[Unit] = tasksRef.replace(watchers.scheduleAll)

  implicit class WatcherOps(watcher: Watcher[F]) {

    type LastSnapshot = Option[watcher.SubjectSnapshot]

    def schedule: F[Fiber[F, Unit]] = Concurrent[F].start(loop(None))

    def loop(lastSnapshot: LastSnapshot): F[Unit] =
      for {
        next <- step(lastSnapshot)
        _ <- timer.sleep(watcher.interval)
        _ <- loop(next)
      } yield ()

    def step(lastSnapshot: LastSnapshot): F[LastSnapshot] =
      for {
        _ <- logger.info(s"checking source")
        now <- timer.nowF
        result <- watcher.check(now, lastSnapshot).attempt
        next <- result.fold[F[Option[watcher.SubjectSnapshot]]](
          e => logger.error(e)(s"check failed.") map (_ => lastSnapshot),
          s => logger.info(s"check succeeded.") map (_ => Some(s))
        )
      } yield next

  }

  implicit class WatchersOps(watchers: List[Watcher[F]]) {

    def scheduleAll: F[List[Fiber[F, Unit]]] = watchers.traverse(_.schedule)

  }

  implicit class TimerOps(timer: Timer[F]) {

    def nowF: F[Instant] =
      timer.clock.monotonic(TimeUnit.MILLISECONDS).map(Instant.ofEpochMilli)

  }

  implicit class TasksRefOps(tasksRef: TasksRef[F]) {

    def replace(newTasksF: F[List[Task[F]]]): F[Unit] =
      for {
        _ <- tasksRef.cleanup
        _ <- logger.info("scheduling new tasks.")
        nextTasks <- newTasksF
        _ <- logger.info("new tasks scheduled.")
        _ <- tasksRef.set(nextTasks)
      } yield ()

    def cleanup: F[Unit] =
      tasksRef.get flatMap {
        case Nil => Monad[F].unit
        case currentTasks =>
          logger.info("cleaning up running tasks.") >> currentTasks.traverse(task => task.cancel)
      }

  }
}

object ConcurrentScheduler {
  final type Task[F] = Fiber[F, Unit]
  final type TasksRef[F[_]] = Ref[F, List[Task[F]]]
}
