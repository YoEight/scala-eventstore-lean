package io.coppermine
package internal

case class Env(settings: Settings, conn: Connection)

trait Command {
  def apply(settings: Settings): Result
}

trait Result
case class Send(pkg: Package, cont: PartialFunction[Package, Result]) extends Result
case class Error(e: Exception) extends Result
case class Retry(cmd: Command) extends Result
case object Done extends Result

case class WrongExpectedVersionException(stream: String, version: ExpectedVersion) extends Exception
case class StreamDeletedException(stream: String) extends Exception
case object InvalidTransactionException extends Exception
case class AccessDeniedException(stream: String) extends Exception
