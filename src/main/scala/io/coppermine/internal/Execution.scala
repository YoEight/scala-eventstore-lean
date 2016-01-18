package io.coppermine
package internal

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
