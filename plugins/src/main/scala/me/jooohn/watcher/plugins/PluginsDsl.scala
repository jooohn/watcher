package me.jooohn.watcher.plugins

import cats.Monad
import me.jooohn.watcher.domain.UUIDIssuer
import me.jooohn.watcher.plugins.sink.Sinks
import me.jooohn.watcher.plugins.source.Sources

trait PluginsDsl[F[_], A] {

  val sinkPlugins: Sinks[F, A] = new Sinks[F, A] {}
  val sourcePlugins: Sources[F, A] = new Sources[F, A] {}

  def rootDecode: Decode[A, List[WatcherDefinition[A]]]

  def watcherBuilder(implicit M: Monad[F],
                     U: UUIDIssuer[F]): PluginWatcherBuilder[F, A] =
    PluginWatcherBuilderImpl(
      sourcePlugins = Nil,
      sinkPlugins = Nil,
      rootBuilder = rootDecode.decode
    )

}

object PluginsDsl {

  def apply[F[_], A](
    implicit D: Decode[A, List[WatcherDefinition[A]]]
  ): PluginsDsl[F, A] =
    new PluginsDsl[F, A] {

      override def rootDecode: Decode[A, List[WatcherDefinition[A]]] = D

    }

}
