package me.jooohn.watcher.adapter

import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{ContextShift, Fiber, IO, Timer}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.Json
import me.jooohn.watcher.domain.{Sink, Source, UUIDIssuer, Watcher}
import org.scalatest.{FunSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

case class CounterSource(ref: Ref[IO, Int], until: Int, onFinish: IO[Unit]) extends Source[IO] {
  override type Subject = Int

  override def observe: IO[Subject] =
    for {
      number <- ref.modify(n => (n + 1, n))
      _ <- if (number == until) onFinish else IO.unit
    } yield number

  override def isChanged(a: Subject, b: Subject): IO[Boolean] = IO.pure(a != b)

  override def parameterize(subject: Subject): Json = Json.fromInt(subject)
}

class ConcurrentSchedulerTest extends FunSpec with Matchers {
  val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)
  implicit val uuidIssuer: UUIDIssuer[IO] = UUIDIssuer.random[IO]

  val logger = Slf4jLogger.getLogger[IO]

  val emptyScheduler: IO[ConcurrentScheduler[IO]] =
    for {
      tasksRef <- Ref.of[IO, List[Fiber[IO, Unit]]](Nil)
    } yield new ConcurrentScheduler(tasksRef, logger)

  def counter(
      id: String,
      until: Int,
      intervalMillis: Int,
      log: Ref[IO, Vector[String]],
      onFinish: IO[Unit],
  ): IO[Watcher[IO]] =
    for {
      counterRef <- Ref.of[IO, Int](0)
    } yield
      Watcher(
        interval = FiniteDuration(intervalMillis, MILLISECONDS),
        source = CounterSource(counterRef, until, onFinish),
        sink = Sink { context =>
          log.update { logs =>
            logs :+ s"[${id}] ${context.prev.subject} => ${context.current.subject}"
          }
        }
      )

  describe("schedule") {

    it("starts watcher tasks concurrently") {
      val io =
        for {
          logRef <- Ref.of[IO, Vector[String]](Vector.empty)
          lock <- Semaphore[IO](2)
          _ <- lock.acquireN(2)
          counter1 <- counter("A", 4, 100, logRef, lock.release)
          counter2 <- counter("B", 3, 150, logRef, lock.release)
          scheduler <- emptyScheduler
          _ <- scheduler.schedule(List(counter1, counter2))
          _ <- lock.acquireN(2)
          logs <- logRef.get
        } yield logs
      val logs = io.unsafeRunSync()
      logs should be(Vector(
        "[A] 0 => 1", // 0
        "[B] 0 => 1", // 0
        "[A] 1 => 2", // 100
        "[B] 1 => 2", // 150
        "[A] 2 => 3", // 200
        "[A] 3 => 4", // 300
        "[B] 2 => 3", // 300
      ))
    }

  }

}
