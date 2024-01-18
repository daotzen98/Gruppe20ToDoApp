package com.example.gruppe20todoapp.screens

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gruppe20todoapp.BuildConfig
import com.example.gruppe20todoapp.api.GiphyApiService
import com.example.gruppe20todoapp.database.TodoEntity
import com.example.gruppe20todoapp.reps.TodoRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class MainVM:ViewModel(),KoinComponent {
    private val giphyApiService: GiphyApiService by inject()
    private val repo: TodoRepo by inject()
    private val _filterState: MutableStateFlow<FilterState> = MutableStateFlow(FilterState.ALL)
    private val _tasks:MutableStateFlow<List<TodoEntity>> = MutableStateFlow(emptyList())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    val tasks = _tasks.asStateFlow()
    private val _gifUrl = MutableStateFlow<String?>(null)
    val gifUrl: StateFlow<String?> = _gifUrl.asStateFlow()


    init {
        getTodos()
    }

    enum class FilterState {
        ALL,
        COMPLETED,
        NOT_COMPLETED
    }

    private fun getTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            when (_filterState.value) {
                FilterState.ALL -> repo.getAllTasks()
                FilterState.COMPLETED -> repo.getCompletedTasks()
                FilterState.NOT_COMPLETED -> repo.getNotCompletedTasks()
            }.collect { data ->
                _tasks.value = data
            }
        }
    }


    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.updateTasks(todo)
            getTodos()

            if (todo.done) {
                fetchCelebratoryGif("celebration")
            }
        }
    }

    private fun fetchCelebratoryGif(tag: String) {
        viewModelScope.launch {
            try {
                val response = giphyApiService.getRandomGif(BuildConfig.GIPHY_API_KEY, tag)
                val gifUrl = response.data.images.original.url
                _gifUrl.value = gifUrl
                Log.d("MainVM", "GIF URL fetched: $gifUrl") // Log the URL
            } catch (e: Exception) {
                _gifUrl.value = null
                Log.e("MainVM", "Error fetching GIF: ${e.message}")
            }
        }
    }
    fun clearGifUrl() {
        _gifUrl.value = null
    }

    fun deleteTodo(todo: TodoEntity){
        viewModelScope.launch(Dispatchers.IO){
            repo.deleteTasks(todo)
        }
    }
    fun addTodo(todo: TodoEntity){
        viewModelScope.launch(Dispatchers.IO) {
            repo.addTasks(todo)
            getTodos()
        }
    }


    fun showAllTasks() {
        _filterState.value = FilterState.ALL
        getTodos()
    }

    fun showCompletedTasks() {
        _filterState.value = FilterState.COMPLETED
        getTodos()
    }

    fun showNotCompletedTasks() {
        _filterState.value = FilterState.NOT_COMPLETED
        getTodos()
    }

    fun editTodo(todo: TodoEntity, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            val updatedTodo = todo.copy(title = newTitle, description = newDescription)
            updateTodo(updatedTodo)
        }
    }
    fun searchTasks(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.searchTasks(query).collect { results ->
                _tasks.value = results
            }
        }
    }


}