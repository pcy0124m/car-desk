package com.loomy.cardesk.card

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Toast
import com.loomy.cardesk.R
import com.loomy.cardesk.util.AppUtil

/**
 * 快捷导航卡：导航 / 回家 / 去公司。
 * - 「导航」可绑定一个地图应用（从已装应用挑选），点击直接启动
 * - 「回家 / 去公司」长按可设置地址，点击用 geo: URI 唤起地图导航
 */
class NavCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_card_nav, this)
    }

    /**
     * @param mapAppPkg   绑定的地图应用包名（可空）
     * @param homeAddr    家的地址（可空）
     * @param companyAddr 公司地址（可空）
     * @param onPickMap   未绑定地图应用时点击「导航」触发
     * @param onSetHome   长按「回家」触发（弹输入框）
     * @param onSetCompany 长按「去公司」触发
     */
    fun bind(
        mapAppPkg: String?,
        homeAddr: String?,
        companyAddr: String?,
        onPickMap: () -> Unit,
        onSetHome: () -> Unit,
        onSetCompany: () -> Unit
    ) {
        val btnNav = findViewById<LinearLayout>(R.id.btnNav)
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnCompany = findViewById<LinearLayout>(R.id.btnCompany)

        // 导航：未绑定先挑应用，已绑定直接启动
        btnNav.setOnClickListener {
            if (mapAppPkg.isNullOrBlank()) onPickMap()
            else AppUtil.launchApp(context, mapAppPkg)
        }
        btnNav.setOnLongClickListener { onPickMap(); true }

        // 回家：有地址就导航，没有就引导设置（长按设置）
        btnHome.setOnClickListener {
            if (homeAddr.isNullOrBlank()) {
                Toast.makeText(context, "长按「回家」可设置家的地址", Toast.LENGTH_SHORT).show()
            } else {
                AppUtil.navigateTo(context, homeAddr)
            }
        }
        btnHome.setOnLongClickListener { onSetHome(); true }

        // 去公司：同上
        btnCompany.setOnClickListener {
            if (companyAddr.isNullOrBlank()) {
                Toast.makeText(context, "长按「去公司」可设置公司地址", Toast.LENGTH_SHORT).show()
            } else {
                AppUtil.navigateTo(context, companyAddr)
            }
        }
        btnCompany.setOnLongClickListener { onSetCompany(); true }
    }
}
