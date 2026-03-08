package com.loud.alarm.ui.challenge

import com.loud.alarm.data.MathDifficulty
import kotlin.random.Random

data class MathProblem(
    val question: String,
    val answer: Int
)

object MathProblemGenerator {

    fun generateProblem(difficulty: MathDifficulty): MathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> generateEasyProblem()
            MathDifficulty.MEDIUM -> generateMediumProblem()
            MathDifficulty.HARD -> generateHardProblem()
            MathDifficulty.EXTREME -> generateExtremeProblem()
        }
    }

    private fun generateEasyProblem(): MathProblem {
        // 2-digit addition/subtraction
        val a = Random.nextInt(10, 99)
        val b = Random.nextInt(10, 99)
        val isAddition = Random.nextBoolean()

        return if (isAddition) {
            MathProblem("$a + $b = ?", a + b)
        } else {
            // Ensure positive result for simplicity or allow negative? Let's ensure a >= b
            val max = maxOf(a, b)
            val min = minOf(a, b)
            MathProblem("$max - $min = ?", max - min)
        }
    }

    private fun generateMediumProblem(): MathProblem {
        // Multi-step: (a + b) * c or similar
        val a = Random.nextInt(10, 50)
        val b = Random.nextInt(10, 50)
        val c = Random.nextInt(2, 6)

        val operation = Random.nextInt(3)
        return when (operation) {
            0 -> MathProblem("($a + $b) × $c = ?", (a + b) * c)
            1 -> {
                val max = maxOf(a, b)
                val min = minOf(a, b)
                MathProblem("($max - $min) × $c = ?", (max - min) * c)
            }
            else -> MathProblem("$a + $b × $c = ?", a + (b * c))
        }
    }

    private fun generateHardProblem(): MathProblem {
        // Equation type: solve for x
        val x = Random.nextInt(2, 20)       // answer is always a positive integer
        val a = Random.nextInt(2, 9)         // coefficient
        val b = Random.nextInt(1, 30)        // constant

        val operation = Random.nextInt(3)
        return when (operation) {
            0 -> {
                // ax + b = c  →  solve for x
                val c = a * x + b
                MathProblem("${a}x + $b = $c\nx = ?", x)
            }
            1 -> {
                // ax - b = c  →  solve for x
                val c = a * x - b
                MathProblem("${a}x - $b = $c\nx = ?", x)
            }
            else -> {
                // c - ax = b  →  solve for x
                val c = a * x + b
                MathProblem("$c - ${a}x = $b\nx = ?", x)
            }
        }
    }

    private fun generateExtremeProblem(): MathProblem {
        val type = Random.nextInt(3)
        return when (type) {
            0 -> {
                // 3-digit × 2-digit multiplication
                val a = Random.nextInt(100, 999)
                val b = Random.nextInt(10, 99)
                MathProblem("$a × $b = ?", a * b)
            }
            1 -> {
                // Long multi-step chain:  a × b + c - d
                val a = Random.nextInt(10, 99)
                val b = Random.nextInt(3, 9)
                val c = Random.nextInt(100, 500)
                val d = Random.nextInt(10, 99)
                val result = a * b + c - d
                MathProblem("$a × $b + $c - $d = ?", result)
            }
            else -> {
                // Complex equation:  ax + b × c = d,  solve for x
                val x = Random.nextInt(5, 50)
                val a = Random.nextInt(3, 12)
                val b = Random.nextInt(10, 50)
                val c = Random.nextInt(2, 8)
                val d = a * x + b * c
                MathProblem("${a}x + $b × $c = $d\nx = ?", x)
            }
        }
    }
}
