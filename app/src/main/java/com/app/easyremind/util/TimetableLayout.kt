package com.app.easyremind.util

import com.app.easyremind.data.ClassEntity

data class BlockPlacement(val column: Int, val columns: Int)

object TimetableLayout {

    /**
     * Assigns a column to each class on one day. Classes that overlap share
     * the day column side by side; classes that do not overlap reuse column 0.
     * [columns] is the width of the transitively-overlapping group so a block
     * never covers another one.
     *
     * The returned list keeps the same order as [classes] (index-based keys
     * are used internally, so unsaved classes with a placeholder id are safe).
     */
    fun placeDayClasses(classes: List<ClassEntity>): List<BlockPlacement> {
        val sorted = classes.indices.sortedWith(
            compareBy<Int> { classes[it].startMin }.thenBy { classes[it].endMin },
        )
        val columnEnds = mutableListOf<Int>()
        val columnOf = mutableMapOf<Int, Int>()

        sorted.forEach { index ->
            val clazz = classes[index]
            var column = columnEnds.indexOfFirst { it <= clazz.startMin }
            if (column < 0) {
                column = columnEnds.size
                columnEnds.add(clazz.endMin)
            } else {
                columnEnds[column] = clazz.endMin
            }
            columnOf[index] = column
        }

        return classes.indices.map { index ->
            val clazz = classes[index]
            val group = mutableSetOf<Int>()
            fun visit(current: Int) {
                if (!group.add(current)) return
                val c = classes[current]
                sorted.forEach { other ->
                    val o = classes[other]
                    if (other !in group &&
                        o.startMin < c.endMin &&
                        o.endMin > c.startMin
                    ) visit(other)
                }
            }
            visit(index)
            val columns = group.mapNotNull { columnOf[it] }.maxOrNull()?.plus(1) ?: 1
            BlockPlacement(column = columnOf[index] ?: 0, columns = columns)
        }
    }
}
