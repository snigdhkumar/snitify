package com.snitrix.snitify.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snitrix.snitify.data.datastore.OnboardingDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(context: Context) : ViewModel() {

    private val dataStore = OnboardingDataStore(context)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _selectedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val selectedLanguages: StateFlow<Set<String>> = _selectedLanguages.asStateFlow()

    private val _selectedArtists = MutableStateFlow<Set<String>>(emptySet())
    val selectedArtists: StateFlow<Set<String>> = _selectedArtists.asStateFlow()

    private val _isOnboardingFinished = MutableStateFlow<Boolean?>(null)
    val isOnboardingFinished: StateFlow<Boolean?> = _isOnboardingFinished.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.isOnboardingCompleted.collect { completed ->
                _isOnboardingFinished.value = completed
            }
        }
    }

    fun toggleLanguage(language: String) {
        val current = _selectedLanguages.value.toMutableSet()
        if (current.contains(language)) {
            current.remove(language)
        } else {
            current.add(language)
        }
        _selectedLanguages.value = current
    }

    fun toggleArtist(artist: String) {
        val current = _selectedArtists.value.toMutableSet()
        if (current.contains(artist)) {
            current.remove(artist)
        } else {
            current.add(artist)
        }
        _selectedArtists.value = current
    }

    fun nextStep() {
        val step = _currentStep.value
        if (step == 1 && _selectedLanguages.value.isEmpty()) {
            return // Require at least 1 language selection before proceeding
        }
        if (step == 2 && _selectedArtists.value.size < 3) {
            return // Require at least 3 artist selections before proceeding
        }
        if (step < 3) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun finishOnboarding() {
        if (_selectedLanguages.value.isEmpty()) {
            _currentStep.value = 1
            return
        }
        if (_selectedArtists.value.size < 3) {
            _currentStep.value = 2
            return
        }
        viewModelScope.launch {
            dataStore.savePreferences(_selectedLanguages.value, _selectedArtists.value)
            _isOnboardingFinished.value = true
        }
    }
}
