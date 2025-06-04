package me.anisjamadar.ridebookinguberriderapp.data.api

import com.ola.maps.sdk.model.directions.response.DirectionsResponse
import me.anisjamadar.ridebookinguberriderapp.BuildConfig
import me.anisjamadar.ridebookinguberriderapp.data.model.DistanceMatrixResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import javax.inject.Singleton

@Singleton
interface NetworkService {
    @POST("/routing/v1/directions")
    suspend fun getEncodedPolyline(@Query("origin") origin: String,
                                   @Query("destination") destination: String,
                                   @Query("api_key") apiKey: String = BuildConfig.API_KEY): DirectionsResponse

    @GET("/routing/v1/distanceMatrix")
    suspend fun getDistanceMatrix(@Query("origins") origin: String,
                                  @Query("destinations") destination: String,
                                  @Query("api_key") apiKey: String = BuildConfig.API_KEY): DistanceMatrixResponse
}