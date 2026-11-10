package com.cosmic.visionos

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {

    // 0 = Solid Black Boot (VISION OS + cosmic.inc)
    // 1 = Lock Screen (Big Time + Bottom D-Pad Prompt)
    // 2 = Screen 3: Cosmic Lounge (Time + Auto-changing BG + Win12 Bottom Pill with Moveable Apps)
    // 3 = Screen 4: Main Home Screen (Left Vertical Pill Bar + Full Apps Grid + Cloud + Search)
    private var currentScreen = 0
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var rootFrame: FrameLayout
    private lateinit var liquidWallpaper: AutoChangingLiquidWallpaper

    private lateinit var screen1Boot: FrameLayout
    private lateinit var screen2Lock: FrameLayout
    private lateinit var screen3Win12Lounge: FrameLayout
    private lateinit var screen4MainHome: FrameLayout

    private lateinit var lockTimeTv: TextView
    private lateinit var loungeTimeTv: TextView
    private lateinit var homeClockTv: TextView
    private lateinit var win12AppsRow: LinearLayout
    private lateinit var mainAppsGrid: GridLayout
    private lateinit var mainSectionTitle: TextView
    private lateinit var engineStatusSubtitle: TextView

    private val allTvApps = mutableListOf<TvApp>()
    private val dockPinnedApps = mutableListOf<TvApp>()
    private val recentApps = mutableListOf<TvApp>()
    private var selectedDockIndexForMove: Int = -1

    data class TvApp(val name: String, val pkg: String, val icon: Drawable?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        rootFrame = FrameLayout(this)
        setContentView(rootFrame)

        liquidWallpaper = AutoChangingLiquidWallpaper()
        rootFrame.addView(liquidWallpaper, matchParent())

        buildScreen1SolidBlackBoot()
        buildScreen2LockScreen()
        buildScreen3Win12Lounge()
        buildScreen4MainHome()

        switchToScreen(0)
        startLiveClocks()
        startAutoWallpaperChanger()

        thread { scanInstalledApps() }

        CosmicMasterBoss.initializeAllEngines(this) { report ->
            engineStatusSubtitle.text = report
            switchToScreen(1)
        }

        syncWithFirebaseCloud(false)
    }

    // SCREEN 1: SOLID BLACK BOOT ("VISION OS" + Blurry "cosmic.inc")
    private fun buildScreen1SolidBlackBoot() {
        screen1Boot = FrameLayout(this)
        screen1Boot.setBackgroundColor(Color.BLACK)

        val centerStack = LinearLayout(this)
        centerStack.orientation = LinearLayout.VERTICAL
        centerStack.gravity = Gravity.CENTER

        val visionTitle = TextView(this)
        visionTitle.text = "V I S I O N   O S"
        visionTitle.textSize = 48f
        visionTitle.setTextColor(Color.WHITE)
        visionTitle.typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        visionTitle.letterSpacing = 0.12f
        visionTitle.gravity = Gravity.CENTER

        val cosmicWatermark = TextView(this)
        cosmicWatermark.text = "c o s m i c . i n c"
        cosmicWatermark.textSize = 14f
        cosmicWatermark.setTextColor(Color.parseColor("#88FFFFFF"))
        cosmicWatermark.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        cosmicWatermark.setPadding(0, 14, 0, 0)
        cosmicWatermark.gravity = Gravity.CENTER
        cosmicWatermark.setShadowLayer(12f, 0f, 0f, Color.parseColor("#66FFFFFF"))
        cosmicWatermark.alpha = 0.65f

        centerStack.addView(visionTitle)
        centerStack.addView(cosmicWatermark)

        screen1Boot.addView(
            centerStack,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )
        rootFrame.addView(screen1Boot, matchParent())
    }

    // SCREEN 2: LOCK SCREEN
    private fun buildScreen2LockScreen() {
        screen2Lock = FrameLayout(this)

        lockTimeTv = TextView(this)
        lockTimeTv.text = "08:15"
        lockTimeTv.textSize = 104f
        lockTimeTv.setTextColor(Color.WHITE)
        lockTimeTv.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        lockTimeTv.setShadowLayer(35f, 0f, 10f, Color.parseColor("#AA000000"))
        lockTimeTv.gravity = Gravity.CENTER

        val clockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )
        clockLp.topMargin = 110
        screen2Lock.addView(lockTimeTv, clockLp)

        val dpadHintBox = LinearLayout(this)
        dpadHintBox.orientation = LinearLayout.HORIZONTAL
        dpadHintBox.gravity = Gravity.CENTER
        dpadHintBox.setPadding(40, 18, 40, 18)
        dpadHintBox.background = createLiquidGlass(Color.parseColor("#28FFFFFF"), Color.parseColor("#66FFFFFF"), 50f, 2)
        dpadHintBox.isFocusable = true
        dpadHintBox.isClickable = true
        dpadHintBox.setOnClickListener { switchToScreen(2) }

        val dpadIcon = TextView(this)
        dpadIcon.text = "🎮  Press Any D-Pad Button to Enter Cosmic Lounge"
        dpadIcon.textSize = 15f
        dpadIcon.setTextColor(Color.parseColor("#F1F5F9"))
        dpadHintBox.addView(dpadIcon)

        val bottomLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        bottomLp.bottomMargin = 54
        screen2Lock.addView(dpadHintBox, bottomLp)

        rootFrame.addView(screen2Lock, matchParent())
    }

    // SCREEN 3: COSMIC LOUNGE (WINDOWS 12 PILL DOCK WITH MOVEABLE APPS)
    private fun buildScreen3Win12Lounge() {
        screen3Win12Lounge = FrameLayout(this)

        loungeTimeTv = TextView(this)
        loungeTimeTv.text = "08:15 PM"
        loungeTimeTv.textSize = 68f
        loungeTimeTv.setTextColor(Color.WHITE)
        loungeTimeTv.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        loungeTimeTv.setShadowLayer(25f, 0f, 6f, Color.parseColor("#99000000"))
        loungeTimeTv.gravity = Gravity.CENTER

        val topClockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        )
        topClockLp.topMargin = 75
        screen3Win12Lounge.addView(loungeTimeTv, topClockLp)

        val bottomWin12Container = LinearLayout(this)
        bottomWin12Container.orientation = LinearLayout.VERTICAL
        bottomWin12Container.gravity = Gravity.CENTER
        bottomWin12Container.setPadding(34, 22, 34, 22)
        bottomWin12Container.background = createLiquidGlass(Color.parseColor("#400F172A"), Color.parseColor("#9900E5FF"), 64f, 3)

        val pillHeader = TextView(this)
        pillHeader.text = "Windows 12 Quick Dock • Long-Press OK on an App to Move • Press DOWN ▼ for Main OS"
        pillHeader.textSize = 12f
        pillHeader.setTextColor(Color.parseColor("#CBD5E1"))
        pillHeader.gravity = Gravity.CENTER
        pillHeader.setPadding(0, 0, 0, 14)

        win12AppsRow = LinearLayout(this)
        win12AppsRow.orientation = LinearLayout.HORIZONTAL
        win12AppsRow.gravity = Gravity.CENTER

        val goMainOsBtn = createGlassButton("▼  Press DOWN for Main Home Screen") {
            switchToScreen(3)
        }

        bottomWin12Container.addView(pillHeader)
        bottomWin12Container.addView(win12AppsRow)
        bottomWin12Container.addView(View(this), LinearLayout.LayoutParams(1, 12))
        bottomWin12Container.addView(goMainOsBtn)

        val dockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        dockLp.bottomMargin = 36
        screen3Win12Lounge.addView(bottomWin12Container, dockLp)

        rootFrame.addView(screen3Win12Lounge, matchParent())
    }

    private fun renderWin12DockApps() {
        win12AppsRow.removeAllViews()
        if (dockPinnedApps.isEmpty() && allTvApps.isNotEmpty()) {
            dockPinnedApps.addAll(allTvApps.take(5))
        }

        for (index in dockPinnedApps.indices) {
            val app = dockPinnedApps[index]
            val isMoving = (selectedDockIndexForMove == index)
            val strokeColor = if (isMoving) Color.parseColor("#00FF9D") else Color.parseColor("#66FFFFFF")

            val itemBox = LinearLayout(this)
            itemBox.orientation = LinearLayout.VERTICAL
            itemBox.gravity = Gravity.CENTER
            itemBox.setPadding(18, 16, 18, 14)
            itemBox.background = createLiquidGlass(Color.parseColor("#351E293B"), strokeColor, 36f, if (isMoving) 5 else 2)
            itemBox.isFocusable = true
            itemBox.isClickable = true
            itemBox.isLongClickable = true

            itemBox.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(130).start()
                    if (selectedDockIndexForMove == -1) {
                        v.background = createLiquidGlass(Color.parseColor("#660F172A"), Color.parseColor("#00E5FF"), 36f, 4)
                    }
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(130).start()
                    v.background = createLiquidGlass(Color.parseColor("#351E293B"), strokeColor, 36f, 2)
                }
            }

            itemBox.setOnClickListener {
                if (selectedDockIndexForMove != -1) {
                    val fromIdx = selectedDockIndexForMove
                    selectedDockIndexForMove = -1
                    val temp = dockPinnedApps[fromIdx]
                    dockPinnedApps[fromIdx] = dockPinnedApps[index]
                    dockPinnedApps[index] = temp
                    renderWin12DockApps()
                    Toast.makeText(this, "App Moved in Dock!", Toast.LENGTH_SHORT).show()
                } else {
                    launchTvApp(app)
                }
            }

            itemBox.setOnLongClickListener {
                selectedDockIndexForMove = if (selectedDockIndexForMove == index) -1 else index
                Toast.makeText(this, "Move Mode: Click another Dock slot to swap!", Toast.LENGTH_SHORT).show()
                renderWin12DockApps()
                true
            }

            val img = ImageView(this)
            if (app.icon != null) img.setImageDrawable(app.icon)
            else img.setImageResource(android.R.drawable.sym_def_app_icon)
            img.scaleType = ImageView.ScaleType.FIT_CENTER

            val label = TextView(this)
            label.text = if (isMoving) "⇄ ${app.name}" else app.name
            label.textSize = 12f
            label.setTextColor(Color.WHITE)
            label.maxLines = 1
            label.ellipsize = TextUtils.TruncateAt.END
            label.setPadding(0, 8, 0, 0)
            label.gravity = Gravity.CENTER

            itemBox.addView(img, LinearLayout.LayoutParams(68, 68))
            itemBox.addView(label, LinearLayout.LayoutParams(120, ViewGroup.LayoutParams.WRAP_CONTENT))

            val lp = LinearLayout.LayoutParams(145, 125)
            lp.setMargins(10, 4, 10, 4)
            win12AppsRow.addView(itemBox, lp)
        }
    }

    // SCREEN 4: MAIN HOME SCREEN (LEFT VERTICAL PILL BAR + ALL APPS + CLOUD)
    private fun buildScreen4MainHome() {
        screen4MainHome = FrameLayout(this)
        screen4MainHome.setBackgroundColor(Color.parseColor("#78020617"))

        val rootRow = LinearLayout(this)
        rootRow.orientation = LinearLayout.HORIZONTAL
        rootRow.setPadding(34, 28, 34, 28)

        val leftVerticalPill = LinearLayout(this)
        leftVerticalPill.orientation = LinearLayout.VERTICAL
        leftVerticalPill.gravity = Gravity.CENTER
        leftVerticalPill.setPadding(16, 26, 16, 26)
        leftVerticalPill.background = createLiquidGlass(Color.parseColor("#481E293B"), Color.parseColor("#8800E5FF"), 70f, 3)

        val navLounge = createPillCircleIcon("⬆️") { switchToScreen(2) }
        val navHome = createPillCircleIcon("🏠") { renderMainGrid(allTvApps, "VisionOS Main Home (${allTvApps.size} Apps)") }
        val navSearch = createPillCircleIcon("🔍") { openUniversalSearch() }
        val navRecents = createPillCircleIcon("📑") { renderMainGrid(recentApps, "Recents Multitasking (${recentApps.size})") }
        val navCloud = createPillCircleIcon("☁️") { syncWithFirebaseCloud(true) }
        val navSettings = createPillCircleIcon("⚙️") { openStatusControlMenu() }

        leftVerticalPill.addView(navLounge)
        leftVerticalPill.addView(View(this), LinearLayout.LayoutParams(1, 16))
        leftVerticalPill.addView(navHome)
        leftVerticalPill.addView(View(this), LinearLayout.LayoutParams(1, 16))
        leftVerticalPill.addView(navSearch)
        leftVerticalPill.addView(View(this), LinearLayout.LayoutParams(1, 16))
        leftVerticalPill.addView(navRecents)
        leftVerticalPill.addView(View(this), LinearLayout.LayoutParams(1, 16))
        leftVerticalPill.addView(navCloud)
        leftVerticalPill.addView(View(this), LinearLayout.LayoutParams(1, 16))
        leftVerticalPill.addView(navSettings)

        val rightWorkspace = LinearLayout(this)
        rightWorkspace.orientation = LinearLayout.VERTICAL
        rightWorkspace.setPadding(34, 6, 10, 6)

        val topBar = LinearLayout(this)
        topBar.orientation = LinearLayout.HORIZONTAL
        topBar.gravity = Gravity.CENTER_VERTICAL

        val titleStack = LinearLayout(this)
        titleStack.orientation = LinearLayout.VERTICAL

        mainSectionTitle = TextView(this)
        mainSectionTitle.text = "VisionOS Main Home"
        mainSectionTitle.textSize = 26f
        mainSectionTitle.setTextColor(Color.WHITE)
        mainSectionTitle.typeface = Typeface.DEFAULT_BOLD

        engineStatusSubtitle = TextView(this)
        engineStatusSubtitle.text = "cosmic.inc • 6-Language Core Active"
        engineStatusSubtitle.textSize = 12f
        engineStatusSubtitle.setTextColor(Color.parseColor("#00E5FF"))

        titleStack.addView(mainSectionTitle)
        titleStack.addView(engineStatusSubtitle)

        homeClockTv = TextView(this)
        homeClockTv.text = "08:15 PM"
        homeClockTv.textSize = 20f
        homeClockTv.setTextColor(Color.WHITE)
        homeClockTv.typeface = Typeface.DEFAULT_BOLD

        topBar.addView(titleStack)
        topBar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        topBar.addView(createGlassButton("🎨 Next Wallpaper") { liquidWallpaper.nextPalette() })
        topBar.addView(View(this), LinearLayout.LayoutParams(16, 1))
        topBar.addView(homeClockTv)

        val gridScroll = ScrollView(this)
        gridScroll.isVerticalScrollBarEnabled = false
        gridScroll.setPadding(0, 20, 0, 20)

        mainAppsGrid = GridLayout(this)
        mainAppsGrid.columnCount = 4
        gridScroll.addView(mainAppsGrid)

        rightWorkspace.addView(topBar)
        rightWorkspace.addView(
            gridScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        rootRow.addView(leftVerticalPill, LinearLayout.LayoutParams(106, ViewGroup.LayoutParams.MATCH_PARENT))
        rootRow.addView(rightWorkspace, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        screen4MainHome.addView(rootRow, matchParent())
        rootFrame.addView(screen4MainHome, matchParent())
    }

    private fun scanInstalledApps() {
        val pm = packageManager
        val found = mutableListOf<TvApp>()
        try {
            val pkgs = pm.getInstalledApplications(0)
            for (info in pkgs) {
                val launch = pm.getLeanbackLaunchIntentForPackage(info.packageName)
                    ?: pm.getLaunchIntentForPackage(info.packageName)
                if (launch != null && info.packageName != packageName) {
                    val label = pm.getApplicationLabel(info).toString()
                    val icon = try { pm.getApplicationIcon(info) } catch (_: Exception) { null }
                    found.add(TvApp(label, info.packageName, icon))
                }
            }
            found.sortBy { it.name.lowercase() }
        } catch (_: Exception) {}

        allTvApps.clear()
        allTvApps.addAll(found)
        dockPinnedApps.clear()
        dockPinnedApps.addAll(found.take(5))

        handler.post {
            renderWin12DockApps()
            renderMainGrid(allTvApps, "VisionOS Main Home (${allTvApps.size} Apps)")
        }
    }

    private fun renderMainGrid(list: List<TvApp>, headerTitle: String) {
        mainSectionTitle.text = headerTitle
        mainAppsGrid.removeAllViews()

        for (app in list) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.gravity = Gravity.CENTER
            card.setPadding(20, 22, 20, 18)
            card.background = createLiquidGlass(Color.parseColor("#381E293B"), Color.parseColor("#55FFFFFF"), 32f, 2)
            card.isFocusable = true
            card.isClickable = true

            card.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(140).start()
                    v.background = createLiquidGlass(Color.parseColor("#660F172A"), Color.parseColor("#00E5FF"), 32f, 4)
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(140).start()
                    v.background = createLiquidGlass(Color.parseColor("#381E293B"), Color.parseColor("#55FFFFFF"), 32f, 2)
                }
            }

            card.setOnClickListener { launchTvApp(app) }

            val iconImg = ImageView(this)
            if (app.icon != null) iconImg.setImageDrawable(app.icon)
            else iconImg.setImageResource(android.R.drawable.sym_def_app_icon)
            iconImg.scaleType = ImageView.ScaleType.FIT_CENTER

            val titleTv = TextView(this)
            titleTv.text = app.name
            titleTv.textSize = 14f
            titleTv.setTextColor(Color.WHITE)
            titleTv.gravity = Gravity.CENTER
            titleTv.maxLines = 1
            titleTv.ellipsize = TextUtils.TruncateAt.END
            titleTv.setPadding(0, 12, 0, 0)

            card.addView(iconImg, LinearLayout.LayoutParams(90, 90))
            card.addView(titleTv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

            val glp = GridLayout.LayoutParams()
            glp.width = 240
            glp.height = 185
            glp.setMargins(14, 14, 14, 14)
            mainAppsGrid.addView(card, glp)
        }
    }

    private fun launchTvApp(app: TvApp) {
        try {
            recentApps.removeAll { it.pkg == app.pkg }
            recentApps.add(0, app)
            val intent
