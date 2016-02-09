package io.coppermine
package internal

import org.specs2._
import scala.concurrent.SyncVar

object ExecutionSpec extends Specification { def is = s2"""
  EventStore TCP Client specifications
    writing an event should work      $writeEvents
    reading an event should work      $readEvent
    deleting a stream should work     $deleteStream
                                      """

  val settings = Settings("localhost", 1113)

  def writeEvents = {
    val conn   = Connection(settings)
    val evt    = Event("event-type", BinaryData("event-data".getBytes, None))
    val result = new SyncVar[WriteResult]()
    val cmd    = command.SendEvents("test-stream", List(evt), true, AnyVersion, result)

    val action = for {
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => result.get
      case wrong   => sys.error(s"Wrong response $wrong")
    }

    val write = action.eval(settings)

    write must not beNull
  }

  def readEvent = {
    val conn     = Connection(settings)
    val evt      = Event("event-type", BinaryData("event-data".getBytes, None))
    val writeRes = new SyncVar[WriteResult]()
    val cmd      = command.SendEvents("test-read", List(evt), true, AnyVersion, writeRes)

    val writeAction = for {
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => writeRes.get
      case wrong   => sys.error(s"Wrong response during write $wrong")
    }

    val readRes = new SyncVar[ReadResult[ReadEventResult]]()
    val mgr     = writeAction.execute(settings)
    val cmd2    = command.ReadEvent("test-read", 1, true, readRes)

    val readAction = for {
      pkg <- ManagerM.addCommand(cmd2)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => readRes.get
      case wrong   => sys.error(s"Wrong response during read $wrong")
    }

    val (_, read) = readAction(mgr)

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
    val conn     = Connection(settings)
    val evt      = Event("event-type", BinaryData("event-data".getBytes, None))
    val writeRes = new SyncVar[WriteResult]()
    val cmd      = command.SendEvents("test-delete", List(evt), true, AnyVersion, writeRes)

    val writeAction = for {
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => writeRes.get
      case wrong   => sys.error(s"Wrong response during write $wrong")
    }

    val mgr   = writeAction.execute(settings)
    val input = command.DeleteStream.Input("test-delete", AnyVersion, true, false)
    val cmd2  = command.DeleteStream(input)

    val deleteAction = for {
      pkg <- ManagerM.addCommand(cmd2)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => input.result.get
      case wrong   => sys.error(s"Wrong response during delete $wrong")
    }

    val (_, delete) = deleteAction(mgr)

    delete must not beNull
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
}
