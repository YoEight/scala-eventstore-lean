package io.coppermine

sealed trait ExpectedVersion {
  def flag: Int
}

case object AnyVersion extends ExpectedVersion {
  val flag = -2
}

case object NoStream extends ExpectedVersion {
  val flag = -1
}

case object EmptyStream extends ExpectedVersion {
  val flag = 0
}

case class ExactVersion(version: Int) extends ExpectedVersion {
  val flag = version
}
