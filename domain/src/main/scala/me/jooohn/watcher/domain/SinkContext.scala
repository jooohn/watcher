package me.jooohn.watcher.domain

case class SinkContext(prev: Snapshot.Parameterized, current: Snapshot.Parameterized)
