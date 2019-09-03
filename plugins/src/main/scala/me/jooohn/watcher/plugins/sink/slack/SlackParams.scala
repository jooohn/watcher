package me.jooohn.watcher.plugins.sink.slack

import org.http4s.Uri

case class SlackParams(
  incomingWebhookUrl: Uri,
  template: String,
  username: Option[String],
  channel: Option[String],
  iconEmoji: Option[String],
  iconUrl: Option[String],
)
