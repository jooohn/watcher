package me.jooohn.watcher

import cats.data.NonEmptyList

package object port {

  type BuildResult[A] = Either[NonEmptyList[InvalidDefinition], A]

}
