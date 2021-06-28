package org.bspb.smartbirds.pro.tools

import android.view.View
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.material.textfield.TextInputLayout
import org.bspb.smartbirds.pro.ui.views.NomenclatureItem
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo

fun toolbarWithTitle(@StringRes title: Int): ViewInteraction =
    onView(allOf(withText(title), withParent(isAssignableFrom(Toolbar::class.java))))

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
