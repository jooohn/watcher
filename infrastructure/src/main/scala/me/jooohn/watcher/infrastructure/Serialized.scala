package me.jooohn.watcher.infrastructure

sealed trait Serialized
object Serialized {

  case class String(value: String) extends Serialized
  case class ByteArray(value: Array[Byte]) extends Serialized

}
