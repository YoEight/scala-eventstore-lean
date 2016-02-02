package io.coppermine
package internal

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.channels.SocketChannel
import java.util.UUID
import scala.concurrent.SyncVar

case class WrongFlagValueException(cmd: Byte) extends Exception

case class Connection(settings: Settings) {
  private lazy val _socket = {
    val addr = new InetSocketAddress(settings.hostname, settings.port)
    val s = SocketChannel.open()
    s.connect(addr)
    s
  }

  def recv(): Package =
    Read[Package](Connection.deframe(_socket))

  def send(pkg: Package): Unit = {
    val buf = ByteBuffer.allocate(pkg.size + 4)

    buf.order(ByteOrder.LITTLE_ENDIAN)
      .putInt(pkg.size)
      .order(ByteOrder.BIG_ENDIAN)

    Write(buf, pkg)
    buf.flip()

    _socket.write(buf)
  }

  def close(): Unit =
    _socket.close
}

object Connection {

  // The package size of the frame is encodeded with 4 bytes.
  val FRAME_SIZE = 4

  def deframe(sock: SocketChannel): ByteBuffer = {
    val frameBuf = ByteBuffer.allocate(FRAME_SIZE)

    sock.read(frameBuf)
    frameBuf.flip()

    val pkgSiz = frameBuf.order(ByteOrder.LITTLE_ENDIAN).getInt
    val pkgBuf = ByteBuffer.allocate(pkgSiz)

    sock.read(pkgBuf)
    pkgBuf.flip()

    pkgBuf
  }
}
