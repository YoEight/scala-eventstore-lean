package io.coppermine
package internal
package command

import protocol.Messages.ResolvedIndexedEvent

case class Indexed(stream: String, e: ResolvedIndexedEvent) extends ResolvedEvent {
  import java.util.UUID

  val original = e.getEvent

  val eventNumber = original.getEventNumber

  lazy val id = Read[UUID](original.getEventId.asReadOnlyByteBuffer)

  val eventType = original.getEventType

  val contentType = original.getDataContentType match {
    case 1 => JsonContent
    case 0 => BinaryContent
  }

  val data =
    if (original.getData.isEmpty) Array[Byte]()
    else original.getData.toByteArray

  val metadata =
    if (original.getMetadata.isEmpty) None
    else Some(original.getMetadata.toByteArray)
}
