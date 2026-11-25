package com.example.data

import com.example.model.CareerCategory
import com.example.model.CareerTrack
import com.example.model.CodingBattleQuestion
import com.example.model.CodingChallenge
import com.example.model.CodingLesson
import com.example.model.LessonStep
import com.example.model.ManagedCourse
import com.example.model.ProgrammingLanguage

object CurriculumRepository {

    val allSupportedLanguages: List<String> = listOf(
        "Python", "Java", "JavaScript", "C", "C++", "C#", "PHP",
        "HTML", "CSS", "SQL", "Kotlin", "Swift", "Go", "Rust", "TypeScript"
    )

    val tracks: List<CareerTrack> = listOf(
        CareerTrack(
            id = "html_css_basics",
            title = "HTML & CSS Web Foundations",
            subtitle = "HTML5 Semantics, CSS Styling, Flexbox & Responsive Grids",
            category = CareerCategory.WEB,
            languages = listOf("HTML", "CSS"),
            difficulty = "Beginner",
            accentHex = 0xFF00D2FF,
            lessons = listOf(
                CodingLesson(
                    id = "html_1",
                    trackId = "html_css_basics",
                    title = "HTML5 Semantics & Page Structure",
                    durationMinutes = 5,
                    xpReward = 80,
                    summary = "Build accessible web pages with semantic HTML5 tags: header, main, section, and links.",
                    steps = listOf(
                        LessonStep(
                            id = "html_1_s1",
                            title = "HTML5 Document Skeleton",
                            explanation = "Every web page starts with <!DOCTYPE html> and organizes visible elements inside <body> using semantic landmarks like <header> and <main>.",
                            codeSnippet = """<!DOCTYPE html>
<html>
  <body>
    <header><h1>TejasCode</h1></header>
    <main><p>Learn • Code • Grow</p></main>
  </body>
</html>""",
                            languageLabel = "HTML",
                            question = "Which tag wraps the primary content of an HTML5 page?",
                            options = listOf("<main>", "<head>", "<meta>", "<style>"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Exact! <main> identifies the primary content for browsers and screen readers."
                        )
                    )
                ),
                CodingLesson(
                    id = "css_1",
                    trackId = "html_css_basics",
                    title = "CSS Neon Gradients & Flexbox",
                    durationMinutes = 5,
                    xpReward = 90,
                    summary = "Style modern cards with CSS linear-gradient, border-radius, and Flexbox alignment.",
                    steps = listOf(
                        LessonStep(
                            id = "css_1_s1",
                            title = "Flexbox & Gradient Cards",
                            explanation = "Combine display: flex with linear-gradient() to build responsive glowing UI cards.",
                            codeSnippet = """.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, #7C3AED, #00D2FF);
  border-radius: 16px;
}""",
                            languageLabel = "CSS",
                            question = "Which CSS property aligns flex items vertically inside a horizontal row?",
                            options = listOf("align-items: center", "float: center", "text-indent: 50%", "margin: 0"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Nice! align-items centers elements along the cross axis."
                        )
                    )
                )
            )
        ),
        CareerTrack(
            id = "javascript_pro",
            title = "JavaScript, TypeScript & PHP Backend",
            subtitle = "ES6 Pipelines, TypeScript Static Types & PHP Server Routes",
            category = CareerCategory.WEB,
            languages = listOf("JavaScript", "TypeScript", "PHP"),
            difficulty = "Beginner -> Pro",
            accentHex = 0xFFA855F7,
            lessons = listOf(
                CodingLesson(
                    id = "js_1",
                    trackId = "javascript_pro",
                    title = "JavaScript Variables, Arrays & Map",
                    durationMinutes = 5,
                    xpReward = 90,
                    summary = "Master const/let, arrow functions, and array transformations with map & filter.",
                    steps = listOf(
                        LessonStep(
                            id = "js_1_s1",
                            title = "Functional Array Pipelines",
                            explanation = "Use .filter() and .map() with arrow functions instead of mutating arrays manually.",
                            codeSnippet = """const xpList = [40, 80, 120];
const doubled = xpList.map(x => x * 2);
console.log(doubled); // [80, 160, 240]""",
                            languageLabel = "JavaScript",
                            question = "What does [1, 2, 3].filter(n => n > 1) return?",
                            options = listOf("[2, 3]", "[1]", "[1, 2, 3]", "2"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Correct! filter keeps every item where the predicate returns true."
                        )
                    )
                ),
                CodingLesson(
                    id = "ts_1",
                    trackId = "javascript_pro",
                    title = "TypeScript Interfaces & Type Guards",
                    durationMinutes = 6,
                    xpReward = 110,
                    summary = "Eliminate runtime undefined bugs with strict TypeScript interfaces and union types.",
                    steps = listOf(
                        LessonStep(
                            id = "ts_1_s1",
                            title = "Typed API Contracts",
                            explanation = "Define interfaces for payloads so the compiler verifies property names and types at build time.",
                            codeSnippet = """interface Course { id: string; xp: number; }
const c: Course = { id: "ts_1", xp: 110 };""",
                            languageLabel = "TypeScript",
                            question = "When does TypeScript perform type checking?",
                            options = listOf("At compile time before JS runs", "Only in SQL", "During CSS painting", "Never"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Right! TypeScript catches type mismatches during compilation."
                        )
                    )
                ),
                CodingLesson(
                    id = "php_1",
                    trackId = "javascript_pro",
                    title = "PHP Prepared Statements & JSON APIs",
                    durationMinutes = 6,
                    xpReward = 120,
                    summary = "Build secure backend endpoints in PHP with PDO prepared statements.",
                    isPremium = true,
                    steps = listOf(
                        LessonStep(
                            id = "php_1_s1",
                            title = "Preventing SQL Injection in PHP",
                            explanation = "Always bind parameters with PDO prepare() and execute() instead of concatenating user input.",
                            codeSnippet = """${'$'}stmt = ${'$'}pdo->prepare("SELECT * FROM users WHERE id = :id");
${'$'}stmt->execute(['id' => ${'$'}uid]);""",
                            languageLabel = "PHP",
                            question = "Why use PDO prepared statements in PHP?",
                            options = listOf("They prevent SQL injection", "They add CSS styles", "They compile C++", "They skip database auth"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Exact! Parameterized queries neutralize SQL injection."
                        )
                    )
                )
            )
        ),
        CareerTrack(
            id = "python_programming",
            title = "Python, SQL & Algorithms Mastery",
            subtitle = "Pythonic Data Structures, SQL Queries & Interview Prep",
            category = CareerCategory.DATA_AI,
            languages = listOf("Python", "SQL"),
            difficulty = "Beginner to Advanced",
            accentHex = 0xFF10B981,
            lessons = listOf(
                CodingLesson(
                    id = "py_1",
                    trackId = "python_programming",
                    title = "Python Functions & List Comprehensions",
                    durationMinutes = 5,
                    xpReward = 90,
                    summary = "Write crisp Pythonic code with functions, f-strings, and list comprehensions.",
                    steps = listOf(
                        LessonStep(
                            id = "py_1_s1",
                            title = "Transforming Lists in One Line",
                            explanation = "List comprehensions combine a loop and an optional filter into a fast, readable expression.",
                            codeSnippet = """scores = [45, 82, 91, 68, 95]
passing = [s for s in scores if s >= 70]
print(passing)  # [82, 91, 95]""",
                            languageLabel = "Python",
                            question = "What does [x * 2 for x in [1, 2, 3]] evaluate to?",
                            options = listOf("[2, 4, 6]", "[1, 2, 3, 1, 2, 3]", "[1, 4, 9]", "SyntaxError"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Clean! Each element x is multiplied by 2 -> [2, 4, 6]."
                        )
                    )
                ),
                CodingLesson(
                    id = "sql_1",
                    trackId = "python_programming",
                    title = "SQL Filtering, Joins & Aggregations",
                    durationMinutes = 5,
                    xpReward = 100,
                    summary = "Query relational tables cleanly with SELECT, WHERE, GROUP BY, and JOIN.",
                    steps = listOf(
                        LessonStep(
                            id = "sql_1_s1",
                            title = "Selecting & Ranking Rows",
                            explanation = "Combine WHERE and ORDER BY DESC to surface top performers from large tables.",
                            codeSnippet = """SELECT name, xp FROM developers
WHERE xp >= 1000
ORDER BY xp DESC;""",
                            languageLabel = "SQL",
                            question = "Which SQL clause sorts rows from highest to lowest?",
                            options = listOf("ORDER BY xp DESC", "GROUP BY xp", "FILTER BY xp", "TOP xp"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Spot on! ORDER BY ... DESC sorts descending."
                        )
                    )
                ),
                CodingLesson(
                    id = "py_2",
                    trackId = "python_programming",
                    title = "Python Dicts & Two-Sum Interview Lab",
                    durationMinutes = 7,
                    xpReward = 150,
                    isPremium = true,
                    summary = "Solve classic FAANG interview problems in O(n) time using hash maps.",
                    steps = listOf(
                        LessonStep(
                            id = "py_2_s1",
                            title = "O(1) Hash Map Lookups",
                            explanation = "Store seen numbers in a Python dict so you can find complements in constant O(1) time.",
                            codeSnippet = """def two_sum(nums, target):
    seen = {}
    for i, num in enumerate(nums):
        if target - num in seen:
            return [seen[target - num], i]
        seen[num] = i""",
                            languageLabel = "Python",
                            question = "What is the average time complexity of `key in dict` in Python?",
                            options = listOf("O(1)", "O(n^2)", "O(n log n)", "O(2^n)"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Exactly! Hash table lookups run in O(1) average time."
                        )
                    )
                )
            )
        ),
        CareerTrack(
            id = "software_mobile",
            title = "Java, Kotlin, Swift & C# Engineering",
            subtitle = "Mobile & Enterprise OOP across Android, iOS & .NET",
            category = CareerCategory.SOFTWARE_MOBILE,
            languages = listOf("Java", "Kotlin", "Swift", "C#", "C", "C++"),
            difficulty = "Intermediate",
            accentHex = 0xFFF59E0B,
            lessons = listOf(
                CodingLesson(
                    id = "kt_1",
                    trackId = "software_mobile",
                    title = "Kotlin Null Safety & Coroutines",
                    durationMinutes = 5,
                    xpReward = 100,
                    summary = "Prevent NullPointerExceptions at compile time with Kotlin's nullable types.",
                    steps = listOf(
                        LessonStep(
                            id = "kt_1_s1",
                            title = "Safe Calls & Elvis Operator",
                            explanation = "Use ?. and ?: to supply safe fallback values whenever a nullable expression is null.",
                            codeSnippet = """val student: String? = fetchName()
val greeting = "Welcome, ${'$'}{student ?: "Coder"}"
println(greeting)""",
                            languageLabel = "Kotlin",
                            question = "What does the Elvis operator `?:` return when the left-hand side is null?",
                            options = listOf(
                                "The fallback expression on the right",
                                "Throws NullPointerException",
                                "Returns an empty List",
                                "Halts the coroutine"
                            ),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Spot on! ?: supplies a safe default whenever the left side is null."
                        )
                    )
                ),
                CodingLesson(
                    id = "java_1",
                    trackId = "software_mobile",
                    title = "Java Collections, Streams & OOP",
                    durationMinutes = 6,
                    xpReward = 110,
                    summary = "Master Java classes, ArrayList, HashMap, and functional Stream pipelines.",
                    steps = listOf(
                        LessonStep(
                            id = "java_1_s1",
                            title = "Immutable Records & Streams",
                            explanation = "Use Java streams to transform collections cleanly without side effects.",
                            codeSnippet = """List<Integer> nums = List.of(1, 2, 3, 4);
int sum = nums.stream().mapToInt(Integer::intValue).sum();""",
                            languageLabel = "Java",
                            question = "Which Java package contains ArrayList and HashMap?",
                            options = listOf("java.util", "java.io", "java.sql", "java.awt"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Correct! java.util provides the Collections Framework."
                        )
                    )
                ),
                CodingLesson(
                    id = "c_cpp_1",
                    trackId = "software_mobile",
                    title = "C Pointers, C++ STL & Memory Mastery",
                    durationMinutes = 7,
                    xpReward = 140,
                    isPremium = true,
                    summary = "Understand stack vs heap memory, pointers in C, and std::vector in modern C++.",
                    steps = listOf(
                        LessonStep(
                            id = "c_cpp_1_s1",
                            title = "RAII in Modern C++",
                            explanation = "Use smart pointers and STL containers like std::vector so resources clean up automatically.",
                            codeSnippet = """#include <vector>
std::vector<int> scores = {90, 95, 100};""",
                            languageLabel = "C++",
                            question = "Which C++ container provides a dynamically resizable contiguous array?",
                            options = listOf("std::vector", "std::set", "std::map", "int*"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Exact! std::vector manages contiguous dynamic arrays safely."
                        )
                    )
                ),
                CodingLesson(
                    id = "swift_cs_1",
                    trackId = "software_mobile",
                    title = "Swift Optionals & C# LINQ Essentials",
                    durationMinutes = 6,
                    xpReward = 120,
                    summary = "Cross-platform fluency with Swift guard-let unwrapping and C# LINQ queries.",
                    steps = listOf(
                        LessonStep(
                            id = "swift_cs_1_s1",
                            title = "Swift Guard & C# Properties",
                            explanation = "Both Swift and C# prioritize expressive type safety for native apps and cloud APIs.",
                            codeSnippet = """guard let user = currentUser else { return }
print("Hello, \\(user.name)")""",
                            languageLabel = "Swift",
                            question = "What must a Swift `guard` else block always do?",
                            options = listOf("Exit the current scope (return/throw)", "Print to console", "Allocate memory", "Sleep 1s"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Yes! Swift guard enforces early exit on invalid state."
                        )
                    )
                )
            )
        ),
        CareerTrack(
            id = "systems_go_rust",
            title = "Go Concurrency & Rust Memory Safety",
            subtitle = "Goroutines, Channels, Ownership & Borrow Checker (Pro Track)",
            category = CareerCategory.ESSENTIALS,
            languages = listOf("Go", "Rust"),
            difficulty = "Advanced",
            accentHex = 0xFF38BDF8,
            isPremium = true,
            lessons = listOf(
                CodingLesson(
                    id = "go_rust_1",
                    trackId = "systems_go_rust",
                    title = "Go Goroutines & Rust Ownership",
                    durationMinutes = 8,
                    xpReward = 180,
                    isPremium = true,
                    summary = "Build ultra-scalable cloud microservices in Go and fearless zero-GC systems in Rust.",
                    steps = listOf(
                        LessonStep(
                            id = "go_rust_1_s1",
                            title = "Go Channels vs Rust Borrowing",
                            explanation = "Go shares memory by communicating over channels; Rust guarantees thread safety via compile-time ownership.",
                            codeSnippet = """ch := make(chan string, 1)
go func() { ch <- "TejasCode Worker Ready" }()""",
                            languageLabel = "Go",
                            question = "How does Rust prevent data races without a garbage collector?",
                            options = listOf("Ownership and borrowing rules checked at compile time", "Runtime sleep locks", "Disabling functions", "Using HTML"),
                            correctOptionIndex = 0,
                            feedbackCorrect = "Masterful! Rust's borrow checker enforces aliasing rules at compile time."
                        )
                    )
                )
            )
        )
    )

    fun managedCoursesAsTrack(managedCourses: List<ManagedCourse>): CareerTrack? {
        if (managedCourses.isEmpty()) return null
        val lessons = managedCourses.map { course ->
            CodingLesson(
                id = "managed_${course.id}",
                trackId = "tejascode_live_catalog",
                title = course.title,
                durationMinutes = course.durationMinutes.toInt().coerceAtLeast(1),
                xpReward = course.xpReward.toInt().coerceAtLeast(10),
                summary = course.subtitle,
                isPremium = course.isPremium,
                resourceUrl = course.resourceUrl,
                steps = listOf(
                    LessonStep(
                        id = "managed_${course.id}_s1",
                        title = course.title,
                        explanation = course.lessonSummary.ifBlank { course.subtitle },
                        codeSnippet = course.starterCode.ifBlank { null },
                        languageLabel = course.language,
                        question = course.quizQuestion.ifBlank { null },
                        options = course.quizOptions,
                        correctOptionIndex = course.correctOptionIndex.toInt().coerceIn(0, (course.quizOptions.size - 1).coerceAtLeast(0)),
                        feedbackCorrect = "Great job! You completed ${course.title}."
                    )
                )
            )
        }
        return CareerTrack(
            id = "tejascode_live_catalog",
            title = "Instructor Published Courses",
            subtitle = "Live courses curated in the TejasCode Admin Dashboard",
            category = CareerCategory.ALL,
            languages = managedCourses.map { it.language }.distinct(),
            difficulty = "Live Catalog",
            accentHex = 0xFF00D2FF,
            lessons = lessons
        )
    }

    val battleQuestions: List<CodingBattleQuestion> = listOf(
        CodingBattleQuestion(
            id = "bq_html",
            language = "HTML",
            prompt = "Which HTML tag creates a clickable hyperlink?",
            codeSnippet = """<____ href="https://tejascode.dev">Learn</____>""",
            options = listOf("<a>", "<link>", "<href>", "<url>"),
            correctIndex = 0,
            explanation = "The anchor <a> tag with href creates clickable hyperlinks in HTML."
        ),
        CodingBattleQuestion(
            id = "bq_css",
            language = "CSS",
            prompt = "Which CSS property rounds the corners of a button or card?",
            codeSnippet = """.card { ____: 16px; }""",
            options = listOf("border-radius", "corner-curve", "edge-round", "box-arc"),
            correctIndex = 0,
            explanation = "border-radius controls rounded corners in CSS."
        ),
        CodingBattleQuestion(
            id = "bq_js",
            language = "JavaScript",
            prompt = "Which value does this JavaScript expression log?",
            codeSnippet = """const arr = [1, 2, 3];
const res = arr.filter(n => n > 1).map(n => n * 10);
console.log(res[0]);""",
            options = listOf("20", "10", "30", "undefined"),
            correctIndex = 0,
            explanation = "filter keeps [2, 3], map produces [20, 30], so res[0] is 20."
        ),
        CodingBattleQuestion(
            id = "bq_py",
            language = "Python",
            prompt = "What is the exact output of this Python snippet?",
            codeSnippet = """nums = [10, 20, 30, 40]
print(nums[-2] + len(nums))""",
            options = listOf("34", "24", "44", "IndexError"),
            correctIndex = 0,
            explanation = "nums[-2] is 30 and len(nums) is 4, so 30 + 4 = 34."
        ),
        CodingBattleQuestion(
            id = "bq_algo",
            language = "Algorithms",
            prompt = "What is the worst-case time complexity of Binary Search on a sorted array?",
            codeSnippet = """while (low <= high) {
    mid = (low + high) / 2
}""",
            options = listOf("O(log n)", "O(n)", "O(1)", "O(n log n)"),
            correctIndex = 0,
            explanation = "Halving the search space each iteration takes O(log n) steps."
        )
    )

    val challenges: List<CodingChallenge> = listOf(
        CodingChallenge(
            id = "ch_html_card",
            title = "HTML5 Hero Card",
            difficulty = "Easy",
            language = ProgrammingLanguage.HTML,
            prompt = "Build a semantic card with an <h1> heading, <p> tagline, and <button> CTA.",
            starterCode = """<div class="card">
  <h1>TejasCode Academy</h1>
  <p>Master 15 Programming Languages</p>
  <button>Enroll Free</button>
</div>""",
            expectedHint = "Include <h1>, <p>, and <button> tags inside your container.",
            xpReward = 80
        ),
        CodingChallenge(
            id = "ch_fizzbuzz",
            title = "Python FizzBuzz Turbo",
            difficulty = "Easy",
            language = ProgrammingLanguage.PYTHON,
            prompt = "Print numbers 1..15, printing 'Fizz' for multiples of 3, 'Buzz' for 5, and 'FizzBuzz' for both.",
            starterCode = """for i in range(1, 16):
    if i % 15 == 0:
        print("FizzBuzz")
    elif i % 3 == 0:
        print("Fizz")
    elif i % 5 == 0:
        print("Buzz")
    else:
        print(i)""",
            expectedHint = "Check modulo 15 before 3 or 5 so multiples of both print FizzBuzz.",
            xpReward = 90
        ),
        CodingChallenge(
            id = "ch_palindrome",
            title = "JavaScript Palindrome",
            difficulty = "Medium",
            language = ProgrammingLanguage.JAVASCRIPT,
            prompt = "Check whether a string reads the same forward and backward in O(n) time.",
            starterCode = """const word = "racecar";
const reversed = word.split("").reverse().join("");
console.log("Is palindrome:", word === reversed);""",
            expectedHint = "Compare the original string with its reversed form.",
            xpReward = 120
        ),
        CodingChallenge(
            id = "ch_kotlin_freq",
            title = "Frequency Counter in Kotlin",
            difficulty = "Hard",
            language = ProgrammingLanguage.KOTLIN,
            prompt = "Count character frequencies in 'tejascode' and print the resulting map.",
            starterCode = """fun main() {
    val word = "tejascode"
    val freq = word.groupingBy { it }.eachCount()
    println("Frequencies: ${'$'}freq")
}""",
            expectedHint = "Use groupingBy { it }.eachCount() for idiomatic O(n) counting.",
            xpReward = 150
        )
    )
}
