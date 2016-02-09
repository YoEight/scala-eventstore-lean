package io.coppermine
package internal
package command

import protocol.Messages.{ DeleteStream => DS }
import protocol.Messages.DeleteStreamCompleted
import protocol.Messages.OperationResult._

import scala.concurrent.SyncVar

trait DeleteStream extends Command {
  def stream: String
  def version: ExpectedVersion
  def requireMaster: Boolean
  def hardDelete: Boolean
  def result: SyncVar[DeleteResult]

  def apply(settings: Settings) = {
    val msg = DS.newBuilder
      .setEventStreamId(stream)
      .setExpectedVersion(version.flag)
      .setRequireMaster(requireMaster)
      .setHardDelete(hardDelete)
      .build
      .toByteArray

    val pkg = Package(0x8A, msg, settings.credentials)

    Send(pkg, {
      case Package(cmd, _, bytes, _) if cmd == 0x8B =>
        val resp = DeleteStreamCompleted.parseFrom(bytes)

        resp.getResult match {
          case Success =>
            val commit  = Option(resp.getCommitPosition).getOrElse[Long](-1)
            val prepare = Option(resp.getPreparePosition).getOrElse[Long](-1)
            val pos     = Position(commit, prepare)
            val res     = DeleteResult(pos)

            result.put(res)
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

object DeleteStream {
  def apply(_stream: String, _version: ExpectedVersion, _requireMaster: Boolean, _hardDelete: Boolean) = new DeleteStream {
    val stream        = _stream
    val version       = _version
    val requireMaster = _requireMaster
    val hardDelete    = _hardDelete
    val result        = new SyncVar[DeleteResult]
  }
}
