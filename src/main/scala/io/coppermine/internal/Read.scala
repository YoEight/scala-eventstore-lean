package io.coppermine
package internal

import java.nio._
import java.util.UUID

trait Read[A] {
  def read(buf: ByteBuffer): A
}

object Read {
  def apply[A](buf: ByteBuffer)(implicit R: Read[A]): A = R.read(buf)

  implicit val uuidRead = new Read[UUID]{
    def read(buf: ByteBuffer) = {
      val oldOrder = buf.order()

      buf.order(ByteOrder.LITTLE_ENDIAN)

      val uuid = new UUID(buf.getLong, buf.getLong)
      buf.order(oldOrder)
      uuid
    }
  }
}
