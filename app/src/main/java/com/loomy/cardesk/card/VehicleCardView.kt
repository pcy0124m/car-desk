package com.loomy.cardesk.card

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import com.loomy.cardesk.R

/**
 * 车辆卡：俯视图 + 四角数据位。
 * MVP 阶段显示占位 "--"；接入 OBD/车辆总线后调用 [setTire] 等接口喂数据即可，
 * 不需要改 UI。
 */
class VehicleCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val fl: TextView by lazy { findViewById(R.id.vehFrontLeft) }
    private val fr: TextView by lazy { findViewById(R.id.vehFrontRight) }
    private val rl: TextView by lazy { findViewById(R.id.vehRearLeft) }
    private val rr: TextView by lazy { findViewById(R.id.vehRearRight) }

    init {
        inflate(context, R.layout.view_card_vehicle, this)
    }

    /** 预留数据接口：传入胎压/温度等字符串，null 显示占位 */
    fun setVehicleData(
        frontLeft: String? = null,
        frontRight: String? = null,
        rearLeft: String? = null,
        rearRight: String? = null
    ) {
        fl.text = frontLeft ?: "--"
        fr.text = frontRight ?: "--"
        rl.text = rearLeft ?: "--"
        rr.text = rearRight ?: "--"
    }
}
