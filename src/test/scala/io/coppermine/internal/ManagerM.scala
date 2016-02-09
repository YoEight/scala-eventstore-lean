package io.coppermine
package internal

sealed trait ManagerOut

case class MgrSend(pkg: Package) extends ManagerOut
case class MgrRetry(cmd: Command) extends ManagerOut
case object MgrNoop extends ManagerOut

case class ManagerM[A](k: Manager => (Manager, A)) {
  def apply(mgr: Manager): (Manager, A) = k(mgr)

  def map[B](f: (A) => B): ManagerM[B] = ManagerM { m =>
    k(m) match {
      case (newM, a) => (newM, f(a))
    }
  }

  def flatMap[B](f: (A) => ManagerM[B]): ManagerM[B] = ManagerM { m =>
    k(m) match {
      case (newM, a) => f(a).k(newM)
    }
  }

  def execute(settings: Settings): Manager = k(Manager(settings))._1

  def eval(settings: Settings): A = k(Manager(settings))._2
}

object ManagerM {
  def point[A](v: => A): ManagerM[A] = ManagerM(m => (m, v))

  def addCommand(cmd: Command): ManagerM[Package] = ManagerM { m =>
    m(cmd) match {
      case SendPackage(pkg, nxtM) => (nxtM, pkg)
    }
  }

  def handlePackage(pkg: Package): ManagerM[ManagerOut] = ManagerM { m =>
    m(pkg) match {
      case None      => (m, MgrNoop)
      case Some(res) => res match {
        case SendPackage(pkg, newM)   => (newM, MgrSend(pkg))
        case PushOperation(cmd, newM) => (newM, MgrRetry(cmd))
        case Await(newM)              => (newM, MgrNoop)
      }
    }
  }
}
