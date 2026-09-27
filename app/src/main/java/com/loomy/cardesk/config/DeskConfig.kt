package com.loomy.cardesk.config

import android.content.Context
import android.content.SharedPreferences

/**
 * 桌面配置持久化：所有绑定关系、地址、槽位都存在 SharedPreferences。
 * 键值都走字符串，简单可靠，车机场景不需要数据库。
 */
object DeskConfig {

    private const val PREFS = "cardesk_config"

    // 常量键
    const val KEY_MUSIC = "music_pkg"           // 音乐卡绑定的音乐应用
    const val KEY_MAP_APP = "map_app_pkg"       // 导航按钮唤起的地图应用
    const val KEY_HOME_ADDR = "home_addr"       // 家的地址/地名
    const val KEY_COMPANY_ADDR = "company_addr" // 公司地址/地名
    const val KEY_MIC = "mic_pkg"               // 顶部麦克风按钮绑定应用
    const val KEY_PHONE = "phone_pkg"           // 顶部电话按钮绑定应用
    const val KEY_WEATHER = "weather_pkg"       // 天气卡绑定应用
    const val KEY_MAP_ENABLED = "map_enabled"   // 地图模块开关（诊断期默认关）

    private const val KEY_DOCK = "dock_"        // dock_0 ~ dock_4 左侧快捷栏槽位
    private const val KEY_EXTRA_CARDS = "extra_cards" // 追加启动卡（逗号分隔包名）

    const val DOCK_SLOT_COUNT = 5

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- 侧栏槽位 ----------
    fun dockSlot(context: Context, index: Int): String? =
        prefs(context).getString("$KEY_DOCK$index", null)

    fun setDockSlot(context: Context, index: Int, pkg: String?) {
        prefs(context).edit().putString("$KEY_DOCK$index", pkg).apply()
    }

    // ---------- 追加启动卡 ----------
    fun extraCards(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_EXTRA_CARDS, "") ?: ""
        return raw.split(",").filter { it.isNotBlank() }
    }

    fun setExtraCards(context: Context, pkgs: List<String>) {
        prefs(context).edit().putString(KEY_EXTRA_CARDS, pkgs.joinToString(",")).apply()
    }

    // ---------- 通用字符串读写 ----------
    fun getStr(context: Context, key: String): String? =
        prefs(context).getString(key, null)

    fun setStr(context: Context, key: String, value: String?) {
        prefs(context).edit().putString(key, value).apply()
    }

    // ---------- 地图开关（诊断阶段默认关闭，确认稳定后再默认开启） ----------
    fun mapEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MAP_ENABLED, false)

    fun setMapEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MAP_ENABLED, enabled).apply()
    }
}
