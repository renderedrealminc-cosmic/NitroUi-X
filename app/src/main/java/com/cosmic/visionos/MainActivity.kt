package com.cosmic.visionos

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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
    // 1 = Lock Screen (Big Time + Press Any D-Pad Icon)
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

        // Dynamic Auto-Changing Liquid Wallpaper (Used in Screen 2, 3, and 4)
        liquidWallpaper = AutoChangingLiquidWallpaper(this)
        rootFrame.addView(liquidWallpaper, matchParent())

        // Build All 4 Screens
        buildScreen1SolidBlackBoot()
        buildScreen2LockScreen()
        buildScreen3Win12Lounge()
        buildScreen4MainHome()

        switchToScreen(0)
        startLiveClocks()
        startAutoWallpaperChanger()

        // Scan TV Apps in background
        thread { scanInstalledApps() }

        // Start Java Master Boss to initialize C++, Rust, Python, Ruby & unlock Boot Screen
        CosmicMasterBoss.initializeAllEngines(this) { report ->
            engineStatusSubtitle.text = report
            switchToScreen(1) // Move from Solid Black Boot to Lock Screen
        }

        // Connect to Firebase Cloud (visionos-91949)
        syncWithFirebaseCloud()
    }

    // =========================================================================
    // SCREEN 1: SOLID BLACK BOOT ("VISION OS" + Blurry PUBG/Unreal style "cosmic.inc")
    // =========================================================================
    private fun buildScreen1SolidBlackBoot() {
        screen1Boot = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK) // 100% Solid Black Background
        }

        val centerStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        // Bold White VISION OS Title
        val visionTitle = TextView(this).apply {
            text = "V I S I O N   O S"
            textSize = 48f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
        }

        // Subtle Blurred Watermark "cosmic.inc" right below (Krafton / Unreal style)
        val cosmicWatermark = TextView(this).apply {
            text = "c o s m i c . i n c"
            textSize = 14f
            setTextColor(Color.parseColor("#88FFFFFF"))
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setPadding(0, 14, 0, 0)
            gravity = Gravity.CENTER
            // Soft optical blur glow like Unreal Engine watermark
            setShadowLayer(12f, 0f, 0f, Color.parseColor("#66FFFFFF"))
            alpha = 0.65f
        }

        centerStack.addView(visionTitle)
        centerStack.addView(cosmicWatermark)

        screen1Boot.addView(centerStack, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        rootFrame.addView(screen1Boot, matchParent())
    }

    // =========================================================================
    // SCREEN 2: LOCK SCREEN (Only Big Time + Bottom D-Pad Prompt)
    // =========================================================================
    private fun buildScreen2LockScreen() {
        screen2Lock = FrameLayout(this)

        // Single Clean Giant Time Display near Top-Center
        lockTimeTv = TextView(this).apply {
            text = "08:15"
            textSize = 104f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            setShadowLayer(35f, 0f, 10f, Color.parseColor("#AA000000"))
            gravity = Gravity.CENTER
        }

        val clockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = 110 }

        screen2Lock.addView(lockTimeTv, clockLp)

        // Bottom Small D-Pad Icon & Hint
        val dpadHintBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(40, 18, 40, 18)
            background = createLiquidGlass(Color.parseColor("#28FFFFFF"), Color.parseColor("#66FFFFFF"), 50f)
            isFocusable = true
            isClickable = true
            setOnClickListener { switchToScreen(2) }
        }

        val dpadIcon = TextView(this).apply {
            text = "🎮  Press Any D-Pad Button to Enter Cosmic Lounge"
            textSize = 15f
            setTextColor(Color.parseColor("#F1F5F9"))
        }
        dpadHintBox.addView(dpadIcon)

        val bottomLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = 54 }

        screen2Lock.addView(dpadHintBox, bottomLp)
        rootFrame.addView(screen2Lock, matchParent())
    }

    // =========================================================================
    // SCREEN 3: COSMIC LOUNGE (WINDOWS 12 PILL DOCK WITH MOVEABLE APPS)
    // =========================================================================
    private fun buildScreen3Win12Lounge() {
        screen3Win12Lounge = FrameLayout(this)

        // Top Time Display (Just like Lock Screen, clean and minimal)
        loungeTimeTv = TextView(this).apply {
            text = "08:15 PM"
            textSize = 68f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setShadowLayer(25f, 0f, 6f, Color.parseColor("#99000000"))
            gravity = Gravity.CENTER
        }

        val topClockLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.CENTER_HORIZONTAL
        ).apply { topMargin = 75 }
        screen3Win12Lounge.addView(loungeTimeTv, topClockLp)

        // Bottom Windows 12 Pill-Shaped Glass Container
        val bottomWin12Container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(34, 22, 34, 22)
            background = createLiquidGlass(Color.parseColor("#400F172A"), Color.parseColor("#9900E5FF"), 64f, 3)
        }

        val pillHeader = TextView(this).apply {
            text = "Windows 12 Quick Dock • Long-Press OK on an App to Move Left/Right • Press DOWN ▼ for Main OS"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 14)
        }

        win12AppsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

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
        ).apply { bottomMargin = 36 }

        screen3Win12Lounge.addView(bottomWin12Container, dockLp)
        rootFrame.addView(screen3Win12Lounge, matchParent())
    }

    private fun renderWin12DockApps() {
        win12AppsRow.removeAllViews()
        if (dockPinnedApps.isEmpty() && allTvApps.isNotEmpty()) {
            dockPinnedApps.addAll(allTvApps.take(5))
        }

        dockPinnedApps.forEachIndexed { index, app ->
            val isMoving = (selectedDockIndexForMove == index)
            val strokeColor = if (isMoving) Color.parseColor("#00FF9D") else Color.parseColor("#66FFFFFF")

            val itemBox = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(18, 16, 18, 14)
                background = createLiquidGlass(Color.parseColor("#351E293B"), strokeColor, 36f, if (isMoving) 5 else 2)
                isFocusable = true
                isClickable = true
                isLongClickable = true

                setOnFocusChangeListener { v, hasFocus ->
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

                // Short Press: Launch App (or Drop App if moving)
                setOnClickListener {
                    if (selectedDockIndexForMove != -1) {
                        val fromIdx = selectedDockIndexForMove
                        selectedDockIndexForMove = -1
                        Collections.swap(dockPinnedApps, fromIdx, index)
                        renderWin12DockApps()
                        Toast.makeText(this@MainActivity, "App Moved in Dock!", Toast.LENGTH_SHORT).show()
                    } else {
                        launchTvApp(app)
                    }
                }

                // Long Press OK: Activate Move Mode for this App
                setOnLongClickListener {
                    selectedDockIndexForMove = if (selectedDockIndexForMove == index) -1 else index
                    Toast.makeText(
                        this@MainActivity,
                        "Move Mode: Select another Dock slot & press OK to swap!",
                        Toast.LENGTH_SHORT
                    ).show()
                    renderWin12DockApps()
                    true
                }
            }

            val img = ImageView(this).apply {
                if (app.icon != null) setImageDrawable(app.icon)
                else setImageResource(android.R.drawable.sym_def_app_icon)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }

            val label = TextView(this).apply {
                text = if (isMoving) "⇄ ${app.name}" else app.name
                textSize = 12f
                setTextColor(Color.WHITE)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(0, 8, 0, 0)
            }

            itemBox.addView(img, LinearLayout.LayoutParams(68, 68))
            itemBox.addView(label, LinearLayout.LayoutParams(120, ViewGroup.LayoutParams.WRAP_CONTENT))

            val lp = LinearLayout.LayoutParams(145, 125).apply {
                setMargins(10, 4, 10, 4)
            }
            win12AppsRow.addView(itemBox, lp)
        }
    }

    // =========================================================================
    // SCREEN 4: MAIN HOME SCREEN (LEFT VERTICAL PILL BAR + ALL APPS + CLOUD)
    // =========================================================================
    private fun buildScreen4MainHome() {
        screen4MainHome = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#78020617"))
        }

        val rootRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(34, 28, 34, 28)
        }

        // Left Side Vertical Pill-Shaped Glass Navigation Bar
        val leftVerticalPill = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 26, 16, 26)
            background = createLiquidGlass(Color.parseColor("#481E293B"), Color.parseColor("#8800E5FF"), 70f, 3)
        }

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

        // Right Workspace
        val rightWorkspace = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(34, 6, 10, 6)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        mainSectionTitle = TextView(this).apply {
            text = "VisionOS Main Home"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        engineStatusSubtitle = TextView(this).apply {
            text = "cosmic.inc • 6-Language Core Active"
            textSize = 12f
            setTextColor(Color.parseColor("#00E5FF"))
        }

        titleStack.addView(mainSectionTitle)
        titleStack.addView(engineStatusSubtitle)

        homeClockTv = TextView(this).apply {
            text = "08:15 PM"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        topBar.addView(titleStack)
        topBar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        topBar.addView(createGlassButton("🎨 Next Wallpaper") { liquidWallpaper.nextPalette() })
        topBar.addView(View(this), LinearLayout.LayoutParams(16, 1))
        topBar.addView(homeClockTv)

        val gridScroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setPadding(0, 20, 0, 20)
        }

        mainAppsGrid = GridLayout(this).apply {
            columnCount = 4
        }
        gridScroll.addView(mainAppsGrid)

        rightWorkspace.addView(topBar)
        rightWorkspace.addView(gridScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        rootRow.addView(leftVerticalPill, LinearLayout.LayoutParams(106, ViewGroup.LayoutParams.MATCH_PARENT))
        rootRow.addView(rightWorkspace, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        screen4MainHome.addView(rootRow, matchParent())
        rootFrame.addView(screen4MainHome, matchParent())
    }

    // =========================================================================
    // TV APP SCANNER & CLOUD ENGINE
    // =========================================================================
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
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(20, 22, 20, 18)
                background = createLiquidGlass(Color.parseColor("#381E293B"), Color.parseColor("#55FFFFFF"), 32f, 2)
                isFocusable = true
                isClickable = true

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(140).start()
             
