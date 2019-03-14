package me.jooohn.watcher

import cats.syntax.all._
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import org.http4s.Uri

package object plugins {

  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit val uriDecoder: Decoder[Uri] = Decoder[String]
    .emap(string => Uri.fromString(string).leftMap(_.getMessage()))

  implicit val pluginNameDecoder: Decoder[PluginName] = Decoder[String].map(PluginName)


}
