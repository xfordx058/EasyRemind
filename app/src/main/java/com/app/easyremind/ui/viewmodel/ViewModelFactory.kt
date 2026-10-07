package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.easyremind.EasyRemindApp
import com.app.easyremind.focus.FocusViewModel
import com.app.easyremind.ui.viewmodel.HomeViewModel
import com.app.easyremind.ui.viewmodel.ScheduleViewModel
import com.app.easyremind.ui.viewmodel.SettingsViewModel

internal fun CreationExtras.requireApp(): Application =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        ?: error("Application missing from CreationExtras")

internal fun Application.container() = (this as EasyRemindApp).container

object ViewModelFactory {
    val home: ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(requireApp()) }
    }

    val schedule: ViewModelProvider.Factory = viewModelFactory {
        initializer { ScheduleViewModel(requireApp()) }
    }

    val focus: ViewModelProvider.Factory = viewModelFactory {
        initializer { FocusViewModel(requireApp()) }
    }

    val settings: ViewModelProvider.Factory = viewModelFactory {
        initializer { SettingsViewModel(requireApp()) }
    }
}