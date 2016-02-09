package io.coppermine
package internal
package command

import protocol.Messages.{ ReadEvent => RE }
import protocol.Messages.ReadEventCompleted
import protocol.Messages.ResolvedIndexedEvent
import ReadEventCompleted.ReadEventResult._
import scala.concurrent.SyncVar

case class ReadEvent(stream: String, evtNumber: Int, tos: Boolean, res: SyncVar[ReadResult[ReadEventResult]]) extends Command {
  def apply(settings: Settings) = {
    val msg = RE.newBuilder
      .setEventStreamId(stream)
      .setEventNumber(evtNumber)
      .setResolveLinkTos(tos)
      .setRequireMaster(settings.requireMaster)
      .build
      .toByteArray

    val pkg = Package(0xB0, msg, settings.credentials)

    Send(pkg, {
      case Package(cmd, _, bytes, _) if cmd == 0xB1 =>
        val msg = ReadEventCompleted.parseFrom(bytes)

        msg.getResult match {
          case Success =>
            val evt    = msg.getEvent
            val result = EventFound(stream, evtNumber, Indexed(stream, evt))

            res.put(ReadSuccess(result))
            Done
          case NotFound =>
            val value = ReadSuccess(EventNotFound(stream, evtNumber))
            res.put(value)
            Done
          case NoStream =>
            res.put(ReadNoStream(stream))
            Done
          case StreamDeleted =>
            res.put(ReadStreamDeleted(stream))
            Done
          case Error =>
            val value = ReadError(Option(msg.getError()))
            res.put(value)
            Done
          case AccessDenied =>
            res.put(ReadAccessDenied(stream))
            Done
        }
    })
  }
}

import java.util.UUID

case class Indexed(stream: String, e: ResolvedIndexedEvent) extends ResolvedEvent {
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
