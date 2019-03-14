package me.jooohn.watcher.plugins

import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import me.jooohn.watcher.domain.{Sink, Source}
import me.jooohn.watcher.port.BuildResult

sealed trait Plugin[D, R] {

  def name: PluginName

  def load(definition: D): BuildResult[R]

}

trait SourcePlugin[F[_], D] extends Plugin[D, Source[F]]
trait SinkPlugin[F[_], D] extends Plugin[D, Sink[F]]
trait RootBuilder[D] {

  def build(definition: D): BuildResult[List[WatcherDefinition[D]]]

}

final case class PluginName(value: String) {
  override def toString: String = value
}

final case class SourceDefinition[D](plugin: PluginName, attributes: D)
object SourceDefinition {

  implicit val sourceDefinitionDecoder: Decoder[SourceDefinition[Json]] =
    deriveDecoder[SourceDefinition[Json]]

}

final case class SinkDefinition[D](plugin: PluginName, attributes: D)
object SinkDefinition {

  implicit val sinkDefinitionDecoder: Decoder[SinkDefinition[Json]] =
    deriveDecoder[SinkDefinition[Json]]

}

final case class WatcherDefinition[D](
  source: SourceDefinition[D],
  sink: SinkDefinition[D],
)
object WatcherDefinition {

  implicit val watcherDefinitionDecoder: Decoder[WatcherDefinition[Json]] =
    deriveDecoder[WatcherDefinition[Json]]
}
