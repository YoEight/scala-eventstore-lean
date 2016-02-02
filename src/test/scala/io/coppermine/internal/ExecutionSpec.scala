package io.coppermine
package internal

import org.specs2._
import scala.concurrent.SyncVar

object ExecutionSpec extends Specification { def is = s2"""
  EventStore TCP Client specifications
    writing an event should work      $writeEvents
                                      """

  val settings = Settings("localhost", 1113)

  def writeEvents = {
    val conn   = Connection(settings)
    val evt    = Event("event-type", BinaryData("event-data".getBytes, None))
    val result = new SyncVar[WriteResult]()
    val cmd    = command.SendEvents("test-stream", List(evt), true, AnyVersion, result)
    val mgr    = Manager(settings)

    val action = for {
      pkg <- ManagerM.addCommand(cmd)
      _    = conn.send(pkg)
      resp = waitResponse(conn)
      out <- ManagerM.handlePackage(resp)
    } yield out match {
      case MgrNoop => result.get
      case wrong   => sys.error(s"Wrong response $wrong")
    }

    val write = action.execute(settings)

    write must not beNull
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
