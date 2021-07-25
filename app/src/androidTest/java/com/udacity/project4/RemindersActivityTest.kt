package com.udacity.project4

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.ReminderDescriptionActivity.Companion.EXTRA_ReminderDataItem
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.splash.SplashActivity
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = if(Build.VERSION.SDK_INT < 29) {
        GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
    } else {
        GrantPermissionRule.grant(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

        FirebaseAuth.getInstance().signOut()
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unRegisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun testFromBeginToSelectLocation() = runBlocking {
        val scenario = ActivityScenario.launch(SplashActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        // wait for splash screen navigating, check if login AuthenticationActivity is created
        delay(1000)
        onView(withText(R.string.welcome_to_the_location_reminder_app)).check(matches(isDisplayed()))
        // login flow (using already signed up account
        onView(withId(R.id.btn_login)).perform(click())
        delay(1000)
        onView(withText("Sign in with email")).perform((click()))
        onView(withHint("Email")).perform(typeText("gavotte.tran@gmail.com"))
        onView(withText("Next")).perform(click())
        onView(withHint("Password")).perform(typeText("password"))
        onView(withText("SIGN IN")).perform(click())
        // save reminder flow (not include selecting location)
        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("New reminder"))
        onView(withId(R.id.reminderDescription)).perform(typeText("This is the description"))
        onView(withId(R.id.selectLocation)).perform(click())
        // navigate back and try to save reminder without location selected
        pressBack()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))
        // navigate back to ReminderListFragment and logout
        onView(allOf(withContentDescription("Navigate up"))).perform(click())
        onView(withId(R.id.logout)).perform(click())
        onView(withText(R.string.welcome_to_the_location_reminder_app)).check(matches(isDisplayed()))
        scenario.close()
    }

    // This test should be executed with API 29 and below
    @Test
    fun testPressBackWhenLogin() = runBlocking {
        val scenario = ActivityScenario.launch(AuthenticationActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)
        onView(withId(R.id.btn_login)).perform(click())
        delay(1000)
        onView(withText("Sign in with email")).check(matches(isDisplayed()))
        pressBack()
        onView(withText(containsString("Authentication failed with code")))
                .inRoot(withDecorView(not(`is`(getActivity(scenario).window.decorView))))
                .check(matches(isDisplayed()))

        scenario.close()    }

    @Test
    fun testOpenReminderDescriptionActivity() = runBlocking {
        val reminderDTO = ReminderDTO("new reminder", "some description", "Somewhere", 100.0, 100.0, "1")
        val reminderDataItem = ReminderDataItem(
                reminderDTO.title,
                reminderDTO.description,
                reminderDTO.location,
                reminderDTO.latitude,
                reminderDTO.longitude,
                reminderDTO.id
        )
        repository.saveReminder(reminderDTO)
        val intent = ReminderDescriptionActivity.newIntent(appContext, reminderDataItem)
        val scenario = ActivityScenario.launch<ReminderDescriptionActivity>(intent)
        dataBindingIdlingResource.monitorActivity(scenario)
        onView(withId(R.id.txv_title)).check(matches(withText(reminderDTO.title)))
        onView(withId(R.id.txv_location)).check(matches(withText(reminderDTO.location)))
        onView(withId(R.id.txv_location)).check(matches(withText(reminderDTO.location)))
        onView(withId(R.id.btn_back)).perform(click())
        onView(withId(R.id.addReminderFAB)).check(matches(isDisplayed()))
        scenario.close()
    }

    private fun getActivity(scenario: ActivityScenario<AuthenticationActivity>): Activity {
        var activity: Activity? = null
        scenario.onActivity {
            activity = it
        }
        return activity!!
    }

}

//onView(allOf(instanceOf(TextView::class.java), withParent(withResourceName("action_bar")))).check(matches(withText("Location Reminders")))