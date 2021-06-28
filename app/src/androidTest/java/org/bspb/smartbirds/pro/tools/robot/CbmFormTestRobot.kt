package org.bspb.smartbirds.pro.tools.robot

import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.tools.toolbarWithTitle
import org.bspb.smartbirds.pro.tools.withHintParentOrOwn

@TestRobotMarker
public interface CbmFormRobot

class CbmFormTestRobot : CbmFormRobot {
    companion object {
        fun cbmScreen(block: CbmFormTestRobot.() -> Unit): CbmFormTestRobot {
            return CbmFormTestRobot().apply(block)
        }
    }

    fun isDisplayed(): ViewInteraction =
        toolbarWithTitle(R.string.entry_type_cbm).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    fun fieldSpecies(): ViewInteraction =
        Espresso.onView(withHintParentOrOwn(R.string.monitoring_cbm_name))

    fun quickChoiceButtons(): List<ViewInteraction> = listOf(
        Espresso.onView(withId(R.id.quick_1)),
        Espresso.onView(withId(R.id.quick_2)),
        Espresso.onView(withId(R.id.quick_3)),
        Espresso.onView(withId(R.id.quick_4))
    )
}