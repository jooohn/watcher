package me.jooohn.watcher.plugins.helper

import io.circe.Json

trait Renderer[F[_]] {

  def render(template: String, data: Json): F[String]

}
