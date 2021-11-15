package org.bspb.smartbirds.pro.tools.robot

import android.widget.Button
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import org.bspb.smartbirds.pro.R
import org.bspb.smartbirds.pro.tools.toolbarWithTitle
import org.bspb.smartbirds.pro.tools.withHintParentOrOwn
import org.hamcrest.Matchers

@TestRobotMarker
private interface MammalsFormRobot

class MammalsFormTestRobot : MammalsFormRobot {
    companion object {
        fun mammalsScreen(block: MammalsFormTestRobot.() -> Unit): MammalsFormTestRobot {
            return MammalsFormTestRobot().apply(block)
        }
    }

    fun isDisplayed(): ViewInteraction =
        toolbarWithTitle(R.string.entry_type_mammal).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    fun buttonSave(): ViewInteraction =
        onView(
            Matchers.allOf(
                Matchers.instanceOf(Button::class.java),
                ViewMatchers.withText(R.string.menu_entry_save)
            )
        )

    fun tabMain(): ViewInteraction = onView(ViewMatchers.withText(R.string.tab_required))

    fun tabOptional(): ViewInteraction =
        onView(ViewMatchers.withText(R.string.tab_optional))

    // Main fields
    fun fieldModeratorReview(): ViewInteraction =
        onView(ViewMatchers.withText(R.string.monitoring_moderator_review))

    fun fieldConfidential(): ViewInteraction =
        onView(ViewMatchers.withText(R.string.monitoring_mammal_private))

    fun fieldSpecies(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_name))

    fun fieldGender(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_gender))

    fun fieldAge(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_age))

    fun fieldCount(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_count))

    fun fieldHabitat(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_habitat))

    fun fieldFindings(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_mammal_danger_observation))

    fun fieldMarking(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_marking))

    fun fieldDistanceFromAxis(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_axis_distance))

    fun fieldThreats(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_common_threats))

    fun fieldNotes(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_notes))

    // Optional fields
    fun fieldL(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_mammal_L))

    fun fieldC(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_mammal_C))

    fun fieldA(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_mammal_A))

    fun fieldPl(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_mammal_Pl))

    fun fieldWeight(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_weight))

    fun fieldTempSubstrate(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_t_substrate))

    fun fieldTempAir(): ViewInteraction =
        onView(withHintParentOrOwn(R.string.monitoring_herp_t_vha))
}