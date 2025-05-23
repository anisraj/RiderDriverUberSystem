package me.anisjamadar.ridebookinguberriderapp.ui.location_search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ola.maps.sdk.model.places.response.AutocompleteResponse
import com.ola.maps.sdk.model.places.response.Predictions
import me.anisjamadar.ridebookinguberriderapp.R
import me.anisjamadar.ridebookinguberriderapp.ui.base.ShowError
import me.anisjamadar.ridebookinguberriderapp.ui.base.ShowLoading
import me.anisjamadar.ridebookinguberriderapp.ui.base.UiState
import me.anisjamadar.ridebookinguberriderapp.ui.map_screen.FOR_PICKUP

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    onBackClick: () -> Unit,
    locationFor: String,
    navController: NavHostController
) {
    val viewModel = hiltViewModel<LocationSearchViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Plan your ride",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick.invoke()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.down_arrow),
                            contentDescription = "back button"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            TextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.onQueryChange(it)
                },
                shape = RoundedCornerShape(5.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    disabledContainerColor = Color(0xFFE0E0E0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                placeholder = {
                    if (locationFor == FOR_PICKUP) {
                        Text(text = "From?")
                    } else {
                        Text(text = "Where?")
                    }
                }
            )

            LocationSearchContent(uiState, locationFor, navController)
        }
    }
}

@Composable
fun LocationSearchContent(
    uiState: UiState<AutocompleteResponse>,
    locationFor: String,
    navController: NavHostController
) {
    when (uiState) {
        is UiState.Loading -> {
            ShowLoading()
        }

        is UiState.Error -> {
            ShowError(uiState.message)
        }

        is UiState.Success -> {
            LocationsList(uiState.data.predictions, locationFor, navController)
        }

        else -> {

        }
    }
}

@Composable
fun LocationsList(
    predictions: ArrayList<Predictions>,
    locationFor: String,
    navController: NavHostController
) {
    LazyColumn(
        modifier = Modifier
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(
            space = 10.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        items(predictions, key = { prediction -> prediction.placeId ?: Unit }) { item ->
            LocationItemLayout(item, locationFor, navController)
        }
    }
}

@Composable
fun LocationItemLayout(
    prediction: Predictions,
    locationFor: String,
    navController: NavHostController
) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .height(85.dp)
            .clickable {
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(LOCATION_FOR, locationFor)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(MAIN_TEXT, prediction.structuredFormatting?.mainText)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(LATITUDE, prediction.geometry?.location?.lat)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(LONGITUDE, prediction.geometry?.location?.lng)
                navController.popBackStack()
            },
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.Start
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.location_icon),
            contentDescription = "image"
        )

        Column {
            Text(
                text = prediction.structuredFormatting?.mainText ?: "Main text not available",
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = prediction.structuredFormatting?.secondaryText ?: "Secondary text not available",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = prediction.description ?: "Description not available",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchScreenPreview() {
    LocationItemLayout(Predictions(), FOR_PICKUP, rememberNavController())
}

const val MAIN_TEXT = "main_text"
const val LATITUDE = "latitude"
const val LONGITUDE = "longitude"
const val LOCATION_FOR = "location_for"