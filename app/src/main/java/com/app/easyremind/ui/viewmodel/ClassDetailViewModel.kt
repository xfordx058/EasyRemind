package com.app.easyremind.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.app.easyremind.data.ClassEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClassDetailViewModel(app: Application, classId: Long) : AndroidViewModel(app) {

    private val repository = app.container().repository

    private val _classState = MutableStateFlow<ClassEntity?>(null)
    val classState = _classState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.classesFlow.collect { list ->
                _classState.value = list.firstOrNull { it.id == classId }
            }
        }
    }

    fun toggleEnabled(clazz: ClassEntity, enabled: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.setEnabled(clazz, enabled)
            } catch (e: Exception) {
                onError(e.message ?: "Something went wrong.")
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteClass(id)
        }
    }

    companion object {
        fun factory(classId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                ClassDetailViewModel(app, classId)
            }
        }
    }
}