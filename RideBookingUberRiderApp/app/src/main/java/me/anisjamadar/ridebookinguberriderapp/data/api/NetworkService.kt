package me.anisjamadar.ridebookinguberriderapp.data.api

import com.ola.maps.sdk.model.directions.response.DirectionsResponse
import me.anisjamadar.ridebookinguberriderapp.BuildConfig
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Singleton

@Singleton
interface NetworkService {
    @POST("/routing/v1/directions")
    suspend fun getEncodedPolyline(@Query("origin") origin: String,
                                   @Query("destination") destination: String,
                                   @Query("api_key") apiKey: String = BuildConfig.API_KEY): DirectionsResponse
}