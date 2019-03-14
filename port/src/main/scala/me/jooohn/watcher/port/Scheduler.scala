package me.jooohn.watcher.port

import me.jooohn.watcher.domain.Watcher

trait Scheduler[F[_]] {

  def schedule(watchers: List[Watcher[F]]): F[Unit]

}
