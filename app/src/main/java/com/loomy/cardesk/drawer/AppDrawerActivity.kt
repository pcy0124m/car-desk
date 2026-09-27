package com.loomy.cardesk.drawer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loomy.cardesk.R
import com.loomy.cardesk.util.AppInfo
import com.loomy.cardesk.util.AppUtil

/**
 * 应用抽屉：4 列网格，点应用启动并关闭抽屉。
 * 从左侧栏底部九宫格进入。
 */
class AppDrawerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)

        findViewById<ImageView>(R.id.btnDrawerClose).setOnClickListener { finish() }

        val apps = AppUtil.installedApps(this, excludeSelf = true)
        val grid = findViewById<RecyclerView>(R.id.appGrid)
        grid.layoutManager = GridLayoutManager(this, 4)
        grid.adapter = AppGridAdapter(apps) { app ->
            AppUtil.launchApp(this, app.pkg)
            finish()
        }
    }

    private class AppGridAdapter(
        private val apps: List<AppInfo>,
        private val onClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<GridVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridVH {
            val item = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_grid_app, parent, false)
            return GridVH(item)
        }

        override fun getItemCount(): Int = apps.size

        override fun onBindViewHolder(holder: GridVH, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.icon)
            holder.name.text = app.name
            holder.itemView.setOnClickListener { onClick(app) }
        }
    }

    private class GridVH(item: View) : RecyclerView.ViewHolder(item) {
        val icon: ImageView = item.findViewById(R.id.gridIcon)
        val name: TextView = item.findViewById(R.id.gridName)
    }
}
