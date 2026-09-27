package com.loomy.cardesk.apppicker

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loomy.cardesk.R
import com.loomy.cardesk.util.AppInfo
import com.loomy.cardesk.util.AppUtil

/**
 * 已安装应用选择器：核心交互入口。
 * 侧栏槽位、音乐卡、导航卡、顶部按钮绑定，全部走这里挑应用。
 */
class AppPickerDialog(
    private val context: Context,
    private val title: String,
    private val onPick: (AppInfo) -> Unit
) {

    private val dialog = Dialog(context)

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_app_picker, null)
        dialog.setContentView(view)
        dialog.setCancelable(true)

        view.findViewById<TextView>(R.id.pickerTitle).text = title

        val apps = AppUtil.installedApps(context)
        val rv = view.findViewById<RecyclerView>(R.id.pickerList)
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = object : RecyclerView.Adapter<PickerVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerVH {
                val item = LayoutInflater.from(context)
                    .inflate(R.layout.item_app, parent, false)
                return PickerVH(item)
            }

            override fun getItemCount(): Int = apps.size

            override fun onBindViewHolder(holder: PickerVH, position: Int) {
                val app = apps[position]
                holder.icon.setImageDrawable(app.icon)
                holder.name.text = app.name
                holder.pkg.text = app.pkg
                holder.itemView.setOnClickListener {
                    onPick(app)
                    dialog.dismiss()
                }
            }
        }

        view.findViewById<TextView>(R.id.pickerCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private class PickerVH(item: View) : RecyclerView.ViewHolder(item) {
        val icon: ImageView = item.findViewById(R.id.itemIcon)
        val name: TextView = item.findViewById(R.id.itemName)
        val pkg: TextView = item.findViewById(R.id.itemPkg)
    }
}
