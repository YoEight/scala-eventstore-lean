package io.coppermine

sealed trait ReadResult[+A] {
  def map[B](f: (A) => B): ReadResult[B] = this match {
    case ReadSuccess(v)       => ReadSuccess(f(v))
    case ReadNoStream(s)      => ReadNoStream(s)
    case ReadStreamDeleted(s) => ReadStreamDeleted(s)
    case ReadNotModified      => ReadNotModified
    case ReadError(m)         => ReadError(m)
    case ReadAccessDenied(s)  => ReadAccessDenied(s)
  }

  def fold[B](seed: => B)(f: (A) => B): B = this match {
    case ReadSuccess(v) => f(v)
    case _              => seed
  }

  def foldLeft[B](seed: B)(f: (B, A) => B): B = this match {
    case ReadSuccess(v) => f(seed, v)
    case _              => seed
  }

  def foldRight[B](seed: B)(f: (A, B) => B): B = this match {
    case ReadSuccess(v) => f(v, seed)
    case _              => seed
  }

  def getOrElse[B >: A](seed: => B): B = fold(seed)(identity[B])

  def foreach[U](f: (A) => U): Unit = fold(())(a => {f(a); ()})

  lazy val get = fold[Option[A]](None)(Some(_))
}

case class ReadSuccess[A](v: A) extends ReadResult[A]
case class ReadNoStream(stream: String) extends ReadResult[Nothing]
case class ReadStreamDeleted(stream: String) extends ReadResult[Nothing]
case object ReadNotModified extends ReadResult[Nothing]
case class ReadError(message: Option[String]) extends ReadResult[Nothing]
case class ReadAccessDenied(stream: String) extends ReadResult[Nothing]
