package org.bspb.smartbirds.pro.tools.robot

import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.tools.toolbarWithTitle
import org.bspb.smartbirds.pro.tools.withHintParentOrOwn
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf

@TestRobotMarker
private interface BirdsFormRobot

class BirdsFormTestRobot : BirdsFormRobot {

    companion object {
        fun birdsScreen(block: BirdsFormTestRobot.() -> Unit): BirdsFormTestRobot {
            return BirdsFormTestRobot().apply(block)
        }
    }

    fun isDisplayed(): ViewInteraction =
        toolbarWithTitle(R.string.entry_type_birds).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    fun fieldSpecies(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_birds_name))

    fun fieldCountUnit(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_birds_count_unit))

    fun fieldCountType(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_birds_count_type))

    fun fieldCount(): ViewInteraction = onView(withHintParentOrOwn(R.string.monitoring_birds_count))
    fun buttonSave(): ViewInteraction =
        onView(allOf(instanceOf(Button::class.java), withText(R.string.menu_entry_save)))

}