package me.jooohn.watcher.plugins

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Json}
import me.jooohn.watcher.plugins.sink.slack.SlackAttributes
import me.jooohn.watcher.plugins.source.screenshot.ScreenshotAttributes

import scala.util.Try

trait JsonPluginsDsl[F[_]] extends PluginsDsl[F, Json] {

  implicit val screenshotAttributes: Decoder[ScreenshotAttributes] =
    deriveDecoder[ScreenshotAttributes]
  implicit val slackAttributesDecoder: Decoder[SlackAttributes] =
    deriveDecoder[SlackAttributes]
  implicit def toleranceDecoder[A]: Decoder[Tolerance[A]] =
    Decoder[BigDecimal].emapTry(bd => Try(Tolerance(bd)))

  override val rootDecode: Decode[Json, List[WatcherDefinition[Json]]] =
    Decode[Json, List[WatcherDefinition[Json]]]

}
