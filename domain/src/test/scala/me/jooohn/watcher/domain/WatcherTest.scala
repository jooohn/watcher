package me.jooohn.watcher.domain

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

import cats.Id
import io.circe.Json
import org.scalatest.{FunSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

class WatcherTest extends FunSpec with Matchers {
  implicit val uuidIssuer: UUIDIssuer[Id] = UUIDIssuer.random[Id]

  describe("Watcher") {

    case class ContextSummary(
        prevTakenAt: Instant,
        prevValue: Int,
        currentTakenAt: Instant,
        currentValue: Int,
    )
    def summarize(context: SinkContext): ContextSummary =
      ContextSummary(
        prevTakenAt = context.prev.takenAt,
        prevValue = context.prev.subject.asNumber.get.toInt.get,
        currentTakenAt = context.current.takenAt,
        currentValue = context.current.subject.asNumber.get.toInt.get,
      )

    def repeatSource(candidates: List[Int]): Source[Id] =
      new Source[Id] {
        val ref: AtomicReference[List[Int]] = new AtomicReference(candidates)

        override type Subject = Int

        override def observe: Id[Subject] =
          ref.getAndUpdate(list => list.tail :+ list.head).head

        override def isChanged(a: Subject, b: Subject): Id[Boolean] = a != b

        override def parameterize(subject: Subject): Json = Json.fromInt(subject)
      }

    def buildTestWatcher(candidates: List[Int])(onChange: SinkContext => Unit): Watcher[Id] =
      Watcher[Id](
        FiniteDuration(100, duration.MILLISECONDS),
        source = repeatSource(candidates),
        sink = Sink[Id, SinkContext, Unit](onChange)
      )

    describe("check") {

      it("triggers onChange when change detected") {
        val resultBuf: ListBuffer[SinkContext] = ListBuffer()

        val watcher = buildTestWatcher(1 :: 2 :: 2 :: 3 :: Nil) { context =>
          resultBuf += context
        }

        val instants = List(
          Instant.ofEpochSecond(0),
          Instant.ofEpochSecond(10),
          Instant.ofEpochSecond(20),
          Instant.ofEpochSecond(30),
        )
        instants.foldLeft(None: Option[watcher.SubjectSnapshot]) { (prev, instant) =>
          Some(watcher.check(instant, prev))
        }

        val result = resultBuf.toList.map(summarize)
        result.length should be(2)

        result(0) should be(
          ContextSummary(
            prevTakenAt = instants(0),
            prevValue = 1,
            currentTakenAt = instants(1),
            currentValue = 2
          )
        )
        result(1) should be(
          ContextSummary(
            prevTakenAt = instants(2),
            prevValue = 2,
            currentTakenAt = instants(3),
            currentValue = 3
          ),
        )
      }

    }

  }

}
