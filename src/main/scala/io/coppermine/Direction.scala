package io.coppermine

sealed trait Direction

case object Forward extends Direction
case object Backward extends Direction
