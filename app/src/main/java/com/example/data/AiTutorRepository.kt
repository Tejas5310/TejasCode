package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiTutorRepository {
    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are TejasCode AI Tutor, an encouraging, concise senior software engineer " +
                        "and computer science mentor. Help learners debug bugs, explain concepts " +
                        "with clear step-by-step intuition, suggest Big-O improvements, and provide " +
                        "clean, runnable code examples in fenced markdown blocks. Keep answers focused " +
                        "and mobile-readable."
                )
            }
        )
    }

    private val sandboxModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You act as a deterministic multi-language code compiler, executor, and debugger. " +
                        "Given source code and its language, output: \n" +
                        "1) Exact simulated stdout output (or compiler/runtime error if buggy),\n" +
                        "2) Time/space complexity note,\n" +
                        "3) One crisp tip to make the code cleaner. Keep output concise."
                )
            }
        )
    }

    fun streamTutorResponse(prompt: String, conversationContext: String): Flow<String> {
        val fullPrompt = if (conversationContext.isBlank()) prompt
        else "Recent conversation:\n$conversationContext\n\nLearner request:\n$prompt"
        return tutorModel.generateContentStream(fullPrompt)
            .map { chunk -> chunk.text.orEmpty() }
    }

    suspend fun evaluateCodeInSandbox(language: String, code: String): String {
        val prompt = """
            Language: $language
            Code:
            ```$language
            $code
            ```
            Simulate execution and show stdout, exit status, and a brief 1-line code quality tip.
        """.trimIndent()
        val response = sandboxModel.generateContent(prompt)
        return response.text?.trim().orEmpty().ifBlank { "No output returned from AI Sandbox." }
    }
}
