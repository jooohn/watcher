package me.jooohn.watcher.plugins.source.screenshot

import java.awt.image.BufferedImage

import me.jooohn.watcher.plugins.Tolerance
import org.http4s.Uri

case class ScreenshotParams(
  uri: Uri,
  selector: Option[String],
  tolerance: Tolerance[BufferedImage]
)
