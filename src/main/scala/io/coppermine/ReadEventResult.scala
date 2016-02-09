package io.coppermine

sealed trait ReadEventResult {
  def stream: String
  def eventNumber: Int
  def event: Option[ResolvedEvent]
}

case class EventFound(stream: String, eventNumber: Int, evt: ResolvedEvent) extends ReadEventResult {
  val event = Some(evt)
}
case class EventNotFound(stream: String, eventNumber: Int) extends ReadEventResult {
  val event = None
}
