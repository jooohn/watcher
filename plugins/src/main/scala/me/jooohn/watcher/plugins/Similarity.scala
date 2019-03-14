package me.jooohn.watcher.plugins

case class Similarity(value: BigDecimal) {
  require(BigDecimal.exact(0L) <= value && value <= BigDecimal.exact(1L))
}
