package io.coppermine
package internal

import java.util.concurrent.LinkedBlockingDeque
import java.util.UUID

case class Execution() {
  val queue    = new LinkedBlockingDeque[Msg]()
  val settings = Settings("localhost", 1113)
  val conn     = Connection(settings)
  var reader   = Reader(queue, conn)
  var manager  = Manager(settings, queue, conn)

  manager.start()
  reader.start()

  def push(cmd: Command): Unit =
    queue.add(Operation(cmd))

  def shutdown(): Unit =
    queue.add(Shutdown)
}

case class Manager(settings: Settings, queue: LinkedBlockingDeque[Msg], conn: Connection) extends Thread {
  case class Cont(f: PartialFunction[Package, Result])

  val ops = scala.collection.mutable.Map[UUID, Cont]()

  override def run = loop

  @annotation.tailrec
  final def loop: Unit = {
    queue.poll match {
      case Shutdown =>
        conn.close()
      case Pkg(pkg) => {
        ops.get(pkg.correlation).foreach {
          case Cont(partial) =>
            partial.lift(pkg).foreach {
              case Send(newPkg, k) =>
                ops - pkg.correlation
                ops + (newPkg.correlation -> Cont(k))
                conn.send(newPkg)
              case Done =>
                ops - pkg.correlation
              case Error(e) =>
                println(s"Exception raised $e")
              case Retry(cmd) =>
                queue.add(Operation(cmd))
            }
        }
        loop
      }
      case Operation(cmd) => {
        cmd(settings) match {
          case Send(pkg, k) =>
            ops + (pkg.correlation -> Cont(k))
            conn.send(pkg)
          case Retry(newCmd) => queue.add(Operation(newCmd))
          case _ => /* Nothing to do here */
        }

        loop
      }
    }
  }
}

case class Reader(queue: LinkedBlockingDeque[Msg], conn: Connection) extends Thread {
  override def run: Unit = {
    @annotation.tailrec
    def loop: Unit = {
      val pkg = conn.recv

      pkg.cmd match {
        case 0x01 => conn.send(Package.heartbeatResponse(pkg.correlation))
        case _    => queue.add(Pkg(pkg))
      }

      loop
    }

    loop
  }
}

sealed trait Msg
case class Operation(cmd: Command) extends Msg
case class Pkg(pkg: Package) extends Msg
case object Shutdown extends Msg

object Execution {
  val settings = Settings("localhost", 1113)
  val conn     = Connection(settings)

  @annotation.tailrec
  def loop(): Unit = {
    val pkg = conn.recv
    println(pkg)

    pkg.cmd match {
      case 0x01 =>
        conn.send(Package.heartbeatResponse(pkg.correlation))
      case _ =>
        sys.error(s"Unhandled commond type ${pkg.cmd}")
    }

    loop
  }
}
