package io.coppermine
package internal

import java.util.UUID

case class Package(cmd: Int, correlation: UUID, data: Array[Byte], auth: Option[Credentials]) {

  lazy val isAuthenticated = auth.fold(false)(_ => true)

  lazy val size = {
    val credSiz = auth.fold(0){
      case Credentials(l, p) => 2 + l.length + p.length
    }

    Package.MANDATORY_SIZE + credSiz + data.length
  }
}

object Package {
  val MANDATORY_SIZE = 18

  def heartbeatResponse(uuid: UUID): Package =
    Package(0x02, uuid, new Array[Byte](0), None)

  def apply(cmd: Int, data: Array[Byte], auth: Option[Credentials]): Package = {
    val uuid = UUID.randomUUID

    Package(cmd, uuid, data, auth)
  }

  import java.nio.ByteBuffer

  implicit val packageRead = new Read[Package] {
    def read(buf: ByteBuffer) = {
      val cmd    = buf.get & 0xff
      val flag   = buf.get
      val uuid   = Read[UUID](buf)
      val auth   = flag match {
        case 0 => None
        case 1 => Some(Read[Credentials](buf))
      }

      val msgSiz = buf.remaining
      val msg    = new Array[Byte](msgSiz)

      buf.get(msg)
      Package(cmd, uuid, msg, auth)
    }
  }

  implicit val packageWrite = new Write[Package] {
    def write(buf: ByteBuffer, value: Package) = {
      val flag = value.auth.fold[Byte](0)(_ => 1)

      buf.put(value.cmd.asInstanceOf[Byte])
      buf.put(flag)
      Write(buf, value.correlation)

      value.auth.foreach(auth => Write(buf, auth))
      buf.put(value.data)
    }
  }
}
