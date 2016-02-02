package io.coppermine
package internal
package command

import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import org.json4s._
import protocol.Messages.NewEvent
import protocol.Messages.OperationResult._
import protocol.Messages.WriteEvents
import protocol.Messages.WriteEventsCompleted
import scala.collection.JavaConverters._
import scala.concurrent.SyncVar

case class SendEvents(stream: String, evts: List[Event], requireMaster: Boolean, version: ExpectedVersion, res: SyncVar[WriteResult]) extends Command {
  def apply(settings: Settings) = {
    val xs = evts.map {
      case Event(tpe, id, data) =>
        val uuidByteString = ByteString.copyFrom(Utils.toByteBuffer(id))
        val builder        = NewEvent.newBuilder
          .setEventId(uuidByteString)
          .setEventType(tpe)
          .setDataContentType(data.flag)
          .setMetadataContentType(0)

        data match {
          case BinaryData(bs, meta) =>
            builder.setData(ByteString.copyFrom(bs))
            meta.foreach { ms =>
              builder.setMetadata(ByteString.copyFrom(ms))
            }
          case JsonData(js, meta) =>
            val charset = Charset.forName("US-ASCII")
            val datajs  = native.compactJson(native.renderJValue(js))

            builder.setData(ByteString.copyFrom(charset.encode(datajs)))
            meta.foreach { ms =>
              val metajs = native.compactJson(native.renderJValue(ms))
              val bs     = charset.encode(metajs)

              builder.setMetadata(ByteString.copyFrom(bs))
            }
        }

        builder.build
    }

    val dto = WriteEvents.newBuilder
      .setEventStreamId(stream)
      .setExpectedVersion(version.flag)
      .addAllEvents(xs.toIterable.asJava)
      .setRequireMaster(requireMaster)
      .build
    val bytes = dto.toByteArray
    val pkg   = Package(0x82, bytes, settings.credentials)

    Send(pkg, {
      case Package(cmd, _, bytes, _) if cmd == 0x83 =>
        val msg = WriteEventsCompleted.parseFrom(bytes)

        msg.getResult match {
          case Success =>
            val commit  = Option(msg.getCommitPosition).getOrElse[Long](-1)
            val prepare = Option(msg.getPreparePosition).getOrElse[Long](-1)
            val pos     = Position(commit, prepare)
            val last    = msg.getLastEventNumber

            res.put(WriteResult(last, pos))
            Done
          case WrongExpectedVersion =>
            Error(WrongExpectedVersionException(stream, version))
          case StreamDeleted =>
            Error(StreamDeletedException(stream))
          case InvalidTransaction =>
            Error(InvalidTransactionException)
          case AccessDenied =>
            Error(AccessDeniedException(stream))
          case _ => Retry(this)
        }
    })
  }
}
