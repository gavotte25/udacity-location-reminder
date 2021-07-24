package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.*
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest: AutoCloseKoinTest() {
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var repoModule: Module
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun setup() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                createModifiedViewModel(
                        appContext,
                        get() as ReminderDataSource,
                        get()
                )
            }
            single {
                SaveReminderViewModel(
                        appContext,
                        get() as ReminderDataSource
                )
            }

            single { LocalDB.createRemindersDao(appContext) }
        }
        repoModule = module {
            single { RemindersLocalRepository(get()) as ReminderDataSource }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule, repoModule))
        }

        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            repository.deleteAllReminders()
        }
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
    fun whenNotLogin_thenNavigateToAuthenticationActivity() {
        loadAuthModule(RemindersListViewModel.AuthenticationState.UNAUTHENTICATED)
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()))
    }

    @Test
    fun givenAfterLogin_whenClickFAB_thenNavigateToSaveReminderFragment() {
        loadAuthModule(RemindersListViewModel.AuthenticationState.AUTHENTICATED)
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            it.view?.let { it1 -> Navigation.setViewNavController(it1, navController) }
        }
        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun givenAfterLogin_whenNoData_thenShowNoData() {
        loadAuthModule(RemindersListViewModel.AuthenticationState.AUTHENTICATED)
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun givenAfterLogin_whenHasData_thenDataIsDisplayed(){
        runBlocking {
            loadAuthModule(RemindersListViewModel.AuthenticationState.AUTHENTICATED)
            for(i in 1..2) {
                repository.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
            }
            val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
            dataBindingIdlingResource.monitorFragment(scenario)
            onView(withText("title1")).check(matches(isDisplayed()))
            onView(withText("title2")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun givenAfterLogin_whenLoadingStartToEnd_ProgressBarAppearAndDisappear() {
        runBlocking{
            loadAuthModule(RemindersListViewModel.AuthenticationState.AUTHENTICATED)
            for(i in 1..2) {
                repository.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
            }
            val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
            dataBindingIdlingResource.monitorFragment(scenario)
            onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
            delay(1000)
            onView(withId(R.id.progressBar)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun givenAfterLogin_whenLoadDataError_thenSnackBarPopup() {
        runBlocking{
            loadAuthModule(RemindersListViewModel.AuthenticationState.AUTHENTICATED)
            swapFakeRepoModule(repoModule)
            for(i in 1..2) {
                repository.saveReminder(ReminderDTO("title$i", "description$i","location$i",i.toDouble(),i.toDouble()))
            }
            val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
            dataBindingIdlingResource.monitorFragment(scenario)
            onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Error")))
        }
    }
}