package com.github.hiyuuu.guimaker

class GUIMaker_CornerSlot(var upLeft: Int, var upRight: Int, var downLeft: Int, var downRight: Int) {

    fun getFillSlots() : List<Int> {

        var line = 0
        val slots = mutableListOf<Int>()
        val spaceDiff = (downRight % 9) - (upLeft % 9)
        while (true) {
            val startAt = upLeft + (9 * line)
            if (startAt > downLeft) break

            (startAt..(startAt + spaceDiff)).forEach { slots.add(it) }
            line++
        }

        return slots
    }

    fun getHollowSlots() : List<Int> {

        val slots = mutableListOf<Int>()
        val lines = (downLeft - upLeft) / 9 + 1
        (1..lines).forEach { l ->
            val line = (l - 1)

            slots.add(upLeft + (9 * line))
            slots.add(upRight + (9 * line))
        }
        slots.addAll((upLeft..upRight).toList())
        slots.addAll((downLeft..downRight).toList())

        return slots
    }

}
