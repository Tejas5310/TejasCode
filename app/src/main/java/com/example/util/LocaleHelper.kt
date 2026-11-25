package com.example.util

import androidx.compose.runtime.staticCompositionLocalOf

data class LocalizedLabels(
    val navCourses: String,
    val navEditor: String,
    val navTutor: String,
    val navQuizzes: String,
    val navCommunity: String,
    val navProfile: String,
    val welcomeBack: String,
    val keepCoding: String,
    val upgradePro: String,
    val popularCourses: String,
    val continueLearning: String,
    val settingsTitle: String,
    val darkModeLabel: String,
    val languageLabel: String
)

object AppLocaleStrings {
    fun forCode(code: String): LocalizedLabels = when (code.lowercase()) {
        "hi" -> LocalizedLabels(
            navCourses = "कोर्स",
            navEditor = "एडिटर",
            navTutor = "AI ट्यूटर",
            navQuizzes = "क्विज़",
            navCommunity = "कम्युनिटी",
            navProfile = "प्रोफ़ाइल",
            welcomeBack = "वापसी पर स्वागत है,",
            keepCoding = "सीखते रहें, कोडिंग करते रहें!",
            upgradePro = "TejasCode Pro में अपग्रेड करें",
            popularCourses = "लोकप्रिय कोर्स",
            continueLearning = "सीखना जारी रखें",
            settingsTitle = "सेटिंग्स और सहायता",
            darkModeLabel = "डार्क नेवी थीम",
            languageLabel = "ऐप भाषा"
        )
        "gu" -> LocalizedLabels(
            navCourses = "કોર્સ",
            navEditor = "એડિટર",
            navTutor = "AI ટ્યુટર",
            navQuizzes = "ક્વિઝ",
            navCommunity = "સમુદાય",
            navProfile = "પ્રોફાઇલ",
            welcomeBack = "ફરી સ્વાગત છે,",
            keepCoding = "શીખતા રહો, કોડિંગ કરતા રહો!",
            upgradePro = "TejasCode Pro પર અપગ્રેડ કરો",
            popularCourses = "લોકપ્રિય કોર્સ",
            continueLearning = "શીખવાનું ચાલુ રાખો",
            settingsTitle = "સેટિંગ્સ અને સહાય",
            darkModeLabel = "ડાર્ક નેવી થીમ",
            languageLabel = "એપ ભાષા"
        )
        "es" -> LocalizedLabels(
            navCourses = "Cursos",
            navEditor = "Editor",
            navTutor = "Tutor IA",
            navQuizzes = "Retos",
            navCommunity = "Comunidad",
            navProfile = "Perfil",
            welcomeBack = "Bienvenido de nuevo,",
            keepCoding = "¡Sigue aprendiendo y programando!",
            upgradePro = "Mejorar a TejasCode Pro",
            popularCourses = "Cursos Populares",
            continueLearning = "Continuar Aprendiendo",
            settingsTitle = "Ajustes y Legal",
            darkModeLabel = "Tema Oscuro Navy",
            languageLabel = "Idioma de la app"
        )
        "fr" -> LocalizedLabels(
            navCourses = "Cours",
            navEditor = "Éditeur",
            navTutor = "Tuteur IA",
            navQuizzes = "Quiz",
            navCommunity = "Communauté",
            navProfile = "Profil",
            welcomeBack = "Bon retour,",
            keepCoding = "Continuez à apprendre et coder !",
            upgradePro = "Passer à TejasCode Pro",
            popularCourses = "Cours Populaires",
            continueLearning = "Continuer l'apprentissage",
            settingsTitle = "Paramètres et Légal",
            darkModeLabel = "Thème Sombre Navy",
            languageLabel = "Langue de l'application"
        )
        "de" -> LocalizedLabels(
            navCourses = "Kurse",
            navEditor = "Editor",
            navTutor = "KI-Tutor",
            navQuizzes = "Quiz",
            navCommunity = "Community",
            navProfile = "Profil",
            welcomeBack = "Willkommen zurück,",
            keepCoding = "Weiter lernen, weiter coden!",
            upgradePro = "Auf TejasCode Pro upgraden",
            popularCourses = "Beliebte Kurse",
            continueLearning = "Weiterlernen",
            settingsTitle = "Einstellungen & Recht",
            darkModeLabel = "Dunkles Navy-Design",
            languageLabel = "App-Sprache"
        )
        "pt" -> LocalizedLabels(
            navCourses = "Cursos",
            navEditor = "Editor",
            navTutor = "Tutor IA",
            navQuizzes = "Quizzes",
            navCommunity = "Comunidade",
            navProfile = "Perfil",
            welcomeBack = "Bem-vindo de volta,",
            keepCoding = "Continue aprendendo e programando!",
            upgradePro = "Assinar TejasCode Pro",
            popularCourses = "Cursos Populares",
            continueLearning = "Continuar Aprendendo",
            settingsTitle = "Configurações e Legal",
            darkModeLabel = "Tema Escuro Navy",
            languageLabel = "Idioma do app"
        )
        "ar" -> LocalizedLabels(
            navCourses = "الدورات",
            navEditor = "المحرر",
            navTutor = "المرشد الذكي",
            navQuizzes = "الاختبارات",
            navCommunity = "المجتمع",
            navProfile = "الملف",
            welcomeBack = "مرحبًا بعودتك،",
            keepCoding = "واصل التعلم والبرمجة!",
            upgradePro = "الترقية إلى TejasCode Pro",
            popularCourses = "الدورات الشائعة",
            continueLearning = "متابعة التعلم",
            settingsTitle = "الإعدادات والدعم",
            darkModeLabel = "السمة الداكنة",
            languageLabel = "لغة التطبيق"
        )
        "ja" -> LocalizedLabels(
            navCourses = "コース",
            navEditor = "エディタ",
            navTutor = "AI講師",
            navQuizzes = "クイズ",
            navCommunity = "交流",
            navProfile = "プロフィール",
            welcomeBack = "おかえりなさい、",
            keepCoding = "学び続けよう、コードを書き続けよう！",
            upgradePro = "TejasCode Pro にアップグレード",
            popularCourses = "人気のコース",
            continueLearning = "学習を続ける",
            settingsTitle = "設定・サポート",
            darkModeLabel = "ダークネイビーテーマ",
            languageLabel = "表示言語"
        )
        "zh" -> LocalizedLabels(
            navCourses = "课程",
            navEditor = "编辑器",
            navTutor = "AI导师",
            navQuizzes = "测验",
            navCommunity = "社区",
            navProfile = "我的",
            welcomeBack = "欢迎回来，",
            keepCoding = "坚持学习，持续编程！",
            upgradePro = "升级到 TejasCode Pro",
            popularCourses = "热门课程",
            continueLearning = "继续学习",
            settingsTitle = "设置与条款",
            darkModeLabel = "深蓝夜间主题",
            languageLabel = "应用语言"
        )
        else -> LocalizedLabels(
            navCourses = "Courses",
            navEditor = "Editor",
            navTutor = "AI Tutor",
            navQuizzes = "Quizzes",
            navCommunity = "Community",
            navProfile = "Profile",
            welcomeBack = "Welcome Back,",
            keepCoding = "Keep learning, keep coding!",
            upgradePro = "Upgrade to TejasCode Pro",
            popularCourses = "Popular Courses",
            continueLearning = "Continue Learning",
            settingsTitle = "Settings & Legal",
            darkModeLabel = "Dark Navy Theme",
            languageLabel = "App Language"
        )
    }
}

val LocalAppLabels = staticCompositionLocalOf { AppLocaleStrings.forCode("en") }
