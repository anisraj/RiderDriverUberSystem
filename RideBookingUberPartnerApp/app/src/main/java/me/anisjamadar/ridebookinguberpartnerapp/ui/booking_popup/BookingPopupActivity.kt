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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        fun start(
            context: Context,
            deliveryFrom: String,
            deliveryTo: String,
            time: Int,
            budget: Double,
            ridePickupLatitude: Double,
            ridePickupLongitude: Double,
            rideDropLatitude: Double,
            rideDropLongitude: Double
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
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // do nothing as user need to accept or reject
            }
        })
        setContent {
            LocationSharingApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) { innerPadding ->
                    BookingPopupScreen(
                        time,
                        deliveryFrom,
                        deliveryTo,
                        budget,
                        ridePickupLatitude,
                        ridePickupLongitude,
                        rideDropLatitude,
                        rideDropLongitude,
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
        wakeUpWhenLock()
    }
}

@Composable
fun BookingPopupScreen(
    time: Int,
    deliveryFrom: String,
    deliveryTo: String,
    budget: Double,
    ridePickupLatitude: Double,
    ridePickupLongitude: Double,
    rideDropLatitude: Double,
    rideDropLongitude: Double,
    modifier: Modifier
) {
    val viewModel = hiltViewModel<BookingPopupViewModel>()
    val finishPage by viewModel.finishPage.collectAsStateWithLifecycle()
    val startMain by viewModel.startMain.collectAsStateWithLifecycle()
    val mediaPlayer = MediaPlayer.create(LocalContext.current, R.raw.alarm)

    if (startMain) {
        LocalActivity.current?.let {
            val intent = MainActivity.start(
                it,
                ridePickupLatitude,
                ridePickupLongitude,
                rideDropLatitude,
                rideDropLongitude
            )
            it.startActivity(intent)
            it.finish()
        }
    }

    if (finishPage) {
        LocalActivity.current?.finish()
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
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp
        )
    ) {
        Budget(budget)
        ShowAddress(deliveryFrom, 1)
        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            thickness = 5.dp,
            color = Color.Green
        )
        ShowAddress(deliveryTo, 2)
        TimerLayout(viewModel)
        ActionButtons(viewModel)
    }
}

@Composable
fun Budget(budget: Double) {
    Box(
        modifier = Modifier
            .background(color = Color(0xFF8B93FF)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .padding(15.dp),
            color = Color.White,
            text = "BUDGET : ₹$budget",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ShowAddress(deliveryAddress: String, addressType: Int) {
    Column(
        modifier = Modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 10.dp,
            alignment = Alignment.CenterVertically
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 16.dp,
                alignment = Alignment.Start
            )
        ) {
            Image(
                painter = if (addressType == 1) {
                    painterResource(id = R.drawable.pickup_label)
                } else {
                    painterResource(id = R.drawable.drop_label)
                },
                contentDescription = "pickup_label",
                modifier = Modifier
                    .size(50.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = if (addressType == 1) {
                    "Pickup Address"
                } else {
                    "Delivery Address"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }

        Text(
            text = deliveryAddress,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 66.dp)
        )

    }
}

@Composable
fun TimerLayout(viewModel: BookingPopupViewModel) {
    val progress by viewModel.progressValue.collectAsStateWithLifecycle()
    val timerText by viewModel.timer.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .padding(top = 15.dp, bottom = 15.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 80.dp)
                .size(150.dp),
            contentAlignment = Alignment.Center
        ) {

            CircularProgressIndicator(
                progress = { progress.toFloat() / 100 },
                modifier = Modifier
                    .size(150.dp),
                color = Color.Green,
                trackColor = Color.LightGray,
                strokeWidth = 12.dp,
                strokeCap = StrokeCap.Round
            )

            Text(
                text = timerText,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

    }
}

@Composable
fun ActionButtons(viewModel: BookingPopupViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = 16.dp,
            alignment = Alignment.CenterHorizontally
        )
    ) {
        Button(
            modifier = Modifier
                .height(60.dp)
                .weight(weight = 1f),
            colors = ButtonColors(
                containerColor = Color.Red,
                contentColor = Color.White,
                disabledContentColor = Color.Red,
                disabledContainerColor = Color.White
            ),
            onClick = {
                viewModel.updateBooking(3)
            }
        ) {
            Text(
                text = "REJECT",
                fontWeight = FontWeight.Bold,
            )
        }

        Button(
            modifier = Modifier
                .height(60.dp)
                .weight(weight = 1f),
            colors = ButtonColors(
                containerColor = Color.Green,
                contentColor = Color.White,
                disabledContentColor = Color.Green,
                disabledContainerColor = Color.White
            ),
            onClick = {
                viewModel.updateBooking(2)
            }
        ) {
            Text(
                text = "ACCEPT",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(
    showBackground = true
)
@Composable
private fun BookingPopupPreview() {
    BookingPopupScreen(30,"Pune", "Mumbai", 20.0, 0.0, 0.0, 0.0, 0.0, Modifier)
}
