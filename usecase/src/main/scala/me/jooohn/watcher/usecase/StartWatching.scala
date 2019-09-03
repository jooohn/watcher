package me.jooohn.watcher.usecase

import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import me.jooohn.watcher.domain.Watcher
import me.jooohn.watcher.port.{InvalidDefinition, Scheduler, WatcherBuilder}

case class StartWatching[F[_]: Monad, A](
    scheduler: Scheduler[F],
    watcherBuilder: WatcherBuilder[F, A]
) {

  def execute(definition: A): F[Either[ScheduleWatchError, Unit]] = {
    type Result[B] = EitherT[F, ScheduleWatchError, B]

    def buildWatchers: EitherT[F, ScheduleWatchError, List[Watcher[F]]] =
      EitherT(watcherBuilder.build(definition))
        .leftMap(new InvalidDefinitionError(_))

    def schedule(
        watchers: List[Watcher[F]]
    ): EitherT[F, ScheduleWatchError, Unit] =
      EitherT.right(scheduler.schedule(watchers))

    (for {
      watchers <- buildWatchers
      _ <- schedule(watchers)
    } yield ()).value
  }

}

sealed abstract class ScheduleWatchError(val message: String)
class InvalidDefinitionError(errors: NonEmptyList[InvalidDefinition])
    extends ScheduleWatchError(errors.map(_.message).toList.mkString("\n"))
