package com.example.aviorcms.api

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------
// Все имена полей — ровно как в JSON от cms.avior.moscow/api/mobile/
// (см. docs/MOBILE_API.md в репозитории cms_avior). Не переименовывать
// поля класса без @SerializedName — иначе Gson тихо подставит null,
// как это уже было с report_url.
// ---------------------------------------------------------------------

data class AuthResponse(
    val ok: Boolean,
    val token: String?,
    val user: User?,
    val error: String?
)

data class User(
    val id: Int,
    val username: String,
    @SerializedName("full_name") val fullName: String,
    val role: String // "owner" | "admin" | "engineer"
)

data class MetaResponse(
    val ok: Boolean,
    val statuses: List<String>?,
    @SerializedName("client_sources") val clientSources: Map<String, String>?,
    @SerializedName("device_types") val deviceTypes: List<String>?,
    @SerializedName("device_models") val deviceModels: List<String>?
)

data class Client(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val email: String?,
    val address: String?,
    val source: String?
)

data class ClientsResponse(
    val ok: Boolean,
    val clients: List<Client>?
)

data class InlineNewClient(
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val source: String?
)

data class StatusLogEntry(
    val status: String,
    @SerializedName("changed_at") val changedAt: String,
    val comment: String?
)

data class RepairPart(
    val name: String,
    val qty: String,
    val price: String,
    val category: String? // "part" | "service"
)

data class Order(
    val id: Int,
    @SerializedName("order_no") val orderNo: String,
    @SerializedName("order_type") val orderType: String?,
    val status: String,
    @SerializedName("client_id") val clientId: Int?,
    @SerializedName("client_name") val clientName: String,
    @SerializedName("client_phone") val clientPhone: String,
    @SerializedName("client_email") val clientEmail: String?,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("problem_description") val problemDescription: String?,
    @SerializedName("price_estimate") val priceEstimate: Double?,
    @SerializedName("receipt_url") val receiptUrl: String?,
    @SerializedName("report_url") val reportUrl: String?,
    @SerializedName("status_log") val statusLog: List<StatusLogEntry>?,
    val parts: List<RepairPart>?
)

data class OrdersResponse(
    val ok: Boolean,
    val orders: List<Order>?,
    val order: Order? // ответ при запросе одного заказа по id — сервер может отдавать в этом поле
)

data class CreateOrderRequest(
    @SerializedName("client_id") val clientId: Int?,
    @SerializedName("new_client") val newClient: InlineNewClient?,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("device_model") val deviceModel: String?,
    @SerializedName("problem_description") val problemDescription: String,
    @SerializedName("price_estimate") val priceEstimate: Double?
)

data class UpdateStatusRequest(
    val status: String,
    val comment: String?
)

data class SimpleOkResponse(
    val ok: Boolean,
    val error: String?
)
