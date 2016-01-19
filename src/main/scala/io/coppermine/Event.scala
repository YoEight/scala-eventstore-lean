package io.coppermine

import java.util.UUID
import org.json4s._

case class Event(
  tpe: String,
  id: UUID,
  data: EventData)

object Event {
  def apply(tpe: String, data: EventData): Event =
    Event(tpe, UUID.randomUUID, data)
}

sealed trait EventData {
  def flag: Int
}

case class JsonData(value: JValue, meta: Option[JValue]) extends EventData {
  val flag = 1
}

case class BinaryData(value: Array[Byte], meta: Option[Array[Byte]]) extends EventData {
  val flag = 0
}

object JsonData {
  def apply[A](a: A)(implicit W: Writer[A]): JsonData =
    JsonData(W.write(a), None)

  def apply[A, B](a: A, b: B)(implicit W1: Writer[A], W2: Writer[B]): JsonData =
    JsonData(W1.write(a), Some(W2.write(b)))
}
