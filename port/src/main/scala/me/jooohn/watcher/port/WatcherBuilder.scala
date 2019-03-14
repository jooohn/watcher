package me.jooohn.watcher.port

import me.jooohn.watcher.domain.Watcher

trait WatcherBuilder[F[_], A] {

  def build(definition: A): F[BuildResult[List[Watcher[F]]]]

}
