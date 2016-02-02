package io.coppermine

import internal.Read
import internal.Write

import java.nio.ByteBuffer

case class Credentials(login: String, passw: String)

object Credentials {
  implicit val credentialsRead = new Read[Credentials] {
    def read(buf: ByteBuffer) = {
      val loginLen = buf.get & 0xff
      val loginBuf = new Array[Byte](loginLen)

      buf.get(loginBuf)

      val login    = new String(loginBuf)
      val passwLen = buf.get & 0xff
      val passwBuf = new Array[Byte](passwLen)
      buf.get(passwBuf)

      val passw = new String(passwBuf)

      Credentials(login, passw)
    }
  }

  implicit val credentialsWrite = new Write[Credentials] {
    def write(buf: ByteBuffer, value: Credentials) = {
      buf.put(value.login.length.asInstanceOf[Byte])
      buf.put(value.login.getBytes)
      buf.put(value.passw.length.asInstanceOf[Byte])
      buf.put(value.passw.getBytes)
    }
  }
}
