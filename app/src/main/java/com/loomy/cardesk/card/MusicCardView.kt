package com.loomy.cardesk.card

import android.content.Context
import android.media.session.MediaController
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.loomy.cardesk.R
import com.loomy.cardesk.util.AppUtil

/**
 * 音乐卡：核心亮点之一——「自己选择音乐应用」。
 * - 点击「换应用」从已装应用里挑音乐 App 绑定
 * - 点击播放键：绑定的 App 没在播 → 直接启动它；在播 → 暂停/继续
 * - 上一曲/下一曲通过 MediaSession 控制当前媒体会话
 * - 标题/进度条实时反映系统正在播放的内容
 */
class MusicCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val appIcon: ImageView by lazy { findViewById(R.id.musicAppIcon) }
    private val titleView: TextView by lazy { findViewById(R.id.musicTitle) }
    private val subtitleView: TextView by lazy { findViewById(R.id.musicSubtitle) }
    private val btnPrev: ImageView by lazy { findViewById(R.id.btnPrev) }
    private val btnPlayPause: ImageView by lazy { findViewById(R.id.btnPlayPause) }
    private val btnNext: ImageView by lazy { findViewById(R.id.btnNext) }
    private val btnChange: TextView by lazy { findViewById(R.id.btnChangeMusicApp) }
    private val progressBar: ProgressBar by lazy { findViewById(R.id.musicProgress) }

    private var boundPkg: String? = null
    private var controller: MediaController? = null

    init {
        inflate(context, R.layout.view_card_music, this)
    }

    /** 绑定音乐应用（空则尚未绑定） */
    fun bindMusicApp(pkg: String?, onPick: () -> Unit) {
        boundPkg = pkg
        btnChange.setOnClickListener { onPick() }
        appIcon.setImageDrawable(AppUtil.appIcon(context, pkg))
        refresh()
    }

    /** 由 MainActivity 的 MediaSession 监听喂入当前媒体控制器 */
    fun attachController(c: MediaController?) {
        controller = c
        refresh()
    }

    /** 刷新曲目信息与播放状态 */
    fun refresh() {
        val c = controller
        // 原生 PlaybackState 没有 isPlaying 属性，用 STATE_PLAYING 状态判断
        val playing = c?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        val metadata = c?.metadata

        // 标题：优先媒体元数据（曲名），否则显示绑定的应用名
        val metaTitle = metadata?.description?.title?.toString()
        titleView.text = metaTitle ?: (AppUtil.appName(context, boundPkg) ?: "未在播放")
        subtitleView.text = metadata?.description?.subtitle?.toString()
            ?: (if (boundPkg == null) context.getString(R.string.not_playing) else "点击播放键启动应用")

        // 播放/暂停图标随状态切换
        btnPlayPause.setImageResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }

    /** 进度条刷新（由主界面定时器驱动，约每 500ms） */
    fun refreshProgress() {
        val c = controller ?: return
        val state = c.playbackState ?: return
        val duration = c.metadata?.description?.extras?.getLong(
            android.media.MediaMetadata.METADATA_KEY_DURATION
        ) ?: 0L
        if (duration > 0) {
            val pos = state.position
            progressBar.progress = ((pos.toDouble() / duration.toDouble()) * 100).toInt()
        }
    }

    /** 点击上一曲 */
    fun prev() {
        val c = controller
        if (c?.playbackState != null) {
            c.transportControls.skipToPrevious()
        } else {
            boundPkg?.let { AppUtil.launchApp(context, it) }
        }
    }

    /** 点击播放/暂停：无会话启动应用，有会话控制播放 */
    fun playPause() {
        val c = controller
        if (c?.playbackState == null) {
            boundPkg?.let { AppUtil.launchApp(context, it) }
            return
        }
        val playing = c.playbackState.state == android.media.session.PlaybackState.STATE_PLAYING
        if (playing) c.transportControls.pause() else c.transportControls.play()
    }

    /** 点击下一曲 */
    fun next() {
        val c = controller
        if (c?.playbackState != null) {
            c.transportControls.skipToNext()
        } else {
            boundPkg?.let { AppUtil.launchApp(context, it) }
        }
    }

    /** 接线：在 bind 之后调用一次即可 */
    fun wireControls() {
        btnPrev.setOnClickListener { prev() }
        btnPlayPause.setOnClickListener { playPause() }
        btnNext.setOnClickListener { next() }
    }
}
