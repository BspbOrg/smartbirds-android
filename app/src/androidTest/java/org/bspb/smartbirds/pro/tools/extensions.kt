package org.bspb.smartbirds.pro.tools

import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.material.textfield.TextInputLayout
import org.bspb.smartbirds.pro.tools.form.entry.FormEntry
import org.bspb.smartbirds.pro.tools.matcher.FormEntryMatcher
import org.bspb.smartbirds.pro.tools.robot.MultipleChoiceDialogTestRobot.Companion.multipleChoiceDialog
import org.bspb.smartbirds.pro.tools.robot.SingleChoiceDialogTestRobot.Companion.singleChoiceDialog
import org.bspb.smartbirds.pro.ui.views.NomenclatureItem
import org.bspb.smartbirds.pro.ui.views.ZoneFormInput
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo

fun toolbarWithTitle(@StringRes title: Int): ViewInteraction =
    onView(allOf(withText(title), withParent(isAssignableFrom(Toolbar::class.java))))


// Matchers
fun nomenclatureWithLabel(label: String): Matcher<NomenclatureItem> {
    return object :
        BaseMatcher<NomenclatureItem>() {
        override fun matches(item: Any?): Boolean {
            checkNotNull(item)
            if (item !is NomenclatureItem) return false
            return equalTo(label).matches(item.nomenclature?.label?.get("en"))
        }

        override fun describeTo(description: Description) {
            description.appendText("nomenclature with label: ")
        }
    }
}

fun speciesWithLabel(label: String): Matcher<NomenclatureItem> {
    return object :
        BaseMatcher<NomenclatureItem>() {
        override fun matches(item: Any?): Boolean {
            checkNotNull(item)
            if (item !is NomenclatureItem) return false
            return equalTo(label).matches(item.nomenclature?.label?.labelId)
        }

        override fun describeTo(description: Description) {
            description.appendText("species with label: ")
        }
    }
}

fun zoneWithLabel(label: String): Matcher<ZoneFormInput.ZoneHolder> {
    return object :
        BaseMatcher<ZoneFormInput.ZoneHolder>() {
        override fun matches(item: Any?): Boolean {
            checkNotNull(item)
            if (item !is ZoneFormInput.ZoneHolder) return false
            return equalTo(label).matches(item.label)
        }

        override fun describeTo(description: Description) {
            description.appendText("nomenclature with label: ")
        }
    }
}

fun withHintParentOrOwn(resourceId: Int): Matcher<View> =
    object : BoundedMatcher<View, EditText>(EditText::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("with hint: ")
        }

        override fun matchesSafely(item: EditText): Boolean {
            val parent = item.parent.parent
            val expectedHint = item.resources.getString(resourceId)
            return if (parent is TextInputLayout) equalTo(expectedHint).matches(parent.hint) else equalTo(
                expectedHint
            ).matches(item.hint)
        }

    }

fun hasFormEntry(entry: FormEntry): Matcher<String> = FormEntryMatcher(entry)
// End matchers

fun selectSingleChoice(viewInteraction: ViewInteraction, text: String) {
    viewInteraction.perform(scrollTo(), click())
    singleChoiceDialog {
        onRow(text).perform(scrollTo(), click())
    }
}

fun selectMultipleChoice(viewInteraction: ViewInteraction, values: Array<String>) {
    viewInteraction.perform(scrollTo(), click())
    multipleChoiceDialog {
        values.forEach {
            onRow(it).perform(scrollTo(), click())
        }
        buttonOk().perform(click())
    }
}

fun selectSpecies(viewInteraction: ViewInteraction, text: String) {
    viewInteraction.perform(scrollTo(), click())
    singleChoiceDialog {
        onSpeciesRow(text).perform(scrollTo(), click())
    }
}

fun fillTextField(viewInteraction: ViewInteraction, text: String) {
    viewInteraction.perform(scrollTo(), typeText(text))
}

fun selectZone(viewInteraction: ViewInteraction, text: String) {
    viewInteraction.perform(scrollTo(), click())
    singleChoiceDialog {
        onZoneRow(text).perform(scrollTo(), click())
    }
}
