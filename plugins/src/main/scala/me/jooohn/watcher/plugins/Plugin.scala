package me.jooohn.watcher.plugins

import java.util.concurrent.TimeUnit

import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import me.jooohn.watcher.domain.{Sink, Source}
import me.jooohn.watcher.port.BuildResult

import scala.concurrent.duration.FiniteDuration

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

final case class SourceDefinition[D](`type`: PluginName, params: D)
object SourceDefinition {

  implicit val sourceDefinitionDecoder: Decoder[SourceDefinition[Json]] =
    deriveDecoder[SourceDefinition[Json]]

}

final case class SinkDefinition[D](`type`: PluginName, params: D)
object SinkDefinition {

  implicit val sinkDefinitionDecoder: Decoder[SinkDefinition[Json]] =
    deriveDecoder[SinkDefinition[Json]]

}

final case class WatcherDefinition[D](
    intervalMinutes: Option[Int],
    source: SourceDefinition[D],
    sink: SinkDefinition[D],
) {
  val defaultIntervalMinutes: Int = 15

  def interval: FiniteDuration =
    FiniteDuration(
      intervalMinutes.getOrElse(defaultIntervalMinutes),
      TimeUnit.MINUTES)

}
object WatcherDefinition {

  implicit val watcherDefinitionDecoder: Decoder[WatcherDefinition[Json]] =
    deriveDecoder[WatcherDefinition[Json]]
}
