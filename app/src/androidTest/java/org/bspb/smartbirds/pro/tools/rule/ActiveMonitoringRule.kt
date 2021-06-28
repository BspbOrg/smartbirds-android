package org.bspb.smartbirds.pro.tools.rule

import androidx.test.espresso.action.ViewActions.click
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.bspb.smartbirds.pro.tools.robot.CommonFormTestRobot.Companion.commonFormScreen
import org.bspb.smartbirds.pro.tools.robot.MainTestRobot
import org.bspb.smartbirds.pro.tools.robot.MonitoringTestRobot.Companion.monitoringScreen
import org.bspb.smartbirds.pro.tools.robot.SingleChoiceDialogTestRobot.Companion.singleChoiceDialog
import org.bspb.smartbirds.pro.ui.MainActivity_
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ActiveMonitoringRule : TestRule {

    private val activityRule = ActivityScenarioRule(MainActivity_::class.java)
    private val mockApiRule = MockBackendRule()
    private val permissionsRule = SmartbirdsStateRule.grantMonitoringPermissions()
    private val loggedInRule = SmartbirdsStateRule.setLoggedIn(true)
    private val batteryNotificationRule = SmartbirdsStateRule.setBatteryNotification(true)
    private val locationRule = MockLocationRule()

    override fun apply(base: Statement?, description: Description?): Statement {
        var result = statement(base)

        result = activityRule.apply(result, description)
        result = mockApiRule.apply(result, description)
        result = permissionsRule.apply(result, description)
        result = batteryNotificationRule.apply(result, description)
        result = locationRule.apply(result, description)
        return loggedInRule.apply(result, description)
    }

    private fun statement(base: Statement?): Statement {
        return object : Statement() {
            override fun evaluate() {
                activityRule.scenario.onActivity {
                    locationRule.initFusedProvider(it)
                }

                MainTestRobot.mainScreen {
                    isDisplayed()
                    buttonStart().perform(click())
                }

                commonFormScreen {
                    isDisplayed()
                    // Wait a bit for the view to load the data
                    Thread.sleep(1000)
                    fieldSource().perform(click())
                }

                singleChoiceDialog {
                    listItem(0).perform(click())
                }

                commonFormScreen {
                    buttonSubmit().perform(click())
                }

                monitoringScreen {
                    isDisplayed()
                    locationRule.updateLocation()

                    // Wait before pressing the button otherwise the test is failing sometimes on the CI
                    Thread.sleep(500)
                }

                base?.evaluate()
            }

        }
    }

}