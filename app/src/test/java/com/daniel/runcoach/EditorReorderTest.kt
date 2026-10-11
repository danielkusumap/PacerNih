package com.daniel.runcoach

import com.daniel.runcoach.engine.model.Limit
import com.daniel.runcoach.engine.model.Repeat
import com.daniel.runcoach.engine.model.Segment
import com.daniel.runcoach.engine.model.SegmentKind
import com.daniel.runcoach.ui.editor.duplicateRepeat
import com.daniel.runcoach.ui.editor.moveItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** F2: reordering a Repeat block and its children. */
class EditorReorderTest {

    private fun run(name: String) = Segment(name, SegmentKind.RUN, Limit.Distance(400), null)

    @Test
    fun movesRepeatBlockAsOneUnit() {
        val warmup = Segment("Warm up", SegmentKind.WARMUP, Limit.Distance(1000), null)
        val repeat = Repeat(times = 6, children = listOf(run("A"), run("B")))
        val cooldown = Segment("Cool down", SegmentKind.COOLDOWN, Limit.Distance(1000), null)
        val top = listOf(warmup, repeat, cooldown)

        val movedDown = moveItem(top, 1, 2)
        assertEquals(listOf("Warm up", "Cool down"), movedDown.filterIsInstance<Segment>().map { it.label })
        assertEquals(2, movedDown.indexOf(repeat))
        // The block keeps all its children in order.
        assertEquals(listOf("A", "B"), (movedDown[2] as Repeat).children.map { it.label })
    }

    @Test
    fun movesChildWithinTheBlock() {
        val repeat = Repeat(times = 3, children = listOf(run("A"), run("B"), run("C")))
        val moved = moveItem(repeat.children, 0, 1)
        assertEquals(listOf("B", "A", "C"), moved.map { it.label })
    }

    @Test
    fun outOfRangeMoveReturnsSameList() {
        val list = listOf(run("A"), run("B"))
        assertSame(list, moveItem(list, 0, -1))
        assertSame(list, moveItem(list, 0, 2))
    }

    @Test
    fun duplicateRepeatCopiesChildren() {
        val repeat = Repeat(times = 6, children = listOf(run("A"), run("B")))
        val copy = duplicateRepeat(repeat)
        assertEquals(repeat.times, copy.times)
        assertEquals(listOf("A", "B"), copy.children.map { it.label })
    }
}
