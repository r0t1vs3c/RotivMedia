package com.rotivsecurity.launcher

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Inicializa os ajustes antes de qualquer tela ou serviço. */
class RotivApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        CarPhoto.load(this)
        RemoteAdb.watch(this)
        com.rotivsecurity.launcher.ui.WelcomeState.watchScreen(this)
    }
}

/** Ajustes do usuário, salvos no aparelho. */
object Prefs {
    private var sp: SharedPreferences? = null

    private val _name = MutableStateFlow("")
    private val _welcome = MutableStateFlow(true)
    private val _pauseAbove = MutableStateFlow(5)
    private val _speedAlert = MutableStateFlow(0)
    private val _freeform = MutableStateFlow(false)

    private val _carModel = MutableStateFlow("")
    private val _carBody = MutableStateFlow(1)
    private val _carColor = MutableStateFlow(0xFF3B82F6L)
    private val _carPhoto = MutableStateFlow(false)
    private val _fuelType = MutableStateFlow(0)
    private val _fuelPrice = MutableStateFlow(0f)
    private val _engineCc = MutableStateFlow(1600)
    private val _obdAddress = MutableStateFlow("")
    private val _obdName = MutableStateFlow("")
    private val _obdDemo = MutableStateFlow(false)
    private val _remoteAdb = MutableStateFlow(true)
    private val _mapDock = MutableStateFlow(true)
    private val _musicDock = MutableStateFlow(true)

    /** Nome usado na tela de boas-vindas. */
    val name: StateFlow<String> = _name
    val welcome: StateFlow<Boolean> = _welcome
    /** Velocidade (km/h) acima da qual o YouTube pausa. */
    val pauseAbove: StateFlow<Int> = _pauseAbove
    /** Limite do aviso sonoro de velocidade (0 = desligado). */
    val speedAlert: StateFlow<Int> = _speedAlert
    /** Modo direção em janelas livres 40/60 em vez da tela dividida do sistema. */
    val freeform: StateFlow<Boolean> = _freeform

    /** Nome do carro mostrado na tela (ex.: "Civic G8"). */
    val carModel: StateFlow<String> = _carModel
    /** Carroceria do desenho: 0 hatch, 1 sedã, 2 SUV, 3 picape. */
    val carBody: StateFlow<Int> = _carBody
    /** Cor da lataria (ARGB). */
    val carColor: StateFlow<Long> = _carColor
    /** Usar a foto do próprio carro no lugar do desenho. */
    val carPhoto: StateFlow<Boolean> = _carPhoto
    /** 0 gasolina (E27), 1 etanol. */
    val fuelType: StateFlow<Int> = _fuelType
    /** Preço do litro em reais (0 = não informado). */
    val fuelPrice: StateFlow<Float> = _fuelPrice
    /** Cilindrada, usada para estimar consumo quando o carro não tem sensor MAF. */
    val engineCc: StateFlow<Int> = _engineCc
    /** Endereço Bluetooth do adaptador ELM327 pareado. */
    val obdAddress: StateFlow<String> = _obdAddress
    val obdName: StateFlow<String> = _obdName
    /** Dados simulados para ver a tela funcionando sem adaptador. */
    val obdDemo: StateFlow<Boolean> = _obdDemo
    /** Religar a depuração (ADB remoto pelo Tailscale) quando a central desligar. */
    val remoteAdb: StateFlow<Boolean> = _remoteAdb
    /** Google Maps fixo numa janela no painel esquerdo da tela inicial. */
    val mapDock: StateFlow<Boolean> = _mapDock
    /** YouTube Music numa janela no quadro do centro (mexer nele sem sair da tela inicial). */
    val musicDock: StateFlow<Boolean> = _musicDock

    fun init(ctx: Context) {
        if (sp != null) return
        val s = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
        sp = s
        _name.value = s.getString("nome", "").orEmpty()
        _welcome.value = s.getBoolean("boas_vindas", true)
        _pauseAbove.value = s.getInt("pausa_kmh", 5)
        _speedAlert.value = s.getInt("alerta_kmh", 0)
        _freeform.value = s.getBoolean("janelas_livres", false)
        _carModel.value = s.getString("carro_modelo", "").orEmpty()
        _carBody.value = s.getInt("carro_carroceria", 1)
        _carColor.value = s.getLong("carro_cor", 0xFF3B82F6L)
        _carPhoto.value = s.getBoolean("carro_foto", false)
        _fuelType.value = s.getInt("combustivel", 0)
        _fuelPrice.value = s.getFloat("preco_litro", 0f)
        _engineCc.value = s.getInt("motor_cc", 1600)
        _obdAddress.value = s.getString("obd_endereco", "").orEmpty()
        _obdName.value = s.getString("obd_nome", "").orEmpty()
        _obdDemo.value = s.getBoolean("obd_demo", false)
        _remoteAdb.value = s.getBoolean("adb_remoto", true)
        _mapDock.value = s.getBoolean("mapa_fixo", true)
        _musicDock.value = s.getBoolean("musica_fixa", true)
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sp?.edit()?.apply(block)?.apply()
    }

    fun setName(v: String) { _name.value = v; edit { putString("nome", v) } }
    fun setWelcome(v: Boolean) { _welcome.value = v; edit { putBoolean("boas_vindas", v) } }
    fun setPauseAbove(v: Int) { _pauseAbove.value = v; edit { putInt("pausa_kmh", v) } }
    fun setSpeedAlert(v: Int) { _speedAlert.value = v; edit { putInt("alerta_kmh", v) } }
    fun setFreeform(v: Boolean) { _freeform.value = v; edit { putBoolean("janelas_livres", v) } }

    fun setCarModel(v: String) { _carModel.value = v; edit { putString("carro_modelo", v) } }
    fun setCarBody(v: Int) { _carBody.value = v; edit { putInt("carro_carroceria", v) } }
    fun setCarColor(v: Long) { _carColor.value = v; edit { putLong("carro_cor", v) } }
    fun setCarPhoto(v: Boolean) { _carPhoto.value = v; edit { putBoolean("carro_foto", v) } }
    fun setFuelType(v: Int) { _fuelType.value = v; edit { putInt("combustivel", v) } }
    fun setFuelPrice(v: Float) { _fuelPrice.value = v; edit { putFloat("preco_litro", v) } }
    fun setEngineCc(v: Int) { _engineCc.value = v; edit { putInt("motor_cc", v) } }
    fun setObdDevice(address: String, name: String) {
        _obdAddress.value = address
        _obdName.value = name
        edit { putString("obd_endereco", address); putString("obd_nome", name) }
    }
    fun setObdDemo(v: Boolean) { _obdDemo.value = v; edit { putBoolean("obd_demo", v) } }
    fun setRemoteAdb(v: Boolean) { _remoteAdb.value = v; edit { putBoolean("adb_remoto", v) } }
    fun setMapDock(v: Boolean) { _mapDock.value = v; edit { putBoolean("mapa_fixo", v) } }
    fun setMusicDock(v: Boolean) { _musicDock.value = v; edit { putBoolean("musica_fixa", v) } }
}
