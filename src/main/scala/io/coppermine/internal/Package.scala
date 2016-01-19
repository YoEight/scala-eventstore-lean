package io.coppermine
package internal

import java.util.UUID

case class Package(cmd: Byte, correlation: UUID, data: Array[Byte], credentials: Option[Credentials]) {

  lazy val isAuthenticated = credentials match {
    case Some(_) => true
    case _       => false
  }

  lazy val size = {
    val credSiz = credentials match {
      case None => 0
      case Some(Credentials(l, p)) =>
        // 2 is because we have 2 extra Bytes used to store login and password
        // length.
        2 + l.length + p.length
    }

    // 18 is a mandatory size which is comprised of a byte command,
    // authentication command and UUID bytes.
    18 + credSiz + data.size
  }
}

object Package {
  def heartbeatResponse(uuid: UUID): Package =
    Package(0x02, uuid, new Array[Byte](0), None)

  def apply(cmd: Byte, data: Array[Byte], credentials: Option[Credentials]): Package = {
    val uuid = UUID.randomUUID

    Package(cmd, uuid, data, credentials)
  }
}
