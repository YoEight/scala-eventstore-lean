package io.coppermine

import java.util.UUID

sealed trait ContentType
case object JsonContent extends ContentType
case object BinaryContent extends ContentType

trait ResolvedEvent {
  def stream: String
  def eventNumber: Int
  def id: UUID
  def eventType: String
  def contentType: ContentType
  def data: Array[Byte]
  def metadata: Option[Array[Byte]]
}
