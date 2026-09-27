package com.loomy.cardesk.card

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.loomy.cardesk.R
import com.loomy.cardesk.util.AppUtil

/**
 * 通用启动卡：「添加卡片」后动态创建。
 * 显示已绑定应用图标 + 名称，点击启动，长按由外部弹菜单（更换/删除）。
 */
class LauncherCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val appIcon = ImageView(context)
    private val appName = TextView(context)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.bg_card)

        val pad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
            .toInt()
        setPadding(pad, pad, pad, pad)

        appIcon.layoutParams = LayoutParams(
            dp(48), dp(48)
        ).apply {
            setMargins(0, 0, 0, dp(6))
        }
        addView(appIcon)

        appName.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        )
        appName.setTextColor(resources.getColor(R.color.text_primary, null))
        appName.textSize = 14f
        addView(appName)
    }

    /** 绑定应用并设置长按回调 */
    fun setup(pkg: String, onLongPress: () -> Unit) {
        appIcon.setImageDrawable(AppUtil.appIcon(context, pkg))
        appName.text = AppUtil.appName(context, pkg) ?: pkg
        setOnClickListener { AppUtil.launchApp(context, pkg) }
        setOnLongClickListener { onLongPress(); true }
    }

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()
}
