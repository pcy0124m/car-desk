package com.loomy.cardesk

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.loomy.cardesk.apppicker.AppPickerDialog
import com.loomy.cardesk.card.LauncherCardView
import com.loomy.cardesk.card.MusicCardView
import com.loomy.cardesk.card.NavCardView
import com.loomy.cardesk.card.VehicleCardView
import com.loomy.cardesk.config.DeskConfig
import com.loomy.cardesk.drawer.AppDrawerActivity
import com.loomy.cardesk.map.MapManager
import com.loomy.cardesk.util.AppInfo
import com.loomy.cardesk.util.AppUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 车机桌面主界面：左侧快捷栏 + 中间卡片列 + 右侧高德地图区。
 * 所有「选择应用」的交互都复用 AppPickerDialog，配置落在 DeskConfig。
 */
class MainActivity : AppCompatActivity() {

    // ---------- 视图 ----------
    private lateinit var navCard: NavCardView
    private lateinit var musicCard: MusicCardView
    private lateinit var vehicleCard: VehicleCardView
    private lateinit var cardColumn: LinearLayout
    private lateinit var btnAddCard: LinearLayout
    private lateinit var topTimeLeft: TextView
    private lateinit var topTimeRight: TextView
    private lateinit var topBattery: TextView
    private lateinit var topWifi: ImageView

    private val mapManager = MapManager(this)
    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val RC_LOCATION = 1001

    // ---------- 生命周期 ----------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initDock()
        initCards()
        initTopBar()
        initSearchPanel()
        initMap()
        initExtraCards()
        initWeather()

        requestLocationPermission()
        registerBatteryReceiver()
        updateClock()
    }

    override fun onResume() {
        super.onResume()
        mapManager.onResume()
        // 定时器统一在 onResume 启动、onPause 清空，避免重复注册
        handler.post(clockRunnable)
        handler.post(mediaRunnable)
        handler.post(progressRunnable)
    }

    override fun onPause() {
        super.onPause()
        mapManager.onPause()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapManager.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            mapManager.startLocation { _, _ -> /* 定位成功，地图已跟随，可扩展天气/周边 */ }
        }
    }

    // ---------- 初始化 ----------

    private fun initViews() {
        navCard = findViewById(R.id.navCard)
        musicCard = findViewById(R.id.musicCard)
        vehicleCard = findViewById(R.id.vehicleCard)
        cardColumn = findViewById(R.id.cardColumn)
        btnAddCard = findViewById(R.id.btnAddCard)
        topTimeLeft = findViewById(R.id.topTimeLeft)
        topTimeRight = findViewById(R.id.topTimeRight)
        topBattery = findViewById(R.id.topBattery)
        topWifi = findViewById(R.id.topWifi)
    }

    /** 左侧快捷栏：5 个可自定义槽位 + 底部应用抽屉入口 */
    private fun initDock() {
        val container = findViewById<LinearLayout>(R.id.dockSlots)
        for (i in 0 until DeskConfig.DOCK_SLOT_COUNT) {
            val slot = LayoutInflater.from(this)
                .inflate(R.layout.view_slot_item, container, false)
            container.addView(slot)
            renderSlot(slot, i)
        }
        findViewById<ImageView>(R.id.btnDrawer).setOnClickListener {
            startActivity(Intent(this, AppDrawerActivity::class.java))
        }
    }

    /** 渲染单个侧栏槽位：未绑定→虚线"+"，已绑定→应用图标 */
    private fun renderSlot(slotView: View, index: Int) {
        val icon = slotView.findViewById<ImageView>(R.id.slotIcon)
        val pkg = DeskConfig.dockSlot(this, index)

        if (pkg == null || !AppUtil.isInstalled(this, pkg)) {
            icon.setImageResource(R.drawable.ic_add)
            slotView.setOnClickListener {
                pickApp("选择快捷应用") { app ->
                    DeskConfig.setDockSlot(this, index, app.pkg)
                    renderSlot(slotView, index)
                }
            }
            slotView.setOnLongClickListener(null)
        } else {
            icon.setImageDrawable(AppUtil.appIcon(this, pkg))
            slotView.setOnClickListener { AppUtil.launchApp(this, pkg) }
            slotView.setOnLongClickListener {
                showSlotMenu(slotView, index, pkg)
                true
            }
        }
    }

    private fun showSlotMenu(slotView: View, index: Int, pkg: String) {
        val options = arrayOf("更换应用", "解除绑定")
        AlertDialog.Builder(this)
            .setTitle(AppUtil.appName(this, pkg) ?: pkg)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickApp("选择快捷应用") { app ->
                        DeskConfig.setDockSlot(this, index, app.pkg)
                        renderSlot(slotView, index)
                    }
                    1 -> {
                        DeskConfig.setDockSlot(this, index, null)
                        renderSlot(slotView, index)
                    }
                }
            }
            .show()
    }

    /** 三张固定卡片：导航 / 音乐 / 车辆 */
    private fun initCards() {
        navCard.bind(
            mapAppPkg = DeskConfig.getStr(this, DeskConfig.KEY_MAP_APP),
            homeAddr = DeskConfig.getStr(this, DeskConfig.KEY_HOME_ADDR),
            companyAddr = DeskConfig.getStr(this, DeskConfig.KEY_COMPANY_ADDR),
            onPickMap = {
                pickApp("选择地图应用") { app ->
                    DeskConfig.setStr(this, DeskConfig.KEY_MAP_APP, app.pkg)
                    refreshNavCard()
                }
            },
            onSetHome = { inputAddress("设置家的地址", DeskConfig.KEY_HOME_ADDR) },
            onSetCompany = { inputAddress("设置公司地址", DeskConfig.KEY_COMPANY_ADDR) }
        )

        // 音乐卡：绑定任意音乐 App（网易云/QQ音乐/酷狗…）
        musicCard.bindMusicApp(DeskConfig.getStr(this, DeskConfig.KEY_MUSIC)) {
            pickApp("选择音乐应用") { app ->
                DeskConfig.setStr(this, DeskConfig.KEY_MUSIC, app.pkg)
                musicCard.bindMusicApp(app.pkg) {
                    pickApp("选择音乐应用") { newApp ->
                        DeskConfig.setStr(this, DeskConfig.KEY_MUSIC, newApp.pkg)
                        musicCard.bindMusicApp(newApp.pkg) {}
                    }
                }
                musicCard.refresh()
            }
        }
        musicCard.wireControls()

        vehicleCard.setVehicleData() // 占位 "--"，接入 OBD 后喂数据
    }

    private fun refreshNavCard() {
        navCard.bind(
            mapAppPkg = DeskConfig.getStr(this, DeskConfig.KEY_MAP_APP),
            homeAddr = DeskConfig.getStr(this, DeskConfig.KEY_HOME_ADDR),
            companyAddr = DeskConfig.getStr(this, DeskConfig.KEY_COMPANY_ADDR),
            onPickMap = {
                pickApp("选择地图应用") { app ->
                    DeskConfig.setStr(this, DeskConfig.KEY_MAP_APP, app.pkg)
                    refreshNavCard()
                }
            },
            onSetHome = { inputAddress("设置家的地址", DeskConfig.KEY_HOME_ADDR) },
            onSetCompany = { inputAddress("设置公司地址", DeskConfig.KEY_COMPANY_ADDR) }
        )
    }

    /** 顶部工具栏：时钟 / WiFi / 麦克风 / 电话 / 电量 / 主页 / 退出 */
    private fun initTopBar() {
        findViewById<ImageView>(R.id.btnExit).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnHome).setOnClickListener { toggleSearchPanel() }

        // 麦克风：绑定语音助手应用（如小度/系统语音）
        bindTopAction(R.id.topMic, DeskConfig.KEY_MIC, "选择语音/助手应用")
        // 电话：绑定拨号/通讯应用
        bindTopAction(R.id.topPhone, DeskConfig.KEY_PHONE, "选择拨号/通讯应用")
    }

    /** 顶部按钮统一绑定逻辑：已绑定→启动；未绑定/长按→挑选 */
    private fun bindTopAction(btnId: Int, key: String, title: String) {
        val btn = findViewById<ImageView>(btnId)
        val refresh = {
            val pkg = DeskConfig.getStr(this, key)
            if (pkg != null && AppUtil.isInstalled(this, pkg)) {
                btn.setImageDrawable(AppUtil.appIcon(this, pkg))
            } else {
                btn.setImageResource(when (btnId) {
                    R.id.topMic -> R.drawable.ic_mic
                    else -> R.drawable.ic_phone
                })
            }
        }
        refresh()
        btn.setOnClickListener {
            val pkg = DeskConfig.getStr(this, key)
            if (pkg != null && AppUtil.isInstalled(this, pkg)) AppUtil.launchApp(this, pkg)
            else pickApp(title) { app ->
                DeskConfig.setStr(this, key, app.pkg)
                refresh()
            }
        }
        btn.setOnLongClickListener {
            pickApp(title) { app ->
                DeskConfig.setStr(this, key, app.pkg)
                refresh()
            }
            true
        }
    }

    /** 地图搜索悬浮面板：设置家/公司地址 */
    private fun initSearchPanel() {
        findViewById<TextView>(R.id.btnSetHome).setOnClickListener {
            inputAddress("设置家的地址", DeskConfig.KEY_HOME_ADDR)
        }
        findViewById<TextView>(R.id.btnSetCompany).setOnClickListener {
            inputAddress("设置公司地址", DeskConfig.KEY_COMPANY_ADDR)
        }
    }

    private fun inputAddress(title: String, key: String) {
        val et = EditText(this).apply {
            // 注意：apply 块内 this 指向 EditText，取配置必须显式用 this@MainActivity
            setText(DeskConfig.getStr(this@MainActivity, key) ?: "")
            hint = "输入地址或地名，如：朝阳区望京SOHO"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(et)
            .setPositiveButton("保存") { _, _ ->
                DeskConfig.setStr(this, key, et.text.toString().trim())
                refreshNavCard()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 右侧高德地图区 */
    private fun initMap() {
        val container = findViewById<FrameLayout>(R.id.mapContainer)
        findViewById<View>(R.id.keyMissingHint).visibility =
            if (mapManager.isKeyConfigured()) View.GONE else View.VISIBLE
        mapManager.attach(container)

        findViewById<ImageView>(R.id.btnZoomIn).setOnClickListener { mapManager.zoomIn() }
        findViewById<ImageView>(R.id.btnZoomOut).setOnClickListener { mapManager.zoomOut() }
    }

    /** 「添加卡片」：从已装应用挑选，动态生成启动卡 */
    private fun initExtraCards() {
        for (pkg in DeskConfig.extraCards(this)) {
            if (AppUtil.isInstalled(this, pkg)) addLauncherCard(pkg)
        }
        btnAddCard.setOnClickListener {
            pickApp("选择要添加的应用") { app ->
                val list = DeskConfig.extraCards(this).toMutableList()
                if (app.pkg !in list) {
                    list.add(app.pkg)
                    DeskConfig.setExtraCards(this, list)
                    addLauncherCard(app.pkg)
                }
            }
        }
    }

    private fun addLauncherCard(pkg: String) {
        val card = LauncherCardView(this)
        card.setup(pkg) {
            val options = arrayOf("更换应用", "删除卡片")
            AlertDialog.Builder(this)
                .setTitle(AppUtil.appName(this, pkg) ?: pkg)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> pickApp("选择应用") { app ->
                            val list = DeskConfig.extraCards(this).toMutableList()
                            val idx = list.indexOf(pkg)
                            if (idx >= 0) list[idx] = app.pkg else list.add(app.pkg)
                            DeskConfig.setExtraCards(this, list)
                            cardColumn.removeView(card)
                            addLauncherCard(app.pkg)
                        }
                        1 -> {
                            val list = DeskConfig.extraCards(this).toMutableList()
                            list.remove(pkg)
                            DeskConfig.setExtraCards(this, list)
                            cardColumn.removeView(card)
                        }
                    }
                }
                .show()
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10) }
        cardColumn.addView(card, lp)
        // 新卡片始终排在「添加卡片」按钮之前
        cardColumn.removeView(btnAddCard)
        cardColumn.addView(btnAddCard)
    }

    /** 天气卡：绑定天气应用后点击唤起 */
    private fun initWeather() {
        val weatherCard = findViewById<LinearLayout>(R.id.weatherCard)
        val temp = findViewById<TextView>(R.id.weatherTemp)
        val refresh = {
            val pkg = DeskConfig.getStr(this, DeskConfig.KEY_WEATHER)
            temp.text = AppUtil.appName(this, pkg) ?: "天气"
        }
        refresh()
        weatherCard.setOnClickListener {
            val pkg = DeskConfig.getStr(this, DeskConfig.KEY_WEATHER)
            if (pkg != null && AppUtil.isInstalled(this, pkg)) AppUtil.launchApp(this, pkg)
            else pickApp("选择天气应用") { app ->
                DeskConfig.setStr(this, DeskConfig.KEY_WEATHER, app.pkg)
                refresh()
            }
        }
        weatherCard.setOnLongClickListener {
            pickApp("选择天气应用") { app ->
                DeskConfig.setStr(this, DeskConfig.KEY_WEATHER, app.pkg)
                refresh()
            }
            true
        }
    }

    // ---------- 小工具 ----------

    private fun pickApp(title: String, onPick: (AppInfo) -> Unit) {
        AppPickerDialog(this, title, onPick).show()
    }

    private fun toggleSearchPanel() {
        val panel = findViewById<View>(R.id.searchPanel)
        panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val granted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    RC_LOCATION
                )
            } else {
                mapManager.startLocation { _, _ -> }
            }
        } else {
            mapManager.startLocation { _, _ -> }
        }
    }

    private fun registerBatteryReceiver() {
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                topBattery.text = "${level * 100 / scale}%"
            }
        }
    }

    private fun updateClock() {
        val now = clockFormat.format(Date())
        topTimeLeft.text = now
        topTimeRight.text = now
        // WiFi 状态（车机一般插网或走蜂窝，这里只反映 WiFi 开关）
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        topWifi.visibility = if (wifi.isWifiEnabled) View.VISIBLE else View.GONE
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 30_000)
        }
    }

    /** 每 2s 轮询一次系统媒体会话：把「绑定音乐应用」的播放状态喂给音乐卡 */
    private fun refreshMediaSession() {
        val pkg = DeskConfig.getStr(this, DeskConfig.KEY_MUSIC) ?: return
        if (!AppUtil.isInstalled(this, pkg)) return
        val sm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val controller: MediaController? = try {
            sm.getActiveSessions(null).firstOrNull { it.packageName == pkg }
        } catch (e: Exception) {
            null // 部分 ROM 拿不到媒体会话列表，不影响其他功能
        }
        musicCard.attachController(controller)
    }

    private val mediaRunnable = object : Runnable {
        override fun run() {
            refreshMediaSession()
            handler.postDelayed(this, 2000)
        }
    }

    /** 每 500ms 刷新音乐卡进度条 */
    private val progressRunnable = object : Runnable {
        override fun run() {
            musicCard.refreshProgress()
            handler.postDelayed(this, 500)
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
