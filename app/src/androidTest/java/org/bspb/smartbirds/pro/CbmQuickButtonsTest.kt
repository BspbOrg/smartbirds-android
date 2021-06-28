package org.bspb.smartbirds.pro

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.bspb.smartbirds.pro.tools.robot.CbmFormTestRobot.Companion.cbmScreen
import org.bspb.smartbirds.pro.tools.robot.MonitoringTestRobot.Companion.monitoringScreen
import org.bspb.smartbirds.pro.tools.robot.SingleChoiceDialogTestRobot.Companion.singleChoiceDialog
import org.bspb.smartbirds.pro.tools.rule.ActiveMonitoringRule
import org.bspb.smartbirds.pro.tools.rule.CompositeRules
import org.hamcrest.Matchers
import org.hamcrest.Matchers.startsWith
import org.hamcrest.Matchers.startsWithIgnoringCase
import org.hamcrest.core.StringStartsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CbmQuickButtonsTest {
    @Rule
    @JvmField
    var activeMonitoringRule = ActiveMonitoringRule()

    @Rule
    @JvmField
    var screenshotRule = CompositeRules.screenshotTestRule()

    @Before
    fun setUp() {
        monitoringScreen {
            buttonFabAddEntry().perform(click())

            Espresso.onData(
                Matchers.`is`(
                    ApplicationProvider.getApplicationContext<Context>()
                        .getString(R.string.entry_type_cbm)
                )
            ).perform(click())
        }

        cbmScreen {
            isDisplayed()
        }
    }

    @Test
    fun testQuickButtonValueIsAssigned() {
        cbmScreen {
            quickChoiceButtons()[1].perform(click())
        }

        singleChoiceDialog {
            listItem(1).perform(click())
        }

        cbmScreen {
            quickChoiceButtons()[1].check(matches(withText(startsWithIgnoringCase("acc gen"))))
        }
    }

    @Test
    fun testQuickButtonValueIsReassigned() {
        cbmScreen {
            quickChoiceButtons()[1].perform(click())
        }

        singleChoiceDialog {
            listItem(1).perform(click())
        }

        cbmScreen {
            quickChoiceButtons()[1].check(matches(withText(startsWithIgnoringCase("acc gen"))))
            quickChoiceButtons()[1].perform(longClick())
        }

        singleChoiceDialog {
            listItem(2).perform(click())
        }

        cbmScreen {
            quickChoiceButtons()[1].check(matches(withText(startsWithIgnoringCase("acc nis"))))
        }
    }

    @Test
    fun testSpeciesValueIsAssigned() {
        cbmScreen {
            quickChoiceButtons()[1].perform(click())
        }

        singleChoiceDialog {
            listItem(1).perform(click())
        }

        cbmScreen {
            quickChoiceButtons()[1].perform(click())
        }

        cbmScreen {
            fieldSpecies().check(matches(withText(startsWithIgnoringCase("Accipiter gentilis"))))
        }

    }
}