package me.jooohn.watcher.plugins.source

import me.jooohn.watcher.plugins.source.html.Html
import me.jooohn.watcher.plugins.source.now.Now
import me.jooohn.watcher.plugins.source.screenshot.Screenshot

trait Sources[F[_], A] extends Html[F, A] with Screenshot[F, A] with Now[F, A]
