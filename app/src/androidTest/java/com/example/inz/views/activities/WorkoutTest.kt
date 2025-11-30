package com.example.inz.views.activities


import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.inz.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class WorkoutTest {

    @Rule
    @JvmField
    var mActivityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun workoutTest() {
        val bottomNavigationItemView = onView(
            allOf(
                withId(R.id.workout), withContentDescription("Workout"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.bottom_navigation),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        bottomNavigationItemView.perform(click())

        val materialButton = onView(
            allOf(
                withId(R.id.startWorkoutBtn), withText("Start New Workout"),
                childAtPosition(
                    allOf(
                        withId(R.id.prebuiltWorkoutsContainer),
                        childAtPosition(
                            withClassName(`is`("android.widget.ScrollView")),
                            0
                        )
                    ),
                    0
                )
            )
        )
        materialButton.perform(scrollTo(), click())

        val materialButton2 = onView(
            allOf(
                withId(android.R.id.button1), withText("Start"),
                childAtPosition(
                    childAtPosition(
                        withId(com.google.android.material.R.id.buttonPanel),
                        0
                    ),
                    3
                )
            )
        )
        materialButton2.perform(scrollTo(), click())

        val materialButton3 = onView(
            allOf(
                withId(R.id.btnAddExercise), withText("Add exercise"),
                childAtPosition(
                    allOf(
                        withId(R.id.exerciseContainer),
                        childAtPosition(
                            allOf(
                                withId(R.id.exercisesScrollView),
                                withContentDescription("com.google.android.material.appbar.AppBarLayout\$ScrollingViewBehavior")
                            ),
                            0
                        )
                    ),
                    0
                )
            )
        )
        materialButton3.perform(scrollTo(), click())
        runBlocking {
            delay(2000)
        }
        val recyclerView = onView(
            allOf(
                withId(R.id.fragment_list),
                childAtPosition(
                    withClassName(`is`("android.widget.LinearLayout")),
                    5
                )
            )
        )
        recyclerView.perform(actionOnItemAtPosition<ViewHolder>(6, click()))

        val appCompatEditText = onView(
            allOf(
                withId(R.id.weightInput), withText("0.0"),
                childAtPosition(
                    allOf(
                        withId(R.id.setComponent),
                        childAtPosition(
                            withId(R.id.setContainer),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText.perform(click())

        val appCompatEditText2 = onView(
            allOf(
                withId(R.id.weightInput), withText("0.0"),
                childAtPosition(
                    allOf(
                        withId(R.id.setComponent),
                        childAtPosition(
                            withId(R.id.setContainer),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText2.perform(replaceText("50"))

        val appCompatEditText3 = onView(
            allOf(
                withId(R.id.weightInput), withText("50"),
                childAtPosition(
                    allOf(
                        withId(R.id.setComponent),
                        childAtPosition(
                            withId(R.id.setContainer),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        appCompatEditText3.perform(closeSoftKeyboard())

        val appCompatEditText4 = onView(
            allOf(
                withId(R.id.repsInput), withText("0"),
                childAtPosition(
                    allOf(
                        withId(R.id.setComponent),
                        childAtPosition(
                            withId(R.id.setContainer),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatEditText4.perform(replaceText("10"))

        val appCompatEditText5 = onView(
            allOf(
                withId(R.id.repsInput), withText("10"),
                childAtPosition(
                    allOf(
                        withId(R.id.setComponent),
                        childAtPosition(
                            withId(R.id.setContainer),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatEditText5.perform(closeSoftKeyboard())

        val appCompatEditText6 = onView(
            allOf(
                withId(R.id.exerciseDesc),
                childAtPosition(
                    allOf(
                        withId(R.id.exerciseComponent),
                        childAtPosition(
                            withId(R.id.exerciseContainer),
                            0
                        )
                    ),
                    1
                )
            )
        )
        appCompatEditText6.perform(scrollTo(), replaceText("Wide grip"), closeSoftKeyboard())

        val materialButton4 = onView(
            allOf(
                withId(R.id.btnFinishWorkout), withText("Finish"),
                childAtPosition(
                    childAtPosition(
                        withClassName(`is`("android.widget.LinearLayout")),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        materialButton4.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
