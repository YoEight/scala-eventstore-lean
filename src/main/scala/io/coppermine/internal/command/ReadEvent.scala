package io.coppermine
package internal
package command

import protocol.Messages.{ ReadEvent => RE }
import protocol.Messages.ReadEventCompleted
import ReadEventCompleted.ReadEventResult._
import scala.concurrent.SyncVar

trait ReadEvent extends Command {
  def stream: String
  def eventNumber: Int
  def tos: Boolean
  def result: SyncVar[ReadResult[ReadEventResult]]

  def apply(settings: Settings) = {
    val msg = RE.newBuilder
      .setEventStreamId(stream)
      .setEventNumber(eventNumber)
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
            val evt   = msg.getEvent
            val value = EventFound(stream, eventNumber, Indexed(stream, evt))

            result.put(ReadSuccess(value))
            Done
          case NotFound =>
            val value = ReadSuccess(EventNotFound(stream, eventNumber))
            result.put(value)
            Done
          case NoStream =>
            result.put(ReadNoStream(stream))
            Done
          case StreamDeleted =>
            result.put(ReadStreamDeleted(stream))
            Done
          case Error =>
            val value = ReadError(Option(msg.getError()))
            result.put(value)
            Done
          case AccessDenied =>
            result.put(ReadAccessDenied(stream))
            Done
        }
    })
  }
}

object ReadEvent {
  def apply(_stream: String, _eventNumber: Int, _tos: Boolean) = new ReadEvent {
    val stream      = _stream
    val eventNumber = _eventNumber
    val tos         = _tos
    val result      = new SyncVar[ReadResult[ReadEventResult]]()
  }
}
