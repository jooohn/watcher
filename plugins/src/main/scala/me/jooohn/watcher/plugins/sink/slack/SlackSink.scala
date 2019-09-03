package me.jooohn.watcher.plugins.sink.slack

import cats.data.Kleisli
import cats.effect.ConcurrentEffect
import cats.syntax.all._
import io.chrisdavenport.log4cats.Logger
import io.circe.syntax._
import me.jooohn.watcher.domain.Sink
import me.jooohn.watcher.plugins.helper.Renderer
import me.jooohn.watcher.plugins.{Decode, PluginName, SinkPlugin}
import me.jooohn.watcher.port.BuildResult

import scala.concurrent.ExecutionContext

import io.circe.generic.auto._

trait SlackSink[F[_], A] {

  def slack(
      implicit D: Decode[A, SlackParams],
      logger: Logger[F],
      renderer: Renderer[F],
      C: ConcurrentEffect[F],
      ex: ExecutionContext): SinkPlugin[F, A] = new SinkPlugin[F, A] {

    val incomingWebhook: IncomingWebhook[F] = new IncomingWebhook[F]

    override def name: PluginName = PluginName("slack")

    override def load(a: A): BuildResult[Sink[F]] =
      D.decode(a) map { dto =>
        Kleisli { context =>
          for {
            text <- renderer.render(dto.template, context.asJson)
            _ <- incomingWebhook.post(
              dto.incomingWebhookUrl,
              Payload(
                channel = dto.channel,
                text = text,
                username = dto.username,
                iconEmoji = dto.iconEmoji,
                iconUrl = dto.iconUrl
              ))
          } yield ()
        }
      }
  }

}
