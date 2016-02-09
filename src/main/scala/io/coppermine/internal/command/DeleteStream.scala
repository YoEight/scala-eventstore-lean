package io.coppermine
package internal
package command

import protocol.Messages.{ DeleteStream => DS }
import protocol.Messages.DeleteStreamCompleted
import protocol.Messages.OperationResult._

import DeleteStream._

case class DeleteStream(input: Input) extends Command {
  def apply(settings: Settings) = {
    val msg = DS.newBuilder
      .setEventStreamId(input.stream)
      .setExpectedVersion(input.version.flag)
      .setRequireMaster(input.requireMaster)
      .setHardDelete(input.hardDelete)
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

            input.result.put(res)
            Done
          case WrongExpectedVersion =>
            Error(WrongExpectedVersionException(input.stream, input.version))
          case StreamDeleted =>
            Error(StreamDeletedException(input.stream))
          case InvalidTransaction =>
            Error(InvalidTransactionException)
          case AccessDenied =>
            Error(AccessDeniedException(input.stream))
          case _ => Retry(this)
        }
    })
  }
}

object DeleteStream {
  import scala.concurrent.SyncVar

  sealed trait Input {
    def stream: String
    def version: ExpectedVersion
    def requireMaster: Boolean
    def hardDelete: Boolean
    def result: SyncVar[DeleteResult]
  }

  def Input(_stream: String, _version: ExpectedVersion, _requireMaster: Boolean, _hardDelete: Boolean): Input = new Input {
    val stream        = _stream
    val version       = _version
    val requireMaster = _requireMaster
    val hardDelete    = _hardDelete
    val result        = new SyncVar[DeleteResult]
  }
}
