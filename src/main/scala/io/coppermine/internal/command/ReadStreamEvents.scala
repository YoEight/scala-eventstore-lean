package io.coppermine
package internal
package command

import protocol.Messages.{ ReadStreamEvents => RSE }
import protocol.Messages.ReadStreamEventsCompleted
import ReadStreamEventsCompleted.ReadStreamResult._
import scala.collection.JavaConversions._
import scala.concurrent.SyncVar

trait ReadStreamEvents extends Command {
  def stream: String
  def direction: Direction
  def start: Int
  def maxCount: Int
  def tos: Boolean
  def result: SyncVar[ReadResult[StreamSlice]]

  def apply(settings: Settings) = {
    val (reqCmd, respCmd) = direction match {
      case Forward  => (0xB2, 0xB3)
      case Backward => (0xB4, 0xB5)
    }

    val msg = RSE.newBuilder
      .setEventStreamId(stream)
      .setFromEventNumber(start)
      .setMaxCount(maxCount)
      .setResolveLinkTos(tos)
      .setRequireMaster(settings.requireMaster)
      .build
      .toByteArray

    val pkg = Package(reqCmd, msg, settings.credentials)

    Send(pkg, {
      case Package(cmd, _, bytes, _) if cmd == respCmd =>
        val resp = ReadStreamEventsCompleted.parseFrom(bytes)

        resp.getResult match {
          case Success =>
            val events      = resp.getEventsList.toList.map(Indexed(stream, _))
            val next        = resp.getNextEventNumber
            val last        = resp.getLastEventNumber
            val endOfStream = resp.getIsEndOfStream

            val slice = StreamSlice(events, direction, endOfStream, start, next, last)
            result.put(ReadSuccess(slice))
            Done
          case NoStream =>
            result.put(ReadNoStream(stream))
            Done
          case StreamDeleted =>
            result.put(ReadStreamDeleted(stream))
            Done
          case NotModified =>
            result.put(ReadNotModified)
            Done
          case AccessDenied =>
            result.put(ReadAccessDenied(stream))
            Done
          case Error =>
            result.put(ReadError(Option(resp.getError)))
            Done
        }
    })
  }
}

object ReadStreamEvents {
  def apply(_stream: String, _direction: Direction, _start: Int, _maxCount: Int, _tos: Boolean) = new ReadStreamEvents {
    val stream    = _stream
    val direction = _direction
    val start     = _start
    val maxCount  = _maxCount
    val tos       = _tos
    val result    = new SyncVar[ReadResult[StreamSlice]]()
  }
}
