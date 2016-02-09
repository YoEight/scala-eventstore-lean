package io.coppermine

case class Settings(
  hostname: String,
  port: Int,
  credentials: Option[Credentials] = None,
  requireMaster: Boolean = true
)
