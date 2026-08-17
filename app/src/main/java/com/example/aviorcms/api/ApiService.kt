package com.example.aviorcms.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class LoginRequest(
    val username: String,
    val password: String,
    @com.google.gson.annotations.SerializedName("device_label") val deviceLabel: String? = null
)

interface ApiService {

    @POST("auth.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("meta.php")
    suspend fun getMeta(): Response<MetaResponse>

    @GET("clients.php")
    suspend fun getClients(@Query("q") query: String? = null): Response<ClientsResponse>

    @POST("clients.php")
    suspend fun createClient(@Body client: InlineNewClient): Response<SimpleOkResponse>

    @GET("orders.php")
    suspend fun getOrders(@Query("status") status: String? = null): Response<OrdersResponse>

    @GET("orders.php")
    suspend fun getOrder(@Query("id") id: Int): Response<OrdersResponse>

    @POST("orders.php")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<SimpleOkResponse>

    @POST("update_status.php")
    suspend fun updateStatus(@Body request: UpdateStatusRequestWithId): Response<SimpleOkResponse>
}

// update_status.php ожидает id заказа в теле — отдельный класс, чтобы
// не засорять UpdateStatusRequest полем id (он не нужен нигде больше).
data class UpdateStatusRequestWithId(
    val id: Int,
    val status: String,
    val comment: String?
)
