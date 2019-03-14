package me.jooohn.watcher.plugins.source.screenshot

import java.net.URLEncoder

import org.http4s.Uri

case class ScreenshotRequest(uri: Uri, selector: Option[String]) {

  def encodedUri: String = URLEncoder.encode(uri.toString, "UTF-8")

}
