package me.anisjamadar.ridebookinguberpartnerapp.ui.booking_popup

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import me.anisjamadar.locationsharingapplication.R
import me.anisjamadar.ridebookinguberpartnerapp.ui.main.MainActivity
import me.anisjamadar.ridebookinguberpartnerapp.utils.wakeUpWhenLock
import me.anisjamadar.ridebookinguberpartnerapp.ui.theme.LocationSharingApplicationTheme

@AndroidEntryPoint
class BookingPopupActivity : ComponentActivity() {

    private val deliveryFrom: String
        get() = checkNotNull(intent.getStringExtra(DELIVERY_FROM))

    private val deliveryTo: String
        get() = checkNotNull(intent.getStringExtra(DELIVERY_TO))

    private val time: Int
        get() = checkNotNull(intent.getIntExtra(TIME, 20))

    private val distance: Double
        get() = checkNotNull(intent.getDoubleExtra(DISTANCE, 0.0))

    private val budget: Double
        get() = checkNotNull(intent.getDoubleExtra(BUDGET, 0.0))

    private val ridePickupLatitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_PICKUP_LATITUDE, 0.0))

    private val ridePickupLongitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_PICKUP_LONGITUDE, 0.0))

    private val rideDropLatitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_DROP_LATITUDE, 0.0))

    private val rideDropLongitude: Double
        get() = checkNotNull(intent.getDoubleExtra(RIDE_DROP_LONGITUDE, 0.0))

    companion object {
        private const val DELIVERY_FROM = "delivery_from"
        private const val DELIVERY_TO = "delivery_to"
        private const val TIME = "time"
        private const val BUDGET = "budget"
        private const val RIDE_PICKUP_LATITUDE = "rider_pickup_latitude"
        private const val RIDE_PICKUP_LONGITUDE = "rider_pickup_longitude"
        private const val RIDE_DROP_LATITUDE = "rider_drop_latitude"
        private const val RIDE_DROP_LONGITUDE = "rider_drop_longitude"
        private const val DISTANCE = "distance"

        fun start(
            context: Context,
            deliveryFrom: String,
            deliveryTo: String,
            time: Int,
            budget: Double,
            ridePickupLatitude: Double,
            ridePickupLongitude: Double,
            rideDropLatitude: Double,
            rideDropLongitude: Double,
            distance: Double
        ): Intent {
            return Intent(context, BookingPopupActivity::class.java).apply {
                putExtra(DELIVERY_FROM, deliveryFrom)
                putExtra(DELIVERY_TO, deliveryTo)
                putExtra(TIME, time)
                putExtra(BUDGET, budget)
                putExtra(RIDE_PICKUP_LATITUDE, ridePickupLatitude)
                putExtra(RIDE_PICKUP_LONGITUDE, ridePickupLongitude)
                putExtra(RIDE_DROP_LATITUDE, rideDropLatitude)
                putExtra(RIDE_DROP_LONGITUDE, rideDropLongitude)
                putExtra(DISTANCE, distance)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setFinishOnTouchOutside(false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // do nothing as user need to accept or reject
            }
        })
        setContent {
            RideRequestDialog(
                onRejectClick = {
                    finishActivity()
                },
                onAcceptClick = {
                    openRideStatus()
                },
                time,
                deliveryFrom,
                deliveryTo,
                budget,
                distance
            )
        }
        wakeUpWhenLock()
    }

    private fun openRideStatus() {
        val intent = MainActivity.start(
            this,
            ridePickupLatitude,
            ridePickupLongitude,
            rideDropLatitude,
            rideDropLongitude
        )
        startActivity(intent)
        finishActivity()
    }

    private fun finishActivity() {
        finish()
    }
}

@Composable
fun RideRequestDialog(
    onRejectClick: () -> Unit,
    onAcceptClick: () -> Unit,
    time: Int,
    deliveryFrom: String,
    deliveryTo: String,
    budget: Double,
    distance: Double
) {
    val viewModel = hiltViewModel<BookingPopupViewModel>()
    val mediaPlayer = MediaPlayer.create(LocalContext.current, R.raw.alarm)
    val finishPage by viewModel.finishPage.collectAsStateWithLifecycle()

    if (finishPage) {
        onRejectClick.invoke()
    }

    DisposableEffect(Unit) {
        viewModel.startTimer(time)
        mediaPlayer.start()

        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimerView(modifier = Modifier
            .zIndex(1f)
            .fillMaxWidth()
            .background(Color.Transparent),
            viewModel
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-45).dp)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ),
            color = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(58.dp))

                Text(
                    text = "$distance km Ride!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier
                    .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Green, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(28.dp)
                                .background(Color.Gray)
                                .align(Alignment.CenterHorizontally)
                        )

                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Red, CircleShape)
                        )
                    }

                    Column {
                        Text(
                            text = deliveryFrom,
                            fontSize = 14.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = deliveryTo,
                            fontSize = 14.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(),
                    color = Color(0xFFE2E2E2)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TOTAL BUDGET",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Text(
                        text = "₹$budget",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(38.dp))

                Row(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 25.dp
                    )
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f),
                        border = BorderStroke(width = 2.dp, color = Color.Red),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            viewModel.updateBooking(3)
                            onRejectClick.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "REJECT"
                        )
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        onClick = {
                            viewModel.updateBooking(2)
                            onAcceptClick.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "ACCEPT"
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun TimerView(modifier: Modifier, viewModel: BookingPopupViewModel) {
    val progress by viewModel.progressValue.collectAsStateWithLifecycle()
    val timerText by viewModel.timer.collectAsStateWithLifecycle()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(95.dp)
                .clip(shape = CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                progress = { progress.toFloat() / 100 },
                modifier = Modifier
                    .size(95.dp),
                color = Color.Green,
                trackColor = Color.LightGray,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )

            Text(
                text = timerText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

    }
}
