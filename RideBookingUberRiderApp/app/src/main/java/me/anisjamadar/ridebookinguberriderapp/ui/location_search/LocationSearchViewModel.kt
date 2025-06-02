package me.anisjamadar.ridebookinguberriderapp.ui.location_search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ola.maps.sdk.core.client.Platform
import com.ola.maps.sdk.core.config.PlatformConfig
import com.ola.maps.sdk.model.places.request.AutocompleteRequest
import com.ola.maps.sdk.model.places.response.AutocompleteResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.anisjamadar.ridebookinguberriderapp.BuildConfig
import me.anisjamadar.ridebookinguberriderapp.ui.base.UiState
import javax.inject.Inject

@HiltViewModel
class LocationSearchViewModel @Inject constructor() : ViewModel() {
    private val platFormConfig = PlatformConfig.Builder()
        .apiKey(BuildConfig.API_KEY)
        .baseUrl("https://api.olamaps.io")
        .maxRetryAttempts(3)
        .build()

    private val placesClient = Platform.getPlacesClient(platFormConfig)

    private val _uiState = MutableStateFlow<UiState<AutocompleteResponse>>(UiState.Initial)
    val uiState = _uiState.asStateFlow()
    private val _query = MutableStateFlow("")

    init {
        fetchLocations()
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun fetchLocations() {
        viewModelScope.launch {
            _query
                .debounce(500)
                .filter { query ->
                    return@filter query.isNotEmpty()
                }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    _uiState.value = UiState.Loading
                    val autocompleteRequest = AutocompleteRequest.Builder()
                        .queryText(query)
                        .build()
                    flow {
                        emit(placesClient.autocomplete(autocompleteRequest))
                    }
                }
                .flowOn(Dispatchers.IO)
                .catch {
                    _uiState.value = UiState.Error(it.toString())
                }
                .collectLatest {
                    _uiState.value = UiState.Success(it)
                }
        }
    }

}