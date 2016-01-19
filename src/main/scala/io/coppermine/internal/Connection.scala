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

  def recv: Package = {
    val pkgBuf  = Connection.deframe(_socket)
    val cmd     = pkgBuf.get
    val flag    = pkgBuf.get
    val uuidBuf = new Array[Byte](Connection.UUID_SIZE)

    pkgBuf.get(uuidBuf, 0, Connection.UUID_SIZE)

    val correlation = UUID.nameUUIDFromBytes(uuidBuf)

    val creds = flag match {
      case 0x00 => None
      case 0x01 =>
        val charset    = Charset.forName("US-ASCII")
        val loginLen   = pkgBuf.getInt
        val loginBytes = new Array[Byte](loginLen)

        pkgBuf.get(loginBytes, 0, loginLen)

        val passwLen   = pkgBuf.getInt
        val passwBytes = new Array[Byte](passwLen)

        pkgBuf.get(passwBytes, 0, passwLen)

        val login = charset.decode(ByteBuffer.wrap(loginBytes)).toString
        val passw = charset.decode(ByteBuffer.wrap(passwBytes)).toString

        Some(Credentials(login, passw))
      case unknown => throw WrongFlagValueException(unknown)
    }

    val msgData = pkgBuf.slice.array

    Package(cmd, correlation, msgData, creds)
  }

  def send(pkg: Package) {
    val authByte: Byte = if (pkg.isAuthenticated) 1 else 0
    val uuidBytes = ByteBuffer.allocate(16)
      .putLong(pkg.correlation.getMostSignificantBits)
      .putLong(pkg.correlation.getLeastSignificantBits)

    val buf = ByteBuffer.allocate(pkg.size)
      .order(ByteOrder.LITTLE_ENDIAN)
      .put(pkg.cmd)
      .put(authByte)
      .put(uuidBytes)

    pkg.credentials.foreach {
      case Credentials(l, p) =>
        val charset = Charset.forName("US-ASCII")
        buf.put(l.length.toByte)
        buf.put(charset.encode(l))
        buf.put(p.length.toByte)
        buf.put(charset.encode(p))
    }

    buf.put(pkg.data)
    buf.flip()
    _socket.write(buf)
  }

  def close(): Unit =
    _socket.close
}

object Connection {
  val UUID_SIZE        = 16
  val MANDATORY_LENGTH = UUID_SIZE + 2

  def deframe(socket: SocketChannel): ByteBuffer = {
    val frameBuf = ByteBuffer.allocate(4)

    socket.read(frameBuf)
    frameBuf.flip()

    val pkgSiz = frameBuf.getInt
    val pkgBuf = ByteBuffer.allocate(pkgSiz)

    socket.read(pkgBuf)
    pkgBuf.flip()
    pkgBuf
  }
}
