package me.jooohn.watcher.controller

import cats.effect._
import cats.syntax.all._
import io.circe.Json
import io.circe.syntax._
import me.jooohn.watcher.usecase.ScheduleWatch
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object WatchersService {

  def apply[F[_]: Sync](useCase: ScheduleWatch[F, Json]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case req @ POST -> Root / "watchers" =>
        for {
          definition <- req.as[Json]
          result <- useCase.execute(definition)
          response <- result match {
            case Left(error) =>
              BadRequest(error.message.asJson)
            case Right(_) =>
              Ok()
          }
        } yield response
    }
  }

}
