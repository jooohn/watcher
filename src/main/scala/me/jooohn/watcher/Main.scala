package me.jooohn.watcher

import java.util.UUID

import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import me.jooohn.watcher.adapter.ConcurrentScheduler
import me.jooohn.watcher.controller.WatchersService
import me.jooohn.watcher.domain.UUIDIssuer
import me.jooohn.watcher.plugins.JsonPluginsDsl
import me.jooohn.watcher.plugins.helper.{HandlebarsRenderer, Renderer}
import me.jooohn.watcher.plugins.source.screenshot.ScreenshotTaker
import me.jooohn.watcher.usecase.StartWatching
import org.apache.log4j.BasicConfigurator
import org.http4s.{HttpApp, Uri}
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp with JsonPluginsDsl[IO] {

  implicit val IOUUIDIssuer: UUIDIssuer[IO] =
    new UUIDIssuer[IO] {
      override def issue: IO[UUID] = IO(UUID.randomUUID())
    }

  override def run(args: List[String]): IO[ExitCode] = {
    BasicConfigurator.configure()
    org.apache.log4j.Logger.getRootLogger.setLevel(org.apache.log4j.Level.INFO)

    implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    implicit val screenshotTaker: ScreenshotTaker[IO] =
      new ScreenshotTaker[IO](
        Uri.uri("https://rendertron-selector.herokuapp.com/")
      )

    implicit val renderer: Renderer[IO] =
      new HandlebarsRenderer[IO](
        Uri.uri("https://handlebars-web-api.herokuapp.com/")
      )

    def buildApp(tasksRef: Ref[IO, List[Fiber[IO, Unit]]]): HttpApp[IO] = {
      val startWatching =
        StartWatching(
          scheduler = new ConcurrentScheduler[IO](tasksRef),
          watcherBuilder = watcherBuilder
            .use(sourcePlugins.html)
            .use(sourcePlugins.screenshot)
            .use(sourcePlugins.now)
            .use(sinkPlugins.slack)
        )
      WatchersService(startWatching).orNotFound
    }

    for {
      tasksRef <- Ref.of[IO, List[Fiber[IO, Unit]]](Nil)
      server <- BlazeServerBuilder[IO]
        .bindHttp(8080, "localhost")
        .withHttpApp(buildApp(tasksRef))
        .resource
        .use(_ => IO.never)
        .start
      _ <- IO(scala.io.StdIn.readLine())
      _ <- server.cancel
      tasks <- tasksRef.get
      _ <- tasks.traverse(task => task.cancel: IO[Unit])
    } yield ExitCode.Success
  }

}
