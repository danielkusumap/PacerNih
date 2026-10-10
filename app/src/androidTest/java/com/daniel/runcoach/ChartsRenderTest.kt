package com.daniel.runcoach

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daniel.runcoach.ui.charts.Bar
import com.daniel.runcoach.ui.charts.BarChart
import com.daniel.runcoach.ui.charts.LineChart
import com.daniel.runcoach.ui.charts.LinePoint
import com.daniel.runcoach.ui.charts.TargetStep
import com.daniel.runcoach.ui.theme.RunCoachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** P2.7 AC: the Canvas charts render without a chart dependency (normal, empty, gaps). */
@RunWith(AndroidJUnit4::class)
class ChartsRenderTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun chartsRenderForNormalAndEmptyData() {
        rule.setContent {
            RunCoachTheme {
                Column {
                    BarChart(bars = listOf(Bar("Mo", 3.2), Bar("Tu", null), Bar("We", 5.1, target = 4.0)))
                    BarChart(bars = emptyList())
                    LineChart(
                        points = listOf(LinePoint(0.0, 360.0), LinePoint(100.0, null), LinePoint(200.0, 350.0)),
                        steps = listOf(TargetStep(0.0, 200.0, 360.0)),
                        boundaries = listOf(100.0),
                    )
                    LineChart(points = emptyList())
                }
            }
        }
        rule.onRoot().assertExists()
    }
}

