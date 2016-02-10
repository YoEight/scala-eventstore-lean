package io.coppermine
package internal

import org.specs2._
import scala.concurrent.SyncVar

object ExecutionSpec extends Specification { def is = s2"""
  EventStore TCP Client specifications
    writing an event should work      $writeEvents
    reading an event should work      $readEvent
    deleting a stream should work     $deleteStream
    reading stream events shoud work  $readStreamEvents
                                      """

  val settings = Settings("localhost", 1113)

  def writeEvents = {
    val conn   = Connection(settings)
    val action = sendEvent(conn, "test-stream", "event-type", "event-data")
    val write  = action.eval(settings)

    write must not beNull
  }

  def readEvent = {
    val conn = Connection(settings)

    val action = for {
      _ <- sendEvent(conn, "test-read", "event-type", "event-data")
      cmd = command.ReadEvent("test-read", 1, true)
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => cmd.result.get
      case wrong   => sys.error(s"Wrong response during read $wrong")
    }

    val read = action.eval(settings)

    read match {
      case ReadSuccess(r) => r match {
        case EventFound(_, _, evt) =>
          (evt.stream must_== "test-read") and
          (new String(evt.data) must_== "event-data") and
          (evt.eventNumber must_== 1)
        case _ => sys.error("ReadEvent: Event not found!")
      }
      case wrong => sys.error(s"Wrong output from read $wrong")
    }
  }

  def deleteStream = {
    val conn = Connection(settings)

    val action = for {
      _   <- sendEvent(conn, "test-delete", "event-type", "event-data")
      cmd = command.DeleteStream("test-delete", AnyVersion, true, false)
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => cmd.result.get
      case wrong   => sys.error(s"Wrong response during delete $wrong")
    }

    val delete = action.eval(settings)

    delete must not beNull
  }

  def readStreamEvents = {
    val conn = Connection(settings)

    val action = for {
      _   <- sendEvent(conn, "test-read-stream", "event-type", "event-data")
      cmd = command.ReadStreamEvents("test-read-stream", Forward, 1, 10, true)
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => cmd.result.get
      case wrong   => sys.error(s"Wrong response during read-stream $wrong")
    }

    val read = action.eval(settings)

    read match {
      case ReadSuccess(slice) =>
        val evt = slice.events.head

        (evt.stream must_== "test-read-stream") and
        (new String(evt.data) must_== "event-data") and
        (evt.eventNumber must_== 1)
      case wrong => sys.error(s"Wrong output from read $wrong")
    }
  }

  @annotation.tailrec
  def waitResponse(conn: Connection): Package = {
    val pkg = conn.recv

    pkg.cmd match {
      case 0x01 =>
        conn.send(Package.heartbeatResponse(pkg.correlation))
        waitResponse(conn)
      case other => pkg
    }
  }

  def sendEvent(conn: Connection, stream: String, tpe: String, value: String): ManagerM[WriteResult] = {
    val evt = Event(tpe, BinaryData(value.getBytes, None))
    val cmd = command.SendEvents(stream, List(evt), true, AnyVersion)

    for {
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => cmd.result.get
      case wrong   => sys.error(s"Wrong response when writting $wrong")
    }
  }
}
