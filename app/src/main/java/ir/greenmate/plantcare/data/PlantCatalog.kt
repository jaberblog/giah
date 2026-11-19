package ir.greenmate.plantcare.data

data class PlantGuide(
    val name: String,
    val emoji: String,
    val intervalDays: Int,
    val light: String,
    val water: String,
    val tip: String
)

object PlantCatalog {
    val items = listOf(
        PlantGuide(
            name = "پتوس",
            emoji = "🌿",
            intervalDays = 7,
            light = "نور غیرمستقیم و متوسط",
            water = "وقتی چند سانتی‌متر بالای خاک خشک شد",
            tip = "پتوس با نور متوسط سازگار است. از ماندن آب در زیرگلدانی جلوگیری کن."
        ),
        PlantGuide(
            name = "سانسوریا",
            emoji = "🪴",
            intervalDays = 14,
            light = "نور کم تا غیرمستقیم روشن",
            water = "پس از خشک شدن کامل خاک",
            tip = "آبیاری زیاد برای سانسوریا مناسب نیست؛ زهکشی گلدان را بررسی کن."
        ),
        PlantGuide(
            name = "مونسترا",
            emoji = "🍃",
            intervalDays = 7,
            light = "نور زیاد ولی فیلترشده",
            water = "پس از خشک شدن بخش بالایی خاک",
            tip = "برگ‌ها را هر چند وقت یک‌بار با دستمال نرم تمیز کن و فضای کافی بده."
        ),
        PlantGuide(
            name = "زاموفیلیا",
            emoji = "🌱",
            intervalDays = 14,
            light = "نور کم تا متوسط و غیرمستقیم",
            water = "وقتی خاک تقریباً خشک شد",
            tip = "گیاه کم‌توقعی است و آبیاری بیش از حد می‌تواند به ریشه آسیب بزند."
        ),
        PlantGuide(
            name = "اسپاتی‌فیلوم",
            emoji = "🌸",
            intervalDays = 5,
            light = "نور غیرمستقیم متوسط",
            water = "پیش از خشک شدن کامل خاک",
            tip = "رطوبت ملایم را دوست دارد؛ از نور مستقیم و تند دور نگهش دار."
        ),
        PlantGuide(
            name = "کاکتوس",
            emoji = "🌵",
            intervalDays = 21,
            light = "نور زیاد و روشن",
            water = "پس از خشک شدن کامل خاک",
            tip = "در فصل سرد معمولاً به آب کمتری نیاز دارد و خاک باید زهکشی خوبی داشته باشد."
        )
    )
}
