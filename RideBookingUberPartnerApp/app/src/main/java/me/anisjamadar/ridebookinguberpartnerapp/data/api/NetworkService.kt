package me.anisjamadar.ridebookinguberpartnerapp.data.api

import com.ola.maps.sdk.model.directions.response.DirectionsResponse
import com.ola.maps.sdk.model.reversegeocode.response.ReverseGeocodingResponse
import me.anisjamadar.locationsharingapplication.BuildConfig
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Singleton

@Singleton
interface NetworkService {
    @GET("/places/v1/reverse-geocode")
    suspend fun getAddressByReverseGeocode(@Query("latlng") latlng: String,
                                           @Query("api_key") apiKey: String = BuildConfig.API_KEY): ReverseGeocodingResponse

    @POST("/routing/v1/directions")
    suspend fun getEncodedPolyline(@Query("origin") origin: String,
                                   @Query("destination") destination: String,
                                   @Query("api_key") apiKey: String = BuildConfig.API_KEY): DirectionsResponse
}