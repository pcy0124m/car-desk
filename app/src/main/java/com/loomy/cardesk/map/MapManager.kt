package com.loomy.cardesk.map

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng

/**
 * 高德地图管理器：负责 MapView 生命周期、缩放控制、定位跟随。
 *
 * 注意：如果 gradle.properties 里的 AMAP_KEY 未填写，[attach] 不会创建地图，
 * 主界面会显示「高德 Key 未配置」提示卡——这比白屏好排查得多。
 */
class MapManager(private val context: Context) {

    private var mapView: MapView? = null
    private var aMap: AMap? = null
    private var locationClient: AMapLocationClient? = null

    /** 高德 Key 是否已配置（不是占位符） */
    fun isKeyConfigured(): Boolean {
        val meta = try {
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
        } catch (e: Exception) {
            null
        }
        val key = meta?.getString("com.amap.api.v2.apikey").orEmpty()
        return key.isNotBlank() && !key.contains("REPLACE_WITH")
    }

    /** 创建 MapView 挂进容器（Key 有效时）；任何异常都不允许拖垮桌面 */
    fun attach(container: FrameLayout) {
        if (!isKeyConfigured()) return
        try {
            mapView = MapView(context)
            container.addView(
                mapView,
                0,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            aMap = mapView?.map
            aMap?.uiSettings?.isZoomControlsEnabled = false // 用自己的悬浮缩放按钮
            aMap?.uiSettings?.isCompassEnabled = true
            aMap?.isMyLocationEnabled = true               // 显示蓝点
        } catch (e: Throwable) {
            // 地图初始化失败（Key 无效 / so 缺失 / ROM 兼容等）：
            // 移除地图视图，保证桌面其余功能照常
            try {
                container.removeView(mapView)
            } catch (ignore: Throwable) {
            }
            mapView = null
            aMap = null
        }
    }

    fun zoomIn() {
        try {
            aMap?.animateCamera(CameraUpdateFactory.zoomIn())
        } catch (e: Throwable) {
        }
    }

    fun zoomOut() {
        try {
            aMap?.animateCamera(CameraUpdateFactory.zoomOut())
        } catch (e: Throwable) {
        }
    }

    /**
     * 启动高德定位：首次定位成功后把镜头拉到当前位置并回调。
     * 车机一般有 GPS/AGPS，权限授予后基本秒定。
     */
    fun startLocation(onFirstFix: (lat: Double, lng: Double) -> Unit) {
        if (!isKeyConfigured()) return
        try {
            locationClient = AMapLocationClient(context)
            locationClient?.setLocationListener { loc ->
                try {
                    if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                        aMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude), 16f
                            )
                        )
                        onFirstFix(loc.latitude, loc.longitude)
                        // 定位一次后停掉省电；需要持续跟车可去掉这一行并改 option.interval
                        locationClient?.stopLocation()
                    }
                } catch (e: Throwable) {
                    // 定位回调异常不影响主界面
                }
            }
            val option = AMapLocationClientOption()
            option.isOnceLocation = true
            locationClient?.setLocationOption(option)
            locationClient?.startLocation()
        } catch (e: Exception) {
            // 定位组件异常（如缺少权限），不崩溃，地图仍可用
        }
    }

    // ---------- 生命周期委托（MainActivity 必须逐一对齐调用） ----------
    // 高德 MapView 在异常 ROM 上生命周期回调也可能抛错，全部兜住
    fun onResume() {
        try {
            mapView?.onResume()
        } catch (e: Throwable) {
        }
    }

    fun onPause() {
        try {
            mapView?.onPause()
        } catch (e: Throwable) {
        }
    }

    fun onDestroy() {
        try {
            locationClient?.stopLocation()
            locationClient?.onDestroy()
        } catch (e: Throwable) {
        }
        locationClient = null
        try {
            mapView?.onDestroy()
        } catch (e: Throwable) {
        }
        mapView = null
    }

    fun onSaveInstanceState(outState: Bundle) {
        try {
            mapView?.onSaveInstanceState(outState)
        } catch (e: Throwable) {
        }
    }
}
