package ir.greenmate.plantcare.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import ir.greenmate.plantcare.data.Plant
import ir.greenmate.plantcare.data.PlantCatalog
import ir.greenmate.plantcare.data.PlantGuide
import ir.greenmate.plantcare.data.PlantRepository
import ir.greenmate.plantcare.reminders.ReminderScheduler

class MainActivity : Activity() {
    private lateinit var repository: PlantRepository
    private lateinit var root: LinearLayout
    private var currentPage = Page.HOME
    private var selectedPlantId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PlantRepository(this)
        window.statusBarColor = Color.rgb(245, 247, 242)
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        requestNotificationPermissionIfNeeded()
        ReminderScheduler.scheduleAll(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) ReminderScheduler.scheduleAll(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        root.addView(buildTopBar(), LinearLayout.LayoutParams(MATCH, dp(62)))
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(12))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(20))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        when (currentPage) {
            Page.HOME -> buildHome(content)
            Page.PLANTS -> buildPlants(content)
            Page.GUIDE -> buildGuide(content)
            Page.SETTINGS -> buildSettings(content)
            Page.DETAIL -> buildPlantDetail(content)
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(buildBottomBar(), LinearLayout.LayoutParams(MATCH, dp(68)))
        setContentView(root)
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), 0, dp(20), 0)
            setBackgroundColor(BG)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val brand = TextView(this).apply {
            text = "سبزینه  🌿"
            textSize = 20f
            setTextColor(DARK_GREEN)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }
        bar.addView(brand, LinearLayout.LayoutParams(0, MATCH, 1f))
        val settings = makeTextButton("⚙", 22f, DARK_GREEN, WHITE, 42, 42)
        settings.contentDescription = "تنظیمات"
        settings.setOnClickListener {
            currentPage = Page.SETTINGS
            render()
        }
        bar.addView(settings)
        return bar
    }

    private fun buildBottomBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(WHITE)
            elevation = dp(10).toFloat()
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(6), dp(5), dp(6), dp(5))
        }
        val items = listOf(
            Triple(Page.HOME, "⌂", "خانه"),
            Triple(Page.PLANTS, "🪴", "گیاهان"),
            Triple(Page.GUIDE, "▤", "راهنما"),
            Triple(Page.SETTINGS, "⚙", "تنظیمات")
        )
        items.forEach { (page, icon, label) ->
            val selected = currentPage == page || (currentPage == Page.DETAIL && page == Page.PLANTS)
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(5), dp(4), dp(4))
                background = rounded(if (selected) LIGHT_GREEN else WHITE, 15)
                setOnClickListener {
                    currentPage = page
                    selectedPlantId = null
                    render()
                }
            }
            val iconView = text(icon, 19f, if (selected) GREEN else MUTED, true).apply { gravity = Gravity.CENTER }
            val labelView = text(label, 11f, if (selected) GREEN else MUTED, selected).apply { gravity = Gravity.CENTER }
            item.addView(iconView)
            item.addView(labelView)
            bar.addView(item, LinearLayout.LayoutParams(0, MATCH, 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
        return bar
    }

    private fun buildHome(container: LinearLayout) {
        val plants = repository.getAll()
        val due = plants.count { it.isDue() }
        addTitle(container, "سلام، به باغچه‌ات خوش اومدی", "مراقبت از گیاهانت رو ساده‌تر کن 🌱")

        val hero = card(LIGHT_GREEN).apply { setPadding(dp(18), dp(18), dp(18), dp(18)) }
        hero.addView(text("باغچه‌ی کوچک تو", 17f, DARK_GREEN, true))
        addSpace(hero, 6)
        hero.addView(text("${toPersian(plants.size)} گیاه ثبت‌شده داری و ${toPersian(due)} گیاه نیاز به بررسی دارند.", 14f, DARK_GREEN))
        addSpace(hero, 14)
        hero.addView(makeTextButton("＋  افزودن گیاه جدید", 14f, WHITE, GREEN, -1, 48).apply {
            setOnClickListener { showAddPlantPicker() }
        })
        container.addView(hero, matchWrap().withBottom(16))

        val statRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        statRow.addView(statCard("🪴", toPersian(plants.size), "همه گیاهان"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { setMargins(0, 0, dp(7), 0) })
        statRow.addView(statCard("💧", toPersian(due), "نیازمند آب"), LinearLayout.LayoutParams(0, dp(104), 1f).apply { setMargins(dp(7), 0, 0, 0) })
        container.addView(statRow, matchWrap().withBottom(22))

        addSectionTitle(container, "برنامه آبیاری", "گیاهانی که باید حواست به آن‌ها باشد")
        val sorted = plants.sortedBy { it.daysUntilDue() }.take(4)
        if (sorted.isEmpty()) {
            val empty = card(WHITE)
            empty.addView(text("هنوز گیاهی ثبت نکردی", 16f, DARK_GREEN, true))
            addSpace(empty, 7)
            empty.addView(text("اولین گیاهت رو اضافه کن تا زمان مراقبتش رو بهت یادآوری کنیم.", 13f, MUTED))
            addSpace(empty, 12)
            empty.addView(makeTextButton("افزودن اولین گیاه", 13f, WHITE, GREEN, -1, 44).apply {
                setOnClickListener { showAddPlantPicker() }
            })
            container.addView(empty, matchWrap().withBottom(14))
        } else {
            sorted.forEach { plant -> container.addView(plantRow(plant), matchWrap().withBottom(9)) }
        }

        val tip = card(WHITE)
        tip.addView(text("نکته سبزینه 💡", 15f, DARK_GREEN, true))
        addSpace(tip, 6)
        tip.addView(text("قبل از آبیاری، رطوبت خاک را با انگشت بررسی کن. فاصله آبیاری به فصل، نور، جنس خاک و اندازه گلدان بستگی دارد.", 13f, MUTED))
        container.addView(tip, matchWrap().withTop(8))
    }

    private fun buildPlants(container: LinearLayout) {
        val plants = repository.getAll().sortedBy { it.daysUntilDue() }
        addTitle(container, "گیاهان من", "فهرست گیاهانی که به آن‌ها رسیدگی می‌کنی")
        container.addView(makeTextButton("＋  ثبت گیاه جدید", 14f, WHITE, GREEN, -1, 48).apply {
            setOnClickListener { showAddPlantPicker() }
        }, matchWrap().withBottom(16))
        if (plants.isEmpty()) {
            val empty = card(WHITE)
            empty.addView(text("باغچه‌ات هنوز خالیه 🌱", 16f, DARK_GREEN, true))
            addSpace(empty, 8)
            empty.addView(text("از راهنمای گیاهان یک مورد انتخاب کن یا گیاه دلخواهت رو دستی ثبت کن.", 13f, MUTED))
            container.addView(empty, matchWrap())
        } else {
            plants.forEach { container.addView(plantRow(it), matchWrap().withBottom(10)) }
        }
    }

    private fun buildGuide(container: LinearLayout) {
        addTitle(container, "راهنمای گیاهان", "برای شروع، یکی از گیاهان محبوب آپارتمانی را انتخاب کن")
        PlantCatalog.items.forEach { guide ->
            val item = card(WHITE)
            val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
            heading.addView(text(guide.emoji, 29f, DARK_GREEN), LinearLayout.LayoutParams(dp(48), dp(48)))
            val titleGroup = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            titleGroup.addView(text(guide.name, 16f, DARK_GREEN, true))
            titleGroup.addView(text("فاصله پیشنهادی بررسی آب: هر ${toPersian(guide.intervalDays)} روز", 12f, MUTED))
            heading.addView(titleGroup, LinearLayout.LayoutParams(0, WRAP, 1f))
            item.addView(heading)
            addSpace(item, 9)
            item.addView(text("☀  نور: ${guide.light}", 13f, TEXT))
            addSpace(item, 4)
            item.addView(text("💧  آبیاری: ${guide.water}", 13f, TEXT))
            addSpace(item, 7)
            item.addView(text(guide.tip, 12f, MUTED))
            addSpace(item, 12)
            item.addView(makeTextButton("＋ افزودن ${guide.name} به باغچه", 13f, WHITE, GREEN, -1, 42).apply {
                setOnClickListener { showAddPlantDialog(guide) }
            })
            container.addView(item, matchWrap().withBottom(11))
        }
        container.addView(text("توجه: این فاصله‌ها فقط نقطه شروع هستند. قبل از آبیاری، خاک و شرایط واقعی گیاه را بررسی کن.", 12f, MUTED), matchWrap().withTop(4))
    }

    private fun buildSettings(container: LinearLayout) {
        addTitle(container, "تنظیمات", "مدیریت یادآوری‌ها و اطلاعات برنامه")
        val reminders = card(WHITE)
        reminders.addView(text("یادآوری آبیاری", 16f, DARK_GREEN, true))
        addSpace(reminders, 8)
        val switchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }
        switchRow.addView(text("فعال بودن اعلان‌ها", 14f, TEXT), LinearLayout.LayoutParams(0, WRAP, 1f))
        val toggle = Switch(this).apply {
            isChecked = ReminderScheduler.areEnabled(this@MainActivity)
            text = ""
            setOnCheckedChangeListener { _, checked ->
                ReminderScheduler.setEnabled(this@MainActivity, checked)
                if (checked) requestNotificationPermissionIfNeeded()
            }
        }
        switchRow.addView(toggle)
        reminders.addView(switchRow)
        addSpace(reminders, 12)
        val (hour, minute) = ReminderScheduler.getTime(this)
        reminders.addView(makeTextButton("⏰  ساعت یادآوری: ${twoDigits(hour)}:${twoDigits(minute)}", 13f, GREEN, LIGHT_GREEN, -1, 46).apply {
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, h, m ->
                    ReminderScheduler.setTime(this@MainActivity, h, m)
                    render()
                }, hour, minute, true).show()
            }
        })
        container.addView(reminders, matchWrap().withBottom(12))

        val about = card(WHITE)
        about.addView(text("درباره سبزینه", 16f, DARK_GREEN, true))
        addSpace(about, 8)
        about.addView(text("سبزینه یک دستیار ساده و آفلاین برای ثبت گیاهان و یادآوری زمان بررسی آبیاری است.", 13f, MUTED))
        addSpace(about, 8)
        about.addView(text("اطلاعات گیاهان روی همین دستگاه ذخیره می‌شود و در این نسخه به سرور ارسال نمی‌شود.", 13f, MUTED))
        addSpace(about, 8)
        about.addView(text("نسخه ۱.۰.۰", 12f, MUTED))
        container.addView(about, matchWrap())
    }

    private fun buildPlantDetail(container: LinearLayout) {
        val plant = selectedPlantId?.let { repository.find(it) }
        if (plant == null) {
            currentPage = Page.PLANTS
            buildPlants(container)
            return
        }
        val back = makeTextButton("←  بازگشت به گیاهان", 13f, GREEN, LIGHT_GREEN, -1, 42).apply {
            setOnClickListener { currentPage = Page.PLANTS; render() }
        }
        container.addView(back, matchWrap().withBottom(14))

        val hero = card(LIGHT_GREEN)
        hero.gravity = Gravity.CENTER_HORIZONTAL
        hero.addView(text(plant.emoji, 48f, DARK_GREEN).apply { gravity = Gravity.CENTER })
        addSpace(hero, 7)
        hero.addView(text(plant.name, 22f, DARK_GREEN, true).apply { gravity = Gravity.CENTER })
        hero.addView(text(plant.species, 13f, MUTED).apply { gravity = Gravity.CENTER })
        addSpace(hero, 10)
        hero.addView(statusPill(plant))
        container.addView(hero, matchWrap().withBottom(14))

        val details = card(WHITE)
        details.addView(detailLine("📍  محل نگهداری", plant.location))
        details.addView(detailLine("💧  فاصله بررسی آبیاری", "هر ${toPersian(plant.wateringIntervalDays)} روز"))
        details.addView(detailLine("🗓  وضعیت", dueLabel(plant)))
        details.addView(detailLine("📝  یادداشت", plant.notes.ifBlank { "یادداشتی ثبت نشده" }))
        container.addView(details, matchWrap().withBottom(12))

        container.addView(makeTextButton("💧  آبیاری را انجام دادم", 14f, WHITE, GREEN, -1, 50).apply {
            setOnClickListener {
                val updated = plant.copy(lastWateredAt = System.currentTimeMillis())
                repository.save(updated)
                ReminderScheduler.schedule(this@MainActivity, updated)
                Toast.makeText(this@MainActivity, "زمان آبیاری ${plant.name} ثبت شد", Toast.LENGTH_SHORT).show()
                render()
            }
        }, matchWrap().withBottom(9))
        container.addView(makeTextButton("ویرایش اطلاعات", 13f, GREEN, LIGHT_GREEN, -1, 46).apply {
            setOnClickListener { showAddPlantDialog(existing = plant) }
        }, matchWrap().withBottom(9))
        container.addView(makeTextButton("حذف از باغچه", 13f, RED, RED_LIGHT, -1, 46).apply {
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("حذف ${plant.name}؟")
                    .setMessage("این گیاه از فهرست تو حذف می‌شود.")
                    .setNegativeButton("انصراف", null)
                    .setPositiveButton("حذف") { _, _ ->
                        repository.delete(plant.id)
                        ReminderScheduler.cancel(this@MainActivity, plant.id)
                        currentPage = Page.PLANTS
                        selectedPlantId = null
                        render()
                    }.show()
            }
        }, matchWrap())
    }

    private fun detailLine(label: String, value: String): View {
        val line = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(5), 0, dp(7)) }
        line.addView(text(label, 12f, MUTED))
        line.addView(text(value, 14f, DARK_GREEN, true))
        return line
    }

    private fun plantRow(plant: Plant): View {
        val item = card(WHITE).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedPlantId = plant.id
                currentPage = Page.DETAIL
                render()
            }
        }
        val emoji = TextView(this).apply {
            text = plant.emoji
            textSize = 28f
            gravity = Gravity.CENTER
            background = rounded(LIGHT_GREEN, 14)
        }
        item.addView(emoji, LinearLayout.LayoutParams(dp(54), dp(60)).apply { setMargins(0, 0, dp(11), 0) })
        val textGroup = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textGroup.addView(text(plant.name, 15f, DARK_GREEN, true))
        textGroup.addView(text("${plant.location} • هر ${toPersian(plant.wateringIntervalDays)} روز", 11f, MUTED))
        addSpace(textGroup, 5)
        textGroup.addView(text(dueLabel(plant), 12f, if (plant.isDue()) ORANGE_DARK else GREEN, true))
        item.addView(textGroup, LinearLayout.LayoutParams(0, WRAP, 1f))
        item.addView(text("›", 25f, MUTED))
        return item
    }

    private fun statCard(emoji: String, value: String, label: String): View {
        val box = card(WHITE)
        box.addView(text(emoji, 21f, DARK_GREEN))
        addSpace(box, 4)
        box.addView(text(value, 22f, DARK_GREEN, true))
        box.addView(text(label, 11f, MUTED))
        return box
    }

    private fun statusPill(plant: Plant): View {
        val due = plant.isDue()
        val label = if (due) "نیاز به بررسی آب" else dueLabel(plant)
        return makeTextButton(label, 12f, if (due) ORANGE_DARK else GREEN, if (due) ORANGE_LIGHT else WHITE, -1, 36)
    }

    private fun dueLabel(plant: Plant): String {
        val days = plant.daysUntilDue()
        return when {
            days < 0 -> "${toPersian(-days)} روز از زمان پیشنهادی گذشته"
            days == 0 -> "امروز زمان بررسی خاک است"
            days == 1 -> "فردا زمان بررسی خاک است"
            else -> "${toPersian(days)} روز تا بررسی بعدی"
        }
    }

    private fun showAddPlantPicker() {
        val names = PlantCatalog.items.map { "${it.emoji}  ${it.name}" }.toMutableList().apply { add("✏️  گیاه دلخواه") }
        AlertDialog.Builder(this)
            .setTitle("چه گیاهی می‌خواهی اضافه کنی؟")
            .setItems(names.toTypedArray()) { _, which ->
                val guide = PlantCatalog.items.getOrNull(which)
                showAddPlantDialog(guide)
            }
            .setNegativeButton("انصراف", null)
            .show()
    }

    private fun showAddPlantDialog(guide: PlantGuide? = null, existing: Plant? = null) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(7), dp(22), dp(4))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        form.addView(formLabel("نام گیاه"))
        val nameInput = formInput(existing?.name ?: guide?.name.orEmpty(), "مثلاً پتوس")
        form.addView(nameInput, matchWrap().withBottom(9))
        form.addView(formLabel("محل نگهداری"))
        val locationInput = formInput(existing?.location ?: "اتاق نشیمن", "مثلاً کنار پنجره")
        form.addView(locationInput, matchWrap().withBottom(9))
        form.addView(formLabel("فاصله پیشنهادی بررسی آبیاری (روز)"))
        val intervalInput = formInput((existing?.wateringIntervalDays ?: guide?.intervalDays ?: 7).toString(), "مثلاً ۷", true)
        form.addView(intervalInput, matchWrap().withBottom(9))
        form.addView(text("این عدد یادآور بررسی خاک است، نه دستور آبیاری قطعی. شرایط گیاه را قبل از آب دادن بررسی کن.", 11f, MUTED))

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "افزودن گیاه" else "ویرایش گیاه")
            .setView(form)
            .setNegativeButton("انصراف", null)
            .setPositiveButton(if (existing == null) "ثبت گیاه" else "ذخیره تغییرات", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val location = locationInput.text.toString().trim().ifBlank { "نامشخص" }
                val interval = intervalInput.text.toString().trim().toIntOrNull()
                if (name.isBlank()) {
                    nameInput.error = "نام گیاه را وارد کن"
                    return@setOnClickListener
                }
                if (interval == null || interval !in 1..90) {
                    intervalInput.error = "عددی بین ۱ تا ۹۰ وارد کن"
                    return@setOnClickListener
                }
                val guideEmoji = guide?.emoji ?: existing?.emoji ?: "🪴"
                val newPlant = Plant(
                    id = existing?.id ?: System.currentTimeMillis(),
                    name = name,
                    species = guide?.name ?: existing?.species ?: "گیاه آپارتمانی",
                    location = location,
                    wateringIntervalDays = interval,
                    lastWateredAt = existing?.lastWateredAt ?: System.currentTimeMillis(),
                    emoji = guideEmoji,
                    notes = existing?.notes.orEmpty()
                )
                repository.save(newPlant)
                ReminderScheduler.schedule(this, newPlant)
                dialog.dismiss()
                selectedPlantId = newPlant.id
                currentPage = Page.DETAIL
                render()
            }
        }
        dialog.show()
    }

    private fun formLabel(value: String) = text(value, 12f, DARK_GREEN, true).apply {
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun formInput(value: String, hint: String, numeric: Boolean = false): EditText {
        return EditText(this).apply {
            setText(value)
            this.hint = hint
            textSize = 14f
            setTextColor(TEXT)
            setHintTextColor(MUTED)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setSingleLine(true)
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(WHITE, 12, BORDER)
            if (numeric) inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun addTitle(container: LinearLayout, title: String, subtitle: String) {
        container.addView(text(title, 22f, DARK_GREEN, true), matchWrap())
        container.addView(text(subtitle, 13f, MUTED), matchWrap().withTop(5).withBottom(17))
    }

    private fun addSectionTitle(container: LinearLayout, title: String, subtitle: String) {
        container.addView(text(title, 17f, DARK_GREEN, true), matchWrap())
        container.addView(text(subtitle, 12f, MUTED), matchWrap().withTop(3).withBottom(11))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            setLineSpacing(dp(2).toFloat(), 1f)
        }
    }

    private fun makeTextButton(
        label: String,
        size: Float,
        textColor: Int,
        fillColor: Int,
        width: Int,
        height: Int
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = size
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(5), dp(13), dp(5))
            background = rounded(fillColor, 14)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(if (width < 0) MATCH else dp(width), if (height < 0) WRAP else dp(height))
        }
    }

    private fun card(color: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        background = rounded(color, 18, if (color == WHITE) BORDER else color)
        elevation = dp(1).toFloat()
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun rounded(fill: Int, radius: Int, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null && stroke != fill) setStroke(dp(1), stroke)
    }

    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun LinearLayout.LayoutParams.withBottom(value: Int): LinearLayout.LayoutParams = apply { bottomMargin = dp(value) }
    private fun LinearLayout.LayoutParams.withTop(value: Int): LinearLayout.LayoutParams = apply { topMargin = dp(value) }
    private fun matchWrap() = LinearLayout.LayoutParams(MATCH, WRAP)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun twoDigits(value: Int) = value.toString().padStart(2, '0')
    private fun toPersian(value: Int): String = value.toString().map { char -> "۰۱۲۳۴۵۶۷۸۹"[char.digitToIntOrNull() ?: 0] }.joinToString("")

    private enum class Page { HOME, PLANTS, GUIDE, SETTINGS, DETAIL }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 104
        private const val MATCH = -1
        private const val WRAP = -2
        private const val BG = 0xFFF5F7F2.toInt()
        private const val WHITE = -1
        private const val DARK_GREEN = 0xFF214B36.toInt()
        private const val GREEN = 0xFF367653.toInt()
        private const val LIGHT_GREEN = 0xFFE8F2E9.toInt()
        private const val TEXT = 0xFF33473A.toInt()
        private const val MUTED = 0xFF78877D.toInt()
        private const val BORDER = 0xFFE5EBE4.toInt()
        private const val ORANGE_DARK = 0xFF9A5B16.toInt()
        private const val ORANGE_LIGHT = 0xFFFFF1DC.toInt()
        private const val RED = 0xFF9B4747.toInt()
        private const val RED_LIGHT = 0xFFFBECEC.toInt()
    }
}
