package com.sandy.ravenscreenpet

import android.app.*
import android.content.*
import android.graphics.*
import android.os.*
import android.view.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class RavenMood { IDLE, WALK, HOP, LOOK, PREEN, FLUFF, SLEEP, FLY, PECK, CAW, ANNOYED, HAPPY }

class RavenService : Service(), Choreographer.FrameCallback {
    private lateinit var wm: WindowManager
    private lateinit var raven: RavenView
    private lateinit var lp: WindowManager.LayoutParams
    private val choreographer by lazy { Choreographer.getInstance() }
    private var direction = 1
    private var x = 0f
    private var y = 0f
    private var groundY = 0f
    private var stateStart = 0L
    private var stateDuration = 1800L
    private var lastFrameNanos = 0L
    private var animTime = 0f
    private var flyStartX = 0f
    private var flyStartY = 0f
    private var flyTargetX = 0f
    private var flyTargetY = 0f

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("raven", "Raven screen pet", NotificationManager.IMPORTANCE_LOW))
        startForeground(7, Notification.Builder(this, "raven")
            .setContentTitle("Raven is hanging out")
            .setContentText("Your fluffy screen raven is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .build())
        showRaven()
    }

    private fun showRaven() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        raven = RavenView(this)
        val dm = resources.displayMetrics
        val size = dp(76)
        x = dp(18).toFloat()
        groundY = (dm.heightPixels - dp(165)).toFloat()
        y = groundY
        lp = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = this@RavenService.x.toInt()
            this.y = this@RavenService.y.toInt()
        }
        wm.addView(raven, lp)
        raven.setMoveListener { dx, dy ->
            x = (x + dx).coerceIn(0f, (dm.widthPixels - size).toFloat())
            y = (y + dy).coerceIn(dp(35).toFloat(), (dm.heightPixels - size - dp(35)).toFloat())
            groundY = y
            applyPosition()
        }
        raven.onDoubleTap = {
            beginMood(RavenMood.HAPPY, 1600L)
        }
        beginMood(RavenMood.IDLE, 1200L)
        choreographer.postFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!::raven.isInitialized) return
        if (lastFrameNanos == 0L) lastFrameNanos = frameTimeNanos
        val dt = ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNanos = frameTimeNanos

        if (!raven.dragging) {
            animTime += dt
            val now = SystemClock.uptimeMillis()
            if (now - stateStart >= stateDuration) chooseBehavior()
            updateMotion(dt, now)
            raven.animationTime = animTime
            raven.invalidate()
        }
        choreographer.postFrameCallback(this)
    }

    private fun beginMood(newMood: RavenMood, durationMs: Long) {
        raven.changeMood(newMood)
        stateStart = SystemClock.uptimeMillis()
        stateDuration = durationMs
        raven.facing = direction
        if (newMood != RavenMood.HOP && newMood != RavenMood.FLY) y = groundY
    }

    private fun chooseBehavior() {
        val roll = Random.nextInt(100)
        when {
            roll < 29 -> {
                if (Random.nextInt(100) < 28) direction *= -1
                beginMood(RavenMood.WALK, Random.nextLong(2200L, 5200L))
            }
            roll < 44 -> beginMood(RavenMood.IDLE, Random.nextLong(1300L, 3000L))
            roll < 56 -> beginMood(RavenMood.LOOK, Random.nextLong(1600L, 3200L))
            roll < 66 -> beginMood(RavenMood.PREEN, Random.nextLong(2400L, 4300L))
            roll < 73 -> beginMood(RavenMood.PECK, Random.nextLong(1400L, 2500L))
            roll < 79 -> beginMood(RavenMood.FLUFF, Random.nextLong(1300L, 2200L))
            roll < 85 -> {
                if (Random.nextBoolean()) direction *= -1
                beginMood(RavenMood.HOP, Random.nextLong(850L, 1150L))
            }
            roll < 90 -> beginMood(RavenMood.CAW, Random.nextLong(1000L, 1700L))
            roll < 94 -> startFlight()
            else -> beginMood(RavenMood.SLEEP, Random.nextLong(5000L, 10000L))
        }
    }

    private fun startFlight() {
        val dm = resources.displayMetrics
        val maxX = (dm.widthPixels - lp.width).toFloat()
        flyStartX = x
        flyStartY = y
        val distance = dp(Random.nextInt(85, 180)).toFloat()
        direction = if (x > maxX * .70f) -1 else if (x < maxX * .30f) 1 else if (Random.nextBoolean()) 1 else -1
        flyTargetX = (x + direction * distance).coerceIn(0f, maxX)
        flyTargetY = groundY
        beginMood(RavenMood.FLY, Random.nextLong(1200L, 1750L))
    }

    private fun updateMotion(dt: Float, now: Long) {
        val dm = resources.displayMetrics
        val maxX = (dm.widthPixels - lp.width).toFloat()
        val progress = ((now - stateStart).toFloat() / stateDuration.toFloat()).coerceIn(0f, 1f)

        when (raven.mood) {
            RavenMood.WALK -> {
                val ease = edgeEase(progress)
                x += direction * dpF(26f) * dt * ease
                y = groundY
            }
            RavenMood.HOP -> {
                val ease = edgeEase(progress)
                x += direction * dpF(34f) * dt * ease
                y = groundY - sin(progress * PI).toFloat() * dpF(18f)
            }
            RavenMood.FLY -> {
                val smooth = progress * progress * (3f - 2f * progress)
                x = lerp(flyStartX, flyTargetX, smooth)
                val baseY = lerp(flyStartY, flyTargetY, smooth)
                y = baseY - sin(progress * PI).toFloat() * dpF(70f)
            }
            else -> y = groundY
        }

        if (x <= 0f || x >= maxX) {
            x = x.coerceIn(0f, maxX)
            direction *= -1
            raven.facing = direction
        }
        y = y.coerceIn(dp(35).toFloat(), (dm.heightPixels - lp.height - dp(35)).toFloat())
        applyPosition()
    }

    private fun edgeEase(p: Float): Float {
        val fade = 0.18f
        return when {
            p < fade -> smoothStep(p / fade)
            p > 1f - fade -> smoothStep((1f - p) / fade)
            else -> 1f
        }
    }

    private fun smoothStep(v: Float): Float {
        val t = v.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun applyPosition() {
        lp.x = x.toInt()
        lp.y = y.toInt()
        try { wm.updateViewLayout(raven, lp) } catch (_: Exception) { }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dpF(v: Float) = v * resources.displayMetrics.density

    override fun onDestroy() {
        choreographer.removeFrameCallback(this)
        if (::raven.isInitialized) try { wm.removeView(raven) } catch (_: Exception) { }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}

class RavenView(context: Context) : View(context) {
    var facing = 1
    var mood = RavenMood.IDLE
        private set
    var animationTime = 0f
    var dragging = false
    var onDoubleTap: (() -> Unit)? = null
    private var lx = 0f; private var ly = 0f; private var downX = 0f; private var downY = 0f
    private var lastTap = 0L; private var pokes = 0
    private var mover: ((Int, Int) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val idle = load(R.drawable.raven_idle)
    private val walk1 = load(R.drawable.raven_walk1)
    private val walk2 = load(R.drawable.raven_walk2)
    private val walk3 = load(R.drawable.raven_walk3)
    private val look = load(R.drawable.raven_look)
    private val fluff = load(R.drawable.raven_fluff)
    private val peck = load(R.drawable.raven_peck)
    private val stretch = load(R.drawable.raven_stretch)
    private val hop = load(R.drawable.raven_hop)
    private val fly = load(R.drawable.raven_fly)
    private val sleep = load(R.drawable.raven_sleep)
    private val caw = load(R.drawable.raven_caw)
    private val happy = load(R.drawable.raven_happy)

    private fun load(id: Int): Bitmap = BitmapFactory.decodeResource(resources, id)
    fun setMoveListener(f: (Int, Int) -> Unit) { mover = f }
    fun changeMood(newMood: RavenMood) { mood = newMood; invalidate() }

    private fun frame(): Bitmap = when (mood) {
        RavenMood.WALK -> when (((animationTime * 5.2f).toInt()) % 4) { 0 -> walk1; 1 -> walk2; 2 -> walk3; else -> walk2 }
        RavenMood.LOOK, RavenMood.ANNOYED -> look
        RavenMood.PREEN, RavenMood.PECK -> peck
        RavenMood.FLUFF -> if (((animationTime * 3f).toInt()) % 3 == 0) stretch else fluff
        RavenMood.HOP -> hop
        RavenMood.FLY -> if (((animationTime * 4f).toInt()) % 2 == 0) fly else stretch
        RavenMood.SLEEP -> sleep
        RavenMood.CAW -> caw
        RavenMood.HAPPY -> happy
        else -> idle
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = frame()
        canvas.save()
        val bob = when (mood) {
            RavenMood.WALK -> sin(animationTime * 10f) * height * .012f
            RavenMood.IDLE, RavenMood.LOOK -> sin(animationTime * 2.2f) * height * .004f
            else -> 0f
        }
        canvas.translate(0f, bob)
        if (facing < 0) canvas.scale(-1f, 1f, width / 2f, height / 2f)
        val pad = if (mood == RavenMood.FLY || mood == RavenMood.FLUFF) 1f else 4f
        canvas.drawBitmap(bmp, null, RectF(pad, pad, width - pad, height - pad), paint)
        canvas.restore()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { dragging = true; lx=e.rawX; ly=e.rawY; downX=e.rawX; downY=e.rawY; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx=(e.rawX-lx).toInt(); val dy=(e.rawY-ly).toInt()
                if(abs(dx)+abs(dy)>2){mover?.invoke(dx,dy);lx=e.rawX;ly=e.rawY}
                return true
            }
            MotionEvent.ACTION_UP -> {
                dragging=false
                if(abs(e.rawX-downX)+abs(e.rawY-downY) < 18f) {
                    val now=System.currentTimeMillis()
                    if(now-lastTap < 350) { pokes=0; onDoubleTap?.invoke() }
                    else {
                        pokes++
                        mood = if(pokes>=5) RavenMood.ANNOYED else if(pokes%3==0) RavenMood.CAW else RavenMood.LOOK
                    }
                    lastTap=now; invalidate(); performClick()
                }
                return true
            }
        }
        return true
    }
    override fun performClick(): Boolean { super.performClick(); return true }
}
