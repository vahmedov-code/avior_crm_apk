package com.example.aviorcms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aviorcms.api.ApiClient
import com.example.aviorcms.api.Client
import com.example.aviorcms.api.CreateOrderRequest
import com.example.aviorcms.api.InlineNewClient
import com.example.aviorcms.api.LoginRequest
import com.example.aviorcms.api.MetaResponse
import com.example.aviorcms.api.Order
import com.example.aviorcms.api.TokenStore
import com.example.aviorcms.api.UpdateStatusRequestWithId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CmsViewModel(application: Application) : AndroidViewModel(application) {

    private val api get() = ApiClient.service(getApplication())

    private val _isLoggedIn = MutableStateFlow(TokenStore.getToken(getApplication()) != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    val clients: StateFlow<List<Client>> = _clients.asStateFlow()

    private val _meta = MutableStateFlow<MetaResponse?>(null)
    val meta: StateFlow<MetaResponse?> = _meta.asStateFlow()

    private var currentStatusFilter: String? = null

    fun login(username: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.login(LoginRequest(username, password, "Android"))
                val body = response.body()
                if (response.isSuccessful && body?.ok == true && body.token != null && body.user != null) {
                    TokenStore.saveSession(getApplication(), body.token, body.user.role, body.user.fullName)
                    _isLoggedIn.value = true
                    onResult(true, null)
                    refreshData()
                } else {
                    onResult(false, body?.error ?: "Неверный логин или пароль")
                }
            } catch (e: Exception) {
                onResult(false, "Нет связи с сервером: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        TokenStore.clear(getApplication())
        _isLoggedIn.value = false
        _orders.value = emptyList()
        _clients.value = emptyList()
    }

    fun refreshData() {
        loadMeta()
        loadOrders(currentStatusFilter)
        loadClients(null)
    }

    fun loadMeta() {
        viewModelScope.launch {
            try {
                val response = api.getMeta()
                if (response.isSuccessful) _meta.value = response.body()
            } catch (_: Exception) { /* справочники не критичны — молча пропускаем */ }
        }
    }

    fun loadOrders(status: String?) {
        currentStatusFilter = status
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = api.getOrders(status)
                if (response.isSuccessful) {
                    _orders.value = response.body()?.orders ?: emptyList()
                } else {
                    _errorMessage.value = "Ошибка загрузки заказов (${response.code()})"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Нет связи с сервером"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadClients(query: String?) {
        viewModelScope.launch {
            try {
                val response = api.getClients(query)
                if (response.isSuccessful) {
                    _clients.value = response.body()?.clients ?: emptyList()
                }
            } catch (_: Exception) { }
        }
    }

    private val _foundClientByPhone = MutableStateFlow<Client?>(null)
    val foundClientByPhone: StateFlow<Client?> = _foundClientByPhone.asStateFlow()
    private var phoneSearchJob: Job? = null

    /**
     * Подсказка «Найден: ...» на экране нового заказа (§7 PROJECT_STATE.md) —
     * ищет клиента по телефону, только когда введено похожее на полный
     * номер количество цифр (не долбим сервер на каждый символ), с
     * debounce 500мс (не на каждое нажатие клавиши). Отменяет предыдущий
     * незавершённый поиск, если пользователь продолжает печатать.
     */
    fun searchClientByPhone(phone: String) {
        phoneSearchJob?.cancel()
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 10) {
            _foundClientByPhone.value = null
            return
        }
        phoneSearchJob = viewModelScope.launch {
            delay(500)
            try {
                val response = api.findClientByPhone(phone)
                _foundClientByPhone.value = if (response.isSuccessful) response.body()?.client else null
            } catch (_: Exception) {
                _foundClientByPhone.value = null
            }
        }
    }

    fun clearFoundClientByPhone() {
        phoneSearchJob?.cancel()
        _foundClientByPhone.value = null
    }

    fun updateOrderStatus(orderId: Int, status: String, comment: String? = null) {
        viewModelScope.launch {
            try {
                val response = api.updateStatus(UpdateStatusRequestWithId(orderId, status, comment))
                if (response.isSuccessful && response.body()?.ok == true) {
                    loadOrders(currentStatusFilter) // обновляем список, чтобы статус подтянулся сразу
                } else {
                    _errorMessage.value = "Не удалось сменить статус"
                }
            } catch (_: Exception) {
                _errorMessage.value = "Нет связи с сервером"
            }
        }
    }

    fun createOrder(
        clientId: Int?,
        newClient: InlineNewClient?,
        deviceType: String,
        deviceModel: String?,
        description: String,
        priceEstimate: Double?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.createOrder(
                    CreateOrderRequest(clientId, newClient, deviceType, deviceModel, description, priceEstimate)
                )
                val ok = response.isSuccessful && response.body()?.ok == true
                if (ok) refreshData()
                onResult(ok)
            } catch (e: Exception) {
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
