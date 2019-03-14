package me.jooohn.watcher.plugins

case class Tolerance[A](value: BigDecimal) {
  require(BigDecimal.exact(0L) <= value && value <= BigDecimal.exact(1L))

  def isSimilar(similarity: Similarity): Boolean =
    (similarity.value + value) >= BigDecimal.exact(1L)
}
