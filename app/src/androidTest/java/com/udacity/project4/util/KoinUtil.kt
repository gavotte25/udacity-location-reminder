package com.udacity.project4.util

import android.app.Application
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.AndroidTestFakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.splash.SplashActivity
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module

fun loadAuthModule(authState: RemindersListViewModel.AuthenticationState) {
    val authModule = module {
        factory {
            authState
        }
    }
    loadKoinModules(authModule)
}

fun createModifiedViewModel(app: Application, dataSource: ReminderDataSource, authState: RemindersListViewModel.AuthenticationState ): RemindersListViewModel {
    val vm = RemindersListViewModel(app, dataSource)
    vm.setAuthenticationState(authState)
    return vm
}

fun swapFakeRepoModule(repoModule: Module) {
    unloadKoinModules(repoModule)
    val fakeRepoModule = module {
        single { AndroidTestFakeDataSource(arrayListOf(), true) as ReminderDataSource }
    }
    loadKoinModules(fakeRepoModule)
}