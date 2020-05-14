package org.codefreak.codefreak.util

import java.lang.IllegalArgumentException

object PositionUtil {
  fun <T> move(items: Collection<T>, oldPosition: Long, newPosition: Long, getPosition: T.() -> Long, setPosition: T.(Long) -> Unit) {
    if (oldPosition == newPosition) {
      return
    }
    require(newPosition < items.size && newPosition >= 0) { "Invalid position" }

    val movedItem = items.find { it.getPosition() == oldPosition } ?: throw IllegalArgumentException()

    if (oldPosition < newPosition) {
      /*  0
          1 --
          2  |
          3 <|
          4

          0 -> 0 // +- 0
          1 -> 3 // = 3
          2 -> 1 // -1
          3 -> 2 // -1
          4 -> 4 // +- 0
       */
      items
          .filter { it.getPosition() in (oldPosition + 1)..newPosition }
          .forEach { it.setPosition(it.getPosition() - 1) }
    } else {
      /*  0
          1 <|
          2  |
          3 --
          4

          0 -> 0 // +- 0
          1 -> 2 // +1
          2 -> 3 // +1
          3 -> 1 // = 1
          4 -> 4 // +- 0
       */
      items
          .filter { it.getPosition() in newPosition until oldPosition }
          .forEach { it.setPosition(it.getPosition() + 1) }
    }

    movedItem.setPosition(newPosition)
  }
}
