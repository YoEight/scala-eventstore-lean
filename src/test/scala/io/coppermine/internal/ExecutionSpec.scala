package io.coppermine
package internal

import org.specs2._
import scala.concurrent.SyncVar

object ExecutionSpec extends Specification { def is = s2"""
  EventStore TCP Client specifications
    writing an event should work      $writeEvents
                                      """

  def writeEvents = {
    val settings  = Settings("localhost", 1113)
    val conn      = Connection(settings)
    val evt       = Event("event-type", BinaryData("event-data".getBytes, None))
    val result    = new SyncVar[WriteResult]()
    val cmd       = command.SendEvents("test-stream", List(evt), true, AnyVersion, result)
    val mgr       = Manager(settings)

    val SendPackage(pkg, nextMgr) = mgr(cmd)
    conn.send(pkg)

    val resp = waitResponse(conn)
    nextMgr(resp) match {
      case Some(Await(_)) => /* Nothing to do here */
      case err            => sys.error(s"Wrong response $err")
    }

    result.get must not beNull
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
