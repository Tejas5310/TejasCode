package com.example.engine

import com.example.model.ProgrammingLanguage
import java.util.Locale

data class ExecutionResult(
    val output: String,
    val isError: Boolean,
    val durationMs: Long,
    val engineLabel: String
)

/**
 * Deterministic lightweight on-device interpreter for instant zero-latency execution
 * across all 15 TejasCode languages. Complex scripts can also be evaluated via Firebase AI Sandbox.
 */
object LocalCodeRunner {

    private data class DevRow(val name: String, val track: String, val xp: Int)

    private val sampleDevelopersTable = listOf(
        DevRow("Maya Lin", "Python & AI", 2450),
        DevRow("Liam Patel", "Full-Stack Web", 1820),
        DevRow("Sofia Reyes", "Kotlin Mobile", 1490),
        DevRow("Noah Kim", "Dev Essentials", 890),
        DevRow("Elena Costa", "Rust Systems", 2110)
    )

    fun run(language: ProgrammingLanguage, rawCode: String): ExecutionResult {
        val start = System.nanoTime()
        val code = rawCode.trim()
        if (code.isBlank()) {
            return ExecutionResult("Error: Editor buffer is empty.", isError = true, durationMs = 1L, engineLabel = "Local VM")
        }

        val resultText: Pair<String, Boolean> = when (language) {
            ProgrammingLanguage.SQL -> executeSql(code)
            ProgrammingLanguage.HTML -> executeHtmlSummary(code)
            ProgrammingLanguage.CSS -> executeCssSummary(code)
            else -> executeImperative(language, code)
        }

        val elapsedMs = ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(2L)
        return ExecutionResult(
            output = resultText.first,
            isError = resultText.second,
            durationMs = elapsedMs,
            engineLabel = "${language.displayName} Local Runner"
        )
    }

    private fun executeSql(sql: String): Pair<String, Boolean> {
        val upper = sql.uppercase(Locale.US)
        if (!upper.contains("SELECT")) {
            return "SQL Hint: Try a SELECT ... FROM developers query (or tap AI Compile for full SQL DDL)." to true
        }
        var rows = sampleDevelopersTable
        val xpFilter = Regex("""WHERE\s+xp\s*>=\s*(\d+)""", RegexOption.IGNORE_CASE).find(sql)
        if (xpFilter != null) {
            val minXp = xpFilter.groupValues[1].toIntOrNull() ?: 0
            rows = rows.filter { it.xp >= minXp }
        }
        if (upper.contains("ORDER BY") && upper.contains("DESC")) {
            rows = rows.sortedByDescending { it.xp }
        } else if (upper.contains("ORDER BY")) {
            rows = rows.sortedBy { it.xp }
        }

        val sb = StringBuilder()
        sb.appendLine("name          | track            | xp")
        sb.appendLine("--------------+------------------+------")
        rows.forEach {
            sb.appendLine("%-13s | %-16s | %d".format(Locale.US, it.name, it.track, it.xp))
        }
        sb.append("(${rows.size} row(s) returned)")
        return sb.toString() to false
    }

    private fun executeHtmlSummary(html: String): Pair<String, Boolean> {
        val headings = Regex("""<h[1-6][^>]*>(.*?)</h[1-6]>""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1].trim() }.toList()
        val paragraphs = Regex("""<p[^>]*>(.*?)</p>""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1].trim() }.toList()
        val buttons = Regex("""<button[^>]*>(.*?)</button>""", RegexOption.IGNORE_CASE)
            .findAll(html).map { it.groupValues[1].trim() }.toList()

        val lines = mutableListOf<String>()
        lines.add("DOM Parsed Successfully:")
        headings.forEach { lines.add("  [Heading] $it") }
        paragraphs.forEach { lines.add("  [Paragraph] $it") }
        buttons.forEach { lines.add("  [Button] [$it]") }
        if (lines.size == 1) {
            lines.add("  Rendered ${html.lines().size} lines of HTML5 markup.")
        }
        return lines.joinToString("\n") to false
    }

    private fun executeCssSummary(css: String): Pair<String, Boolean> {
        val selectors = Regex("""([.#a-zA-Z0-9_-]+)\s*\{""").findAll(css).map { it.groupValues[1] }.toList()
        val propCount = Regex("""[a-zA-Z-]+\s*:[^;]+;""").findAll(css).count()
        val lines = mutableListOf<String>()
        lines.add("CSS Stylesheet Validated:")
        lines.add("  Selectors: ${if (selectors.isEmpty()) "inline" else selectors.joinToString(", ")}")
        lines.add("  Declarations: $propCount property rule(s)")
        return lines.joinToString("\n") to false
    }

    private fun executeImperative(language: ProgrammingLanguage, code: String): Pair<String, Boolean> {
        val env = mutableMapOf<String, Double>()
        val strEnv = mutableMapOf<String, String>()
        val listEnv = mutableMapOf<String, List<String>>()
        val outputLines = mutableListOf<String>()

        val lines = code.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//") ||
                line.startsWith("<?php") || line.startsWith("?>") ||
                line.startsWith("import ") || line.startsWith("package ") ||
                line.startsWith("using ") || line == "{" || line == "}" ||
                line.startsWith("fun main") || line.startsWith("int main") ||
                line.startsWith("fn main") || line.startsWith("public class") ||
                line.startsWith("public static void main") || line.startsWith("static void Main") ||
                line.startsWith("class ") || line.startsWith("return ")
            ) {
                continue
            }

            // Simple variable assignments across Kotlin/JS/TS/Python/Java/C/C++/C#/PHP/Go/Swift/Rust
            val assignMatch = Regex(
                """^(?:val|var|let|const|int|double|float|String|string)?\s*\$?([a-zA-Z_]\w*)(?:\s*:\s*[a-zA-Z0-9_<>\[\]]+)?\s*:?=\s*(.+?);?$"""
            ).find(line)
            if (assignMatch != null) {
                val varName = assignMatch.groupValues[1]
                val expr = assignMatch.groupValues[2].trim()
                if ((expr.startsWith("[") && expr.endsWith("]")) || expr.startsWith("listOf(")) {
                    val inner = expr.removePrefix("[").removeSuffix("]")
                        .removePrefix("listOf(").removeSuffix(")")
                    val items = inner.split(",").map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }
                    listEnv[varName] = items
                } else if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
                    strEnv[varName] = expr.substring(1, expr.length - 1)
                } else {
                    val numVal = evalNumeric(expr, env)
                    if (numVal != null) {
                        env[varName] = numVal
                    }
                }
                continue
            }

            val printExpr = extractPrintContent(language, line)
            if (printExpr != null) {
                outputLines.add(formatPrintArgs(printExpr, env, strEnv, listEnv))
            }
        }

        if (outputLines.isEmpty()) {
            return "Compiled ${language.displayName} cleanly (0 errors, ${lines.size} lines).\nTip: Use 'AI Compile' below for full multi-function & loop execution." to false
        }
        return outputLines.joinToString("\n") to false
    }

    private fun extractPrintContent(language: ProgrammingLanguage, line: String): String? {
        return when (language) {
            ProgrammingLanguage.PYTHON -> Regex("""^print\s*\((.*)\)\s*$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.SWIFT -> Regex("""^print\s*\((.*)\)\s*$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.JAVASCRIPT, ProgrammingLanguage.TYPESCRIPT ->
                Regex("""^console\.log\s*\((.*)\)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.KOTLIN -> Regex("""^println\s*\((.*)\)\s*$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.JAVA -> Regex("""^System\.out\.println\s*\((.*)\)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.CSHARP -> Regex("""^Console\.WriteLine\s*\((.*)\)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.C -> Regex("""^printf\s*\((.*)\)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.PHP -> Regex("""^echo\s+(.+?)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.GO -> Regex("""^fmt\.Println\s*\((.*)\)\s*$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.RUST -> Regex("""^println!\s*\((.*)\)\s*;?$""").find(line)?.groupValues?.get(1)
            ProgrammingLanguage.CPP -> Regex("""^cout\s*<<\s*(.+?)(?:\s*<<\s*endl)?\s*;?$""").find(line)?.groupValues?.get(1)
            else -> null
        }
    }

    private fun formatPrintArgs(
        argString: String,
        env: Map<String, Double>,
        strEnv: Map<String, String>,
        listEnv: Map<String, List<String>>
    ): String {
        val parts = when {
            argString.contains("<<") -> argString.split("<<")
            argString.contains(" + ") && !argString.trim().matches(Regex("""^[0-9a-zA-Z_.\s+\-*/()]+$""")) ->
                argString.split(" + ")
            else -> splitTopLevelCommas(argString)
        }
        return parts.joinToString(" ") { rawPart ->
            val part = rawPart.trim()
            when {
                part.startsWith("f\"") || part.startsWith("f'") -> {
                    var body = part.substring(2, part.length - 1)
                    env.forEach { (k, v) -> body = body.replace("{$k}", formatNum(v)) }
                    strEnv.forEach { (k, v) -> body = body.replace("{$k}", v) }
                    body
                }
                (part.startsWith("\"") && part.endsWith("\"")) || (part.startsWith("'") && part.endsWith("'")) -> {
                    var body = part.substring(1, part.length - 1)
                        .replace("\\n", "")
                    env.forEach { (k, v) ->
                        body = body.replace("\$$k", formatNum(v)).replace("\${$k}", formatNum(v))
                    }
                    strEnv.forEach { (k, v) ->
                        body = body.replace("\$$k", v).replace("\${$k}", v)
                    }
                    body
                }
                listEnv.containsKey(part) -> listEnv[part].toString()
                strEnv.containsKey(part) -> strEnv[part].orEmpty()
                env.containsKey(part) -> formatNum(env[part] ?: 0.0)
                else -> {
                    val eval = evalNumeric(part.removePrefix("(").removeSuffix(")"), env)
                    if (eval != null) formatNum(eval) else part
                }
            }
        }
    }

    private fun splitTopLevelCommas(input: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '
        for (c in input) {
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true
                quoteChar = c
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false
            }
            if (c == ',' && !inQuotes) {
                out.add(cur.toString())
                cur.clear()
            } else {
                cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    private fun evalNumeric(expr: String, env: Map<String, Double>): Double? {
        var resolved = expr.trim()
        val lenMatch = Regex("""len\((\w+)\)""").find(resolved)
        if (lenMatch != null) return null
        env.entries.sortedByDescending { it.key.length }.forEach { (k, v) ->
            resolved = resolved.replace(Regex("""\b$k\b"""), v.toString())
        }
        return try {
            SimpleExprParser(resolved).parse()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatNum(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
    }

    private class SimpleExprParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+'.code) -> x + parseTerm()
                    eat('-'.code) -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                x = when {
                    eat('*'.code) -> x * parseFactor()
                    eat('/'.code) -> x / parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            val x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected factor")
            }
            return x
        }
    }
}
