package me.jooohn.watcher.plugins

import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Json}
import me.jooohn.watcher.plugins.sink.slack.SlackParams
import me.jooohn.watcher.plugins.source.screenshot.ScreenshotParams

import scala.util.Try

trait JsonPluginsDsl[F[_]] extends PluginsDsl[F, Json] {

  implicit val screenshotAttributes: Decoder[ScreenshotParams] =
    deriveDecoder[ScreenshotParams]

  implicit val slackAttributesDecoder: Decoder[SlackParams] =
    deriveDecoder[SlackParams]

  implicit def toleranceDecoder[A]: Decoder[Tolerance[A]] =
    Decoder[BigDecimal].emapTry(bd => Try(Tolerance(bd)))

  override val rootDecode: Decode[Json, List[WatcherDefinition[Json]]] =
    Decode[Json, List[WatcherDefinition[Json]]]

}
