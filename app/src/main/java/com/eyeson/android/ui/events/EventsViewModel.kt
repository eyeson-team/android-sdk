package com.eyeson.android.ui.events

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val _newEvent = MutableSharedFlow<Event>(replay = 0)
    val newEvent: SharedFlow<Event> = _newEvent

    val events = mutableListOf<Event>()

    fun newEvent(event: Event) {
        events.add(0, event)
        viewModelScope.launch {
            _newEvent.emit(event)
        }
    }
}