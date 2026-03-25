package com.zuvy.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zuvy.app.search.SearchEngine
import com.zuvy.app.search.SearchFilters
import com.zuvy.app.search.SearchResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchEngine: SearchEngine
) : ViewModel() {
    
    private val _searchResults = MutableStateFlow<SearchResults>(SearchResults.Empty)
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            searchEngine.searchHistory.collect { history ->
                _searchHistory.value = history
            }
        }
    }
    
    fun search(query: String, filters: SearchFilters = SearchFilters()) {
        viewModelScope.launch {
            searchEngine.search(query, filters)
        }
    }
    
    fun getSuggestions(query: String) {
        viewModelScope.launch {
            searchEngine.search(query.substring(0, 2.coerceAtMost(query.length)))
        }
    }
    
    fun clearHistory() {
        searchEngine.clearHistory()
    }
    
    fun removeFromHistory(query: String) {
        searchEngine.removeFromHistory(query)
    }
}
