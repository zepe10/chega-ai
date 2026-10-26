package br.com.habil.carona.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CaronaApi {
    @GET("rides")
    suspend fun getRides(): List<RideDto>

    @POST("rides")
    suspend fun createRide(@Body ride: RideDto): RideDto

    @PUT("rides/{id}")
    suspend fun updateRide(@Path("id") id: String, @Body ride: RideDto): RideDto

    @DELETE("rides/{id}")
    suspend fun deleteRide(@Path("id") id: String): Response<Unit>
}

@Serializable
data class RideDto(
    val id: JsonPrimitive? = null,
    val rideId: String? = null,
    val driverEmail: String,
    val driverName: String,
    val origin: String,
    val destination: String,
    val route: String = "",
    val departureAt: Long,
    val seatsTotal: Int,
    val vehicle: String = "",
    val notes: String = "",
    val driverRole: String = "",
    val driverCourse: String = "",
)
