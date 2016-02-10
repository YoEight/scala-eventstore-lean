package io.coppermine

sealed trait Slice {
  type Loc

  def events: List[ResolvedEvent]
  def direction: Direction
  def endOfStream: Boolean
  def from: Loc
  def next: Loc
}

case class StreamSlice(
  events: List[ResolvedEvent],
  direction: Direction,
  endOfStream: Boolean,
  from: Int,
  next: Int,
  last: Int) extends Slice { type Loc = Int }
