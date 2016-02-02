package io.coppermine
package internal

import java.util.concurrent.LinkedBlockingDeque
import java.util.UUID

case class Execution() {
  val queue    = new LinkedBlockingDeque[Msg]()
  val settings = Settings("localhost", 1113)
  val conn     = Connection(settings)
}

case class Cont(f: PartialFunction[Package, Result])

trait Action[A]
case class SendPackage[A](pkg: Package, nxt: A) extends Action[A]
case class RecvPackage[A](pkg: Package, nxt: A) extends Action[A]
case class PushOperation[A](cmd: Command, nxt: A) extends Action[A]
case class Await[A](nxt: A) extends Action[A]

case class Manager(settings: Settings, ops: Map[UUID, Cont] = Map()) {
  def apply(pkg: Package): Option[Action[Manager]] =
    for {
      Cont(partial) <- ops.get(pkg.correlation)
      res           <- partial.lift(pkg)
    } yield res match {
      case Send(newPkg, k) =>
        val finalOps = ops - pkg.correlation + (newPkg.correlation -> Cont(k))
        SendPackage(newPkg, copy(ops = finalOps))
      case Retry(cmd) =>
        val finalOps = ops - pkg.correlation
        PushOperation(cmd, copy(ops = finalOps))
      case Done =>
        val finalOps = ops - pkg.correlation
        Await(copy(ops = finalOps))
      case Error(e) =>
        val finalOps = ops - pkg.correlation
        println(s"Exception raised $e")
        Await(copy(ops = finalOps))
    }

  def apply(cmd: Command): Action[Manager] = cmd(settings) match {
    case Send(pkg, k) =>
      val finalOps = ops + (pkg.correlation -> Cont(k))
      SendPackage(pkg, copy(ops = finalOps))
    case Retry(newCmd) =>
      PushOperation(newCmd, this)
    case _ =>
      Await(this)
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
