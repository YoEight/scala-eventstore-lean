package io.coppermine
package internal

import java.nio.ByteBuffer
import java.util.UUID

object Utils {
  def toByteBuffer(uuid: UUID): ByteBuffer =
    ByteBuffer.allocate(16)
      .putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
}
