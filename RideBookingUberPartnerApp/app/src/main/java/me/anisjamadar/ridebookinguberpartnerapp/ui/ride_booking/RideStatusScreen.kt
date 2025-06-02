package me.anisjamadar.ridebookinguberpartnerapp.ui.ride_booking

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ola.mapsdk.interfaces.OlaMapCallback
import com.ola.mapsdk.view.OlaMap
import com.ola.mapsdk.view.OlaMapView
import me.anisjamadar.grpc.GrpcConstant
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.locationsharingapplication.R
import me.anisjamadar.ridebookinguberpartnerapp.ui.main.MainViewModel
import kotlin.math.max

@Composable
fun RideStatusScreen(mainViewModel: MainViewModel) {
    val rideStatusViewModel = hiltViewModel<RideStatusViewModel>()
    val status by rideStatusViewModel.rideStatus.collectAsStateWithLifecycle()
    var map by remember { mutableStateOf<OlaMap?>(null) }

    var isMapReady by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mapView by remember {
        mutableStateOf(OlaMapView(context).also {
            it.getMap(
                apiKey = BuildConfig.API_KEY,
                olaMapCallback = object : OlaMapCallback {
                    override fun onMapError(error: String) {}
                    override fun onMapReady(olaMap: OlaMap) {
                        map = olaMap
                        map?.showCurrentLocation()
                        isMapReady = true
                    }
                }
            )
        })
    }

    Scaffold { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED -> "Heading to pickup"
                    GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> "Arrived to pickup"
                    GrpcConstant.DriverAction.RIDE_STARTED -> "On the way to drop"
                    else -> {
                        "Ride completed"
                    }
                },
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )

            LocationItemLayout(mainViewModel, status)

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { mapView }
            )

            Box(modifier = Modifier
                .height(200.dp)
                .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SliderButton(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    text = when(status) {
                        GrpcConstant.DriverAction.ACCEPTED -> "Arrived to pickup"
                        GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> "Start the ride"
                        GrpcConstant.DriverAction.RIDE_STARTED -> "Ride completed"
                        else -> {
                            ""
                        }
                    }
                ) {
                    rideStatusViewModel.updateStatus()
                }
            }


        }
    }
}

@Composable
fun LocationItemLayout(mainViewModel: MainViewModel, status: GrpcConstant.DriverAction) {
    val pickAddress by mainViewModel.pickupAddress.collectAsStateWithLifecycle()
    val dropAddress by mainViewModel.dropAddress.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
            .height(85.dp),
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
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED -> pickAddress.first
                    GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> dropAddress.first
                    else -> { "" }
                },
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = when(status) {
                    GrpcConstant.DriverAction.ACCEPTED -> pickAddress.second
                    GrpcConstant.DriverAction.ARRIVED_AT_PICKUP -> dropAddress.second
                    else -> { "" }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SliderButton(
    modifier: Modifier = Modifier,
    text: String,
    onConfirmed: () -> Unit
) {
    val threshold = 0.8f
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(Color.Green)
        .pointerInput(Unit) {
            containerWidth = size.width.toFloat()
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (sliderPosition >= threshold) {
                        onConfirmed()
                    }
                    sliderPosition = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    sliderPosition = (sliderPosition + dragAmount / containerWidth)
                        .coerceIn(0f, 1f)
                }
            )
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFF6200EE),
            style = TextStyle(

                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        val knobWidth = 70.dp
        Box(
            modifier = Modifier
                .offset {
                    val maxOffset = max(0f, ((containerWidth + 16.dp.toPx()) - knobWidth.toPx()))
                    val offset = sliderPosition * maxOffset
                    IntOffset(offset.toInt(), 0)
                }
                .width(knobWidth)
                .fillMaxHeight()
                .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                .background(Color(0xFF6200EE))
                .align(Alignment.CenterStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "",
                tint = Color.White,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun RideStatusPreview() {
    SliderButton(text = "confirm", onConfirmed = {})
}