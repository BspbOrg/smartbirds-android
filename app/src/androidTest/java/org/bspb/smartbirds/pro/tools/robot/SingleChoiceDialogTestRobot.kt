package org.bspb.smartbirds.pro.tools.robot

import androidx.annotation.StringRes
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.bspb.smartbirds.pro.ui.views.NomenclatureItem
import org.bspb.smartbirds.pro.ui.views.SingleChoiceFormInput
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Matcher

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
}
