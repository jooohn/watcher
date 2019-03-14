package me.jooohn.watcher.plugins.sink

import me.jooohn.watcher.plugins.sink.slack.SlackSink

trait Sinks[F[_], A]
  extends SlackSink[F, A]
