package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

// --- Firestore Persisted Models ---

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Student",
    val photoUrl: String? = null,
    val bio: String = "",
    val learningGoal: String = "Full-Stack Web",
    val experienceLevel: String = "Beginner",
    val preferredLanguage: String = "en",
    val registered: Boolean = false,
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val lessonsCompleted: Long = 0L,
    val totalCodingMinutes: Long = 0L,
    val battlesWon: Long = 0L,
    val lastActiveDate: String = "",
    val lastLoginAt: String = "",
    val aiQueriesUsedToday: Long = 0L,
    val aiQuotaDate: String = "",
    val completedLessonIds: List<String> = emptyList(),
    val savedCourseIds: List<String> = emptyList(),
    val badges: List<String> = emptyList(),
    val followingIds: List<String> = emptyList(),
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ManagedCourse(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val language: String = "HTML",
    val category: String = "WEB",
    val difficulty: String = "Beginner",
    val durationMinutes: Long = 5L,
    val xpReward: Long = 100L,
    @get:PropertyName("isPremium")
    @PropertyName("isPremium")
    val isPremium: Boolean = false,
    val resourceUrl: String = "",
    val starterCode: String = "",
    val lessonSummary: String = "",
    val quizQuestion: String = "",
    val quizOptions: List<String> = listOf("Option A", "Option B"),
    val correctOptionIndex: Long = 0L,
    val visibility: String = "public",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CodeSnippet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Snippet",
    val language: String = "Python",
    val code: String = "",
    val lastOutput: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Certificate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val recipientName: String = "",
    val verificationCode: String = "",
    @ServerTimestamp val earnedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val xp: Long = 0L,
    val streakDays: Long = 1L,
    val battlesWon: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityProject(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val language: String = "Python",
    val code: String = "",
    val description: String = "",
    val likesCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityQuestion(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val title: String = "",
    val body: String = "",
    val language: String = "Python",
    val codeSnippet: String = "",
    val likesCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val replyCount: Long = 0L,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuestionReply(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val body: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val status: String = "open",
    val resolvedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetTier: String = "all",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

// --- Billing & Entitlement Models ---

enum class SubscriptionStatus(val label: String) {
    Active("Active"),
    Trialing("Trialing"),
    GracePeriod("Grace Period"),
    Paused("Paused"),
    Cancelled("Cancelled"),
    Expired("Expired"),
    Refunded("Refunded"),
    Revoked("Revoked"),
    Pending("Pending"),
    Free("Free");

    companion object {
        fun fromRaw(raw: String?): SubscriptionStatus {
            if (raw.isNullOrBlank()) return Free
            return entries.firstOrNull {
                it.name.equals(raw.replace(" ", ""), ignoreCase = true) ||
                    it.label.equals(raw, ignoreCase = true)
            } ?: Free
        }
    }
}

@Keep
@IgnoreExtraProperties
data class PlanConfig(
    @DocumentId val id: String = "active",
    val monthlyPriceMinor: Long = 0L,
    val yearlyPriceMinor: Long = 0L,
    val currency: String = "INR",
    val currencySymbol: String = "₹",
    val trialDays: Long = 0L,
    val monthlyPlayProductId: String = "tejascode_pro_monthly",
    val monthlyPlayBasePlanId: String = "monthly-base",
    val yearlyPlayProductId: String = "tejascode_pro_yearly",
    val yearlyPlayBasePlanId: String = "yearly-base",
    val razorpayMonthlyPlanId: String = "",
    val razorpayYearlyPlanId: String = "",
    val razorpayEnabled: Boolean = false,
    val freeDailyAiLimit: Long = 5L,
    val proDailyAiLimit: Long = 100L,
    val billingTermsSummary: String = "Subscriptions renew automatically unless cancelled at least 24 hours before the current period ends. Access continues until the paid period expires.",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isConfigured: Boolean
        get() = monthlyPriceMinor > 0L && yearlyPriceMinor > 0L

    fun formattedMonthlyPrice(): String = formatMinorAmount(monthlyPriceMinor, currencySymbol)
    fun formattedYearlyPrice(): String = formatMinorAmount(yearlyPriceMinor, currencySymbol)

    companion object {
        fun formatMinorAmount(minorUnits: Long, symbol: String): String {
            if (minorUnits <= 0L) return "Not Configured"
            val major = minorUnits / 100
            val remainder = minorUnits % 100
            return if (remainder == 0L) "$symbol$major" else "$symbol$major.${remainder.toString().padStart(2, '0')}"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserEntitlement(
    @DocumentId val id: String = "current",
    val userId: String = "",
    val tier: String = "free",
    val status: String = "Free",
    val provider: String = "none",
    val billingInterval: String = "none",
    val activeSubscriptionId: String? = null,
    val periodEndEpochMs: Long? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val lastVerifiedAtEpochMs: Long? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subscriptionStatus: SubscriptionStatus
        get() = SubscriptionStatus.fromRaw(status)

    val isProActive: Boolean
        get() {
            if (!tier.equals("pro", ignoreCase = true)) return false
            val st = subscriptionStatus
            val activeStatus = st == SubscriptionStatus.Active ||
                st == SubscriptionStatus.Trialing ||
                st == SubscriptionStatus.GracePeriod ||
                (st == SubscriptionStatus.Cancelled && (periodEndEpochMs == null || periodEndEpochMs > System.currentTimeMillis()))
            if (!activeStatus) return false
            return periodEndEpochMs == null || periodEndEpochMs > System.currentTimeMillis()
        }
}

@Keep
@IgnoreExtraProperties
data class SubscriptionRecord(
    @DocumentId val id: String = "",
    val subscriptionId: String = "",
    val userId: String = "",
    val provider: String = "google_play",
    val status: String = "Pending",
    val billingInterval: String = "monthly",
    val amountMinorUnits: Long = 0L,
    val currency: String = "INR",
    val productOrPlanId: String = "",
    val currentPeriodStartMs: Long = 0L,
    val currentPeriodEndMs: Long = 0L,
    val cancelAtPeriodEnd: Boolean = false,
    val cancellationReason: String? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val subscriptionStatus: SubscriptionStatus
        get() = SubscriptionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class PaymentTransaction(
    @DocumentId val id: String = "",
    val transactionId: String = "",
    val subscriptionId: String = "",
    val userId: String = "",
    val provider: String = "google_play",
    val billingInterval: String = "monthly",
    val grossAmountMinor: Long = 0L,
    val gatewayFeeMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val netAmountMinor: Long = 0L,
    val currency: String = "INR",
    val status: String = "captured",
    val paymentMethodSummary: String = "",
    val invoiceRef: String = "",
    val createdAtMs: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class SettlementRecord(
    @DocumentId val id: String = "",
    val settlementId: String = "",
    val provider: String = "razorpay",
    val grossAmountMinor: Long = 0L,
    val gatewayFeeMinor: Long = 0L,
    val gstOnFeeMinor: Long = 0L,
    val netPayoutMinor: Long = 0L,
    val currency: String = "INR",
    val status: String = "pending",
    val utrNumber: String? = null,
    val bankAccountMasked: String = "",
    val settledAtMs: Long? = null
)

// --- Domain Curriculum & Interactive Models ---

enum class CareerCategory(val label: String) {
    ALL("All"),
    WEB("Web (HTML/CSS/JS/TS/PHP)"),
    DATA_AI("Python & SQL"),
    SOFTWARE_MOBILE("Java/Kotlin/Swift/C/C++/C#"),
    ESSENTIALS("Systems (Go/Rust)")
}

enum class ProgrammingLanguage(
    val displayName: String,
    val fileExt: String,
    val starterCode: String
) {
    PYTHON(
        displayName = "Python",
        fileExt = "main.py",
        starterCode = """# TejasCode Python 3 Sandbox
def greet_learners(skills):
    for i, skill in enumerate(skills, 1):
        print(f"{i}. Mastering {skill}")

tracks = ["Python", "JavaScript", "Kotlin", "Go"]
greet_learners(tracks)
print("Total tracks:", len(tracks))"""
    ),
    JAVA(
        displayName = "Java",
        fileExt = "Main.java",
        starterCode = """public class Main {
    public static void main(String[] args) {
        int lessons = 12;
        int xpPerLesson = 100;
        System.out.println("TejasCode Java XP: " + (lessons * xpPerLesson));
    }
}"""
    ),
    JAVASCRIPT(
        displayName = "JavaScript",
        fileExt = "index.js",
        starterCode = """// TejasCode JavaScript Sandbox
const scores = [88, 94, 79, 99];
const topScore = Math.max(...scores);
const avg = scores.reduce((a, b) => a + b, 0) / scores.length;

console.log("Top Score:", topScore);
console.log("Average:", avg);"""
    ),
    TYPESCRIPT(
        displayName = "TypeScript",
        fileExt = "app.ts",
        starterCode = """interface Student {
    name: string;
    xp: number;
}
const coder: Student = { name: "Tejas", xp: 1450 };
console.log("Student:", coder.name, "XP:", coder.xp);"""
    ),
    HTML(
        displayName = "HTML",
        fileExt = "index.html",
        starterCode = """<div class="card">
  <h1>Welcome to TejasCode!</h1>
  <p>Learn, Code, and Grow across 15 languages.</p>
  <button>Start Coding</button>
</div>"""
    ),
    CSS(
        displayName = "CSS",
        fileExt = "styles.css",
        starterCode = """.tejas-card {
  display: flex;
  justify-content: space-between;
  background: linear-gradient(90deg, #00D2FF, #A855F7);
  border-radius: 18px;
  padding: 20px;
}"""
    ),
    SQL(
        displayName = "SQL",
        fileExt = "query.sql",
        starterCode = """-- Query the built-in developers table
SELECT name, track, xp
FROM developers
WHERE xp >= 1000
ORDER BY xp DESC;"""
    ),
    KOTLIN(
        displayName = "Kotlin",
        fileExt = "Main.kt",
        starterCode = """// TejasCode Kotlin Sandbox
fun main() {
    val languages = listOf("Kotlin", "Python", "TypeScript", "Rust")
    println("Selected tracks: ${'$'}languages")
    val xp = 150 + 250
    println("Total XP earned: ${'$'}xp")
}"""
    ),
    SWIFT(
        displayName = "Swift",
        fileExt = "Main.swift",
        starterCode = """let streak = 14
let bonus = 50
print("Swift Streak XP:", streak * bonus)"""
    ),
    C(
        displayName = "C",
        fileExt = "main.c",
        starterCode = """#include <stdio.h>

int main() {
    int level = 5;
    printf("TejasCode C Level: %d\n", level);
    return 0;
}"""
    ),
    CPP(
        displayName = "C++",
        fileExt = "main.cpp",
        starterCode = """#include <iostream>
using namespace std;

int main() {
    int n = 5;
    int sum = n * (n + 1) / 2;
    cout << "Sum 1..5 = " << sum << endl;
    return 0;
}"""
    ),
    CSHARP(
        displayName = "C#",
        fileExt = "Program.cs",
        starterCode = """using System;
class Program {
    static void Main() {
        int modules = 8;
        Console.WriteLine("C# Modules Completed: " + modules);
    }
}"""
    ),
    PHP(
        displayName = "PHP",
        fileExt = "index.php",
        starterCode = """<?php
${'$'}platform = "TejasCode";
${'$'}courses = 15;
echo "Welcome to ${'$'}platform with ${'$'}courses languages!";
?>"""
    ),
    GO(
        displayName = "Go",
        fileExt = "main.go",
        starterCode = """package main
import "fmt"

func main() {
    workers := 4
    tasks := 12
    fmt.Println("Workers:", workers, "Tasks/worker:", tasks/workers)
}"""
    ),
    RUST(
        displayName = "Rust",
        fileExt = "main.rs",
        starterCode = """fn main() {
    let level = 7;
    let multiplier = 50;
    println!("Rustacean Power: {}", level * multiplier);
}"""
    )
}

data class LessonStep(
    val id: String,
    val title: String,
    val explanation: String,
    val codeSnippet: String? = null,
    val languageLabel: String = "",
    val question: String? = null,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = 0,
    val feedbackCorrect: String = "Spot on! You nailed that concept.",
    val feedbackHint: String = "Look closely at the syntax and execution order."
)

data class CodingLesson(
    val id: String,
    val trackId: String,
    val title: String,
    val durationMinutes: Int,
    val xpReward: Int,
    val summary: String,
    val steps: List<LessonStep>,
    val isPremium: Boolean = false,
    val resourceUrl: String = ""
)

data class CareerTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: CareerCategory,
    val languages: List<String>,
    val difficulty: String,
    val accentHex: Long,
    val lessons: List<CodingLesson>,
    val isPremium: Boolean = false
)

data class CodingBattleQuestion(
    val id: String,
    val language: String,
    val prompt: String,
    val codeSnippet: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class CodingChallenge(
    val id: String,
    val title: String,
    val difficulty: String,
    val language: ProgrammingLanguage,
    val prompt: String,
    val starterCode: String,
    val expectedHint: String,
    val xpReward: Int
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
