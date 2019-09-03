package me.jooohn.watcher.plugins

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits._
import me.jooohn.watcher.domain.{UUIDIssuer, Watcher}
import me.jooohn.watcher.port._

private[plugins] trait PluginWatcherBuilder[F[_], D]
    extends WatcherBuilder[F, D] {

  def use(sourcePlugin: SourcePlugin[F, D]): PluginWatcherBuilder[F, D]

  def use(sinkPlugin: SinkPlugin[F, D]): PluginWatcherBuilder[F, D]

}

final private[plugins] case class PluginWatcherBuilderImpl[
    F[_]: Monad: UUIDIssuer,
    D](
    sourcePlugins: List[SourcePlugin[F, D]],
    sinkPlugins: List[SinkPlugin[F, D]],
    rootBuilder: RootBuilder[D]
) extends PluginWatcherBuilder[F, D] {

  private lazy val buildSource = new Builder(sourcePlugins)
  private lazy val buildSink = new Builder(sinkPlugins)

  def use(sourcePlugin: SourcePlugin[F, D]): PluginWatcherBuilder[F, D] =
    copy(sourcePlugins = sourcePlugin :: sourcePlugins)

  def use(sinkPlugin: SinkPlugin[F, D]): PluginWatcherBuilder[F, D] =
    copy(sinkPlugins = sinkPlugin :: sinkPlugins)

  def build(definition: D): F[BuildResult[List[Watcher[F]]]] = Monad[F].pure(
    for {
      watcherDefinitions <- rootBuilder.build(definition)
      watchers <- watcherDefinitions.parTraverse { watcherDefinition =>
        (
          buildSource(
            watcherDefinition.source.`type`,
            watcherDefinition.source.params),
          buildSink(
            watcherDefinition.sink.`type`,
            watcherDefinition.sink.params)
        ).parMapN(Watcher(watcherDefinition.interval, _, _))
      }
    } yield watchers
  )

  private class Builder[A](plugins: List[Plugin[D, A]]) {

    val pluginByName: Map[PluginName, Plugin[D, A]] =
      plugins.map(plugin => plugin.name -> plugin).toMap

    def apply(name: PluginName, definition: D): BuildResult[A] =
      pluginByName.get(name).fold(noSuchPlugin(name))(_.load(definition))

    private def noSuchPlugin(name: PluginName): BuildResult[A] =
      Left(
        NonEmptyList.of(
          InvalidDefinition(s"""No such plugin. (name = "$name")""")))

  }

}
