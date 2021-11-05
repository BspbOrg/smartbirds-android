package org.bspb.smartbirds.pro.tools.robot

import androidx.annotation.StringRes
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.tools.nomenclatureWithLabel
import org.bspb.smartbirds.pro.tools.speciesWithLabel
import org.bspb.smartbirds.pro.tools.zoneWithLabel
import org.bspb.smartbirds.pro.ui.views.NomenclatureItem
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput
import org.bspb.smartbirds.pro.ui.views.ZoneFormInput
import org.bspb.smartbirds.pro.utils.debugLog
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo

@TestRobotMarker
private interface SingleChoiceDialogRobot

class SingleChoiceDialogTestRobot : SingleChoiceDialogRobot {

    companion object {

        fun singleChoiceDialog(block: SingleChoiceDialogTestRobot.() -> Unit): SingleChoiceDialogTestRobot {
            return SingleChoiceDialogTestRobot().apply(block)
        }
    }

    fun isDisplayed(@StringRes resource: Int): ViewInteraction =
        onView(withText(resource)).check(matches(ViewMatchers.isDisplayed()))

    fun listItem(idx: Int): DataInteraction =
        onData(instanceOf(NomenclatureItem::class.java)).atPosition(idx)

    fun onZoneRow(str: String): DataInteraction {
        return onData(
            allOf(
                instanceOf(ZoneFormInput.ZoneHolder::class.java),
                `is`(zoneWithLabel(str))
            )
        )
    }

    fun onRow(str: String): DataInteraction {
        return onData(
            allOf(
                instanceOf(NomenclatureItem::class.java),
                `is`(nomenclatureWithLabel(str))
            )
        )
    }

    fun onSpeciesRow(str: String): DataInteraction = onData(
        allOf(
            instanceOf(NomenclatureItem::class.java),
            `is`(speciesWithLabel(str))
        )
    )

    fun clearSelection() = onView(withText(R.string.clear)).perform(click())
}
