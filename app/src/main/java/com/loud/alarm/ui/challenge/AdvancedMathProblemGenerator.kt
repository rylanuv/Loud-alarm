package com.loud.alarm.ui.challenge

import com.loud.alarm.data.AdvancedMathTopic
import com.loud.alarm.data.MathDifficulty
import kotlin.random.Random
import kotlin.math.*

data class AdvancedMathProblem(
    val question: String,
    val answer: String,
    val topic: AdvancedMathTopic,
    val hint: String = ""
)

object AdvancedMathProblemGenerator {

    fun generateProblem(topics: Set<AdvancedMathTopic>, difficulty: MathDifficulty = MathDifficulty.EASY): AdvancedMathProblem {
        val topic = if (topics.isNotEmpty()) topics.random() else AdvancedMathTopic.POLYNOMIAL
        return when (topic) {
            AdvancedMathTopic.POLYNOMIAL -> generatePolynomial(difficulty)
            AdvancedMathTopic.GEOMETRY -> generateGeometry(difficulty)
            AdvancedMathTopic.TRIGONOMETRY -> generateTrigonometry(difficulty)
            AdvancedMathTopic.CALCULUS -> generateCalculus(difficulty)
            AdvancedMathTopic.MATRIX -> generateMatrix(difficulty)
            AdvancedMathTopic.LOGARITHM -> generateLogarithm(difficulty)
            AdvancedMathTopic.PROBABILITY -> generateProbability(difficulty)
            AdvancedMathTopic.SEQUENCE -> generateSequence(difficulty)
        }
    }

    private fun generatePolynomial(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val a = Random.nextInt(2, 7)
                val b = Random.nextInt(1, 11)
                val c = Random.nextInt(1, 6)
                AdvancedMathProblem(
                    question = "f(x) = ${a}x + $b\nf($c) = ?",
                    answer = (a * c + b).toString(),
                    topic = AdvancedMathTopic.POLYNOMIAL,
                    hint = "Substitute x = $c"
                )
            }
            MathDifficulty.MEDIUM -> {
                val a = Random.nextInt(1, 10)
                val b = Random.nextInt(1, 10)
                val s = a + b
                val p = a * b
                AdvancedMathProblem(
                    question = "Largest root of\nx² - ${s}x + $p = 0",
                    answer = max(a, b).toString(),
                    topic = AdvancedMathTopic.POLYNOMIAL,
                    hint = "Factor the quadratic"
                )
            }
            MathDifficulty.HARD -> {
                val b = Random.nextInt(-5, 6)
                val c = Random.nextInt(1, 11)
                val a = Random.nextInt(2, 6)
                val signB = if (b >= 0) "+ $b" else "- ${abs(b)}"
                AdvancedMathProblem(
                    question = "f(x) = x² $signB x + $c\nf($a) = ?",
                    answer = (a * a + b * a + c).toString(),
                    topic = AdvancedMathTopic.POLYNOMIAL,
                    hint = "Substitute and simplify"
                )
            }
            MathDifficulty.EXTREME -> {
                val a = Random.nextInt(1, 6)
                val b = Random.nextInt(1, 6)
                val c = Random.nextInt(1, 6)
                val s = a + b + c
                val p = a * b + b * c + c * a
                val q = a * b * c
                AdvancedMathProblem(
                    question = "Sum of roots of\nx³ - ${s}x² + ${p}x - $q = 0",
                    answer = s.toString(),
                    topic = AdvancedMathTopic.POLYNOMIAL,
                    hint = "Vieta's formula: sum = -(-S)/1"
                )
            }
        }
    }

    private fun generateGeometry(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val w = Random.nextInt(5, 16)
                val h = Random.nextInt(5, 16)
                AdvancedMathProblem(
                    question = "Area of a $w × $h rectangle",
                    answer = (w * h).toString(),
                    topic = AdvancedMathTopic.GEOMETRY,
                    hint = "Area = width × height"
                )
            }
            MathDifficulty.MEDIUM -> {
                val baseOptions = (4..20 step 2).toList()
                val b = baseOptions.random()
                val h = Random.nextInt(3, 16)
                AdvancedMathProblem(
                    question = "Area of triangle\nbase = $b, height = $h",
                    answer = ((b * h) / 2).toString(),
                    topic = AdvancedMathTopic.GEOMETRY,
                    hint = "Area = ½ × base × height"
                )
            }
            MathDifficulty.HARD -> {
                val triples = listOf(
                    Triple(3, 4, 5),
                    Triple(5, 12, 13),
                    Triple(8, 15, 17),
                    Triple(6, 8, 10),
                    Triple(9, 12, 15)
                )
                val t = triples.random()
                AdvancedMathProblem(
                    question = "Right triangle sides: ${t.first}, ${t.second}\nHypotenuse = ?",
                    answer = t.third.toString(),
                    topic = AdvancedMathTopic.GEOMETRY,
                    hint = "Use a² + b² = c²"
                )
            }
            MathDifficulty.EXTREME -> {
                val l = Random.nextInt(3, 9)
                val w = Random.nextInt(2, 7)
                val h = Random.nextInt(2, 6)
                AdvancedMathProblem(
                    question = "Volume of box\nl=$l, w=$w, h=$h",
                    answer = (l * w * h).toString(),
                    topic = AdvancedMathTopic.GEOMETRY,
                    hint = "V = l × w × h"
                )
            }
        }
    }

    private fun generateTrigonometry(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val problems = listOf(
                    Triple("2 × sin(30°)", 1, "sin(30°) = 0.5"),
                    Triple("sin(90°)", 1, "sin(90°) = 1"),
                    Triple("cos(0°)", 1, "cos(0°) = 1"),
                    Triple("2 × cos(60°)", 1, "cos(60°) = 0.5"),
                    Triple("cos(90°)", 0, "cos(90°) = 0"),
                    Triple("tan(0°)", 0, "tan(0°) = 0"),
                    Triple("tan(45°)", 1, "tan(45°) = 1"),
                    Triple("sin(0°)", 0, "sin(0°) = 0")
                )
                val p = problems.random()
                AdvancedMathProblem(
                    question = "${p.first}\n= ?",
                    answer = p.second.toString(),
                    topic = AdvancedMathTopic.TRIGONOMETRY,
                    hint = "Recall standard angle values"
                )
            }
            MathDifficulty.MEDIUM -> {
                val problems = listOf(
                    Pair("sin²(30°) + cos²(30°)", 1),
                    Pair("sin²(45°) + cos²(45°)", 1),
                    Pair("sin²(60°) + cos²(60°)", 1),
                    Pair("4 × sin(30°) × cos(60°)", 1),
                    Pair("cos²(0°) + sin²(90°)", 2),
                    Pair("tan(45°) × sin(90°)", 1),
                    Pair("2 × sin²(45°)", 1),
                    Pair("2 × cos²(45°)", 1)
                )
                val p = problems.random()
                AdvancedMathProblem(
                    question = "${p.first} = ?",
                    answer = p.second.toString(),
                    topic = AdvancedMathTopic.TRIGONOMETRY,
                    hint = "Use the Pythagorean identity"
                )
            }
            MathDifficulty.HARD -> {
                val problems = listOf(
                    Pair("2 × sin(45°) × cos(45°)", 1),
                    Pair("sin²(90°) + cos²(0°)", 2),
                    Pair("3 × tan(45°)", 3),
                    Pair("4 × sin(30°)", 2),
                    Pair("cos²(0°) - sin²(0°)", 1),
                    Pair("2 × (sin²(30°) + cos²(30°))", 2)
                )
                val p = problems.random()
                AdvancedMathProblem(
                    question = "${p.first} = ?",
                    answer = p.second.toString(),
                    topic = AdvancedMathTopic.TRIGONOMETRY,
                    hint = "Apply the identity"
                )
            }
            MathDifficulty.EXTREME -> {
                val problems = listOf(
                    Pair("(1 - cos(60°)) × 4", 2),
                    Pair("tan(45°) + sin(90°) + cos(0°)", 3),
                    Pair("4 × (sin²(45°) + cos²(45°))", 4),
                    Pair("sin(90°) + 2 × sin(30°) + tan(45°)", 3),
                    Pair("(sin(30°) + cos(60°)) × 2", 2),
                    Pair("5 × tan(45°) - cos(0°)", 4)
                )
                val p = problems.random()
                AdvancedMathProblem(
                    question = "${p.first} = ?",
                    answer = p.second.toString(),
                    topic = AdvancedMathTopic.TRIGONOMETRY,
                    hint = "Break it down step by step"
                )
            }
        }
    }

    private fun generateCalculus(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val c = Random.nextInt(2, 10)
                AdvancedMathProblem(
                    question = "d/dx(${c}x) = ?",
                    answer = c.toString(),
                    topic = AdvancedMathTopic.CALCULUS,
                    hint = "Derivative of cx is c"
                )
            }
            MathDifficulty.MEDIUM -> {
                val c = Random.nextInt(2, 6)
                val a = Random.nextInt(1, 5)
                AdvancedMathProblem(
                    question = "d/dx(${c}x²) at x=$a\n= ?",
                    answer = (2 * c * a).toString(),
                    topic = AdvancedMathTopic.CALCULUS,
                    hint = "Power rule: d/dx(cx²) = 2cx"
                )
            }
            MathDifficulty.HARD -> {
                val c = Random.nextInt(1, 4)
                val a = Random.nextInt(1, 4)
                AdvancedMathProblem(
                    question = "d/dx(${c}x³) at x=$a\n= ?",
                    answer = (3 * c * a * a).toString(),
                    topic = AdvancedMathTopic.CALCULUS,
                    hint = "Power rule: d/dx(cx³) = 3cx²"
                )
            }
            MathDifficulty.EXTREME -> {
                val c = Random.nextInt(1, 4)
                val a = Random.nextInt(1, 5)
                AdvancedMathProblem(
                    question = "d²/dx²(${c}x³) at x=$a\n= ?",
                    answer = (6 * c * a).toString(),
                    topic = AdvancedMathTopic.CALCULUS,
                    hint = "First: 3cx², then: 6cx"
                )
            }
        }
    }

    private fun generateMatrix(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val a = Random.nextInt(1, 10)
                val d = Random.nextInt(1, 10)
                val b = Random.nextInt(1, 6)
                val c = Random.nextInt(1, 6)
                AdvancedMathProblem(
                    question = "Trace of\n| $a  $b |\n| $c  $d |",
                    answer = (a + d).toString(),
                    topic = AdvancedMathTopic.MATRIX,
                    hint = "Trace = sum of diagonal"
                )
            }
            MathDifficulty.MEDIUM -> {
                val a = Random.nextInt(1, 7)
                val b = Random.nextInt(1, 7)
                val c = Random.nextInt(1, 7)
                val d = Random.nextInt(1, 7)
                AdvancedMathProblem(
                    question = "det | $a  $b |\n    | $c  $d |\n= ?",
                    answer = (a * d - b * c).toString(),
                    topic = AdvancedMathTopic.MATRIX,
                    hint = "det = ad - bc"
                )
            }
            MathDifficulty.HARD -> {
                val a1 = Random.nextInt(1, 5)
                val a2 = Random.nextInt(1, 5)
                val a3 = Random.nextInt(1, 5)
                val a4 = Random.nextInt(1, 5)
                val b1 = Random.nextInt(1, 5)
                val b2 = Random.nextInt(1, 5)
                val b3 = Random.nextInt(1, 5)
                val b4 = Random.nextInt(1, 5)
                AdvancedMathProblem(
                    question = "A×B element [1,1]\nA=|$a1 $a2|  B=|$b1 $b2|\n  |$a3 $a4|    |$b3 $b4|",
                    answer = (a1 * b1 + a2 * b3).toString(),
                    topic = AdvancedMathTopic.MATRIX,
                    hint = "Row × Column: a1×b1 + a2×b3"
                )
            }
            MathDifficulty.EXTREME -> {
                val a = Random.nextInt(0, 4)
                val b = Random.nextInt(0, 4)
                val c = Random.nextInt(0, 4)
                val d = Random.nextInt(0, 4)
                val e = Random.nextInt(0, 4)
                val f = Random.nextInt(0, 4)
                val g = Random.nextInt(0, 4)
                val h = Random.nextInt(0, 4)
                val i = Random.nextInt(0, 4)
                val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
                AdvancedMathProblem(
                    question = "det | $a $b $c |\n    | $d $e $f |\n    | $g $h $i |\n= ?",
                    answer = det.toString(),
                    topic = AdvancedMathTopic.MATRIX,
                    hint = "Expand along first row"
                )
            }
        }
    }

    private fun generateLogarithm(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val n = Random.nextInt(1, 7)
                val valPower = (1 shl n)
                AdvancedMathProblem(
                    question = "log₂($valPower) = ?",
                    answer = n.toString(),
                    topic = AdvancedMathTopic.LOGARITHM,
                    hint = "2 to what power gives val?"
                )
            }
            MathDifficulty.MEDIUM -> {
                val isBase3 = Random.nextBoolean()
                if (isBase3) {
                    val n = Random.nextInt(1, 5)
                    val v = 3.0.pow(n.toDouble()).toInt()
                    AdvancedMathProblem(
                        question = "log₃($v) = ?",
                        answer = n.toString(),
                        topic = AdvancedMathTopic.LOGARITHM,
                        hint = "base to what power gives val?"
                    )
                } else {
                    val n = Random.nextInt(1, 4)
                    val v = 5.0.pow(n.toDouble()).toInt()
                    AdvancedMathProblem(
                        question = "log₅($v) = ?",
                        answer = n.toString(),
                        topic = AdvancedMathTopic.LOGARITHM,
                        hint = "base to what power gives val?"
                    )
                }
            }
            MathDifficulty.HARD -> {
                val powers = listOf(2, 4, 8, 16)
                val a = powers.random()
                val b = powers.random()
                val logA = when (a) { 2 -> 1; 4 -> 2; 8 -> 3; else -> 4 }
                val logB = when (b) { 2 -> 1; 4 -> 2; 8 -> 3; else -> 4 }
                AdvancedMathProblem(
                    question = "log₂($a) + log₂($b) = ?",
                    answer = (logA + logB).toString(),
                    topic = AdvancedMathTopic.LOGARITHM,
                    hint = "log₂(a) + log₂(b) = log₂(a×b)"
                )
            }
            MathDifficulty.EXTREME -> {
                val a = Random.nextInt(1, 4)
                val b = Random.nextInt(1, 4)
                AdvancedMathProblem(
                    question = "log₁₀(10^$a × 10^$b) = ?",
                    answer = (a + b).toString(),
                    topic = AdvancedMathTopic.LOGARITHM,
                    hint = "log(x×y) = log(x) + log(y)"
                )
            }
        }
    }

    private fun generateProbability(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val n = Random.nextInt(4, 7)
                val r = Random.nextInt(2, 4)
                AdvancedMathProblem(
                    question = "C($n,$r) = ?",
                    answer = nCr(n, r).toString(),
                    topic = AdvancedMathTopic.PROBABILITY,
                    hint = "C(n,r) = n! / (r! × (n-r)!)"
                )
            }
            MathDifficulty.MEDIUM -> {
                val n = Random.nextInt(4, 7)
                val r = Random.nextInt(2, 4)
                AdvancedMathProblem(
                    question = "P($n,$r) = ?",
                    answer = nPr(n, r).toString(),
                    topic = AdvancedMathTopic.PROBABILITY,
                    hint = "P(n,r) = n! / (n-r)!"
                )
            }
            MathDifficulty.HARD -> {
                val n = Random.nextInt(4, 7)
                AdvancedMathProblem(
                    question = "$n! = ?",
                    answer = fact(n).toString(),
                    topic = AdvancedMathTopic.PROBABILITY,
                    hint = "n × (n-1) × ... × 1"
                )
            }
            MathDifficulty.EXTREME -> {
                val n = Random.nextInt(5, 8)
                val r = Random.nextInt(2, 4)
                AdvancedMathProblem(
                    question = "How many ways to choose $r from $n, then arrange them?",
                    answer = nPr(n, r).toString(),
                    topic = AdvancedMathTopic.PROBABILITY,
                    hint = "Choose then arrange = P(n,r)"
                )
            }
        }
    }

    private fun generateSequence(difficulty: MathDifficulty): AdvancedMathProblem {
        return when (difficulty) {
            MathDifficulty.EASY -> {
                val a = Random.nextInt(2, 11)
                val d = Random.nextInt(2, 9)
                AdvancedMathProblem(
                    question = "Next: $a, ${a + d}, ${a + 2 * d}, ${a + 3 * d}, ?",
                    answer = (a + 4 * d).toString(),
                    topic = AdvancedMathTopic.SEQUENCE,
                    hint = "Common difference = $d"
                )
            }
            MathDifficulty.MEDIUM -> {
                val a = Random.nextInt(1, 6)
                val d = Random.nextInt(1, 5)
                val n = Random.nextInt(4, 7)
                val sum = n * (2 * a + (n - 1) * d) / 2
                AdvancedMathProblem(
                    question = "Sum of first $n terms\nAP: $a, ${a + d}, ${a + 2 * d}, ...",
                    answer = sum.toString(),
                    topic = AdvancedMathTopic.SEQUENCE,
                    hint = "Sum = n(2a + (n-1)d) / 2"
                )
            }
            MathDifficulty.HARD -> {
                val a = Random.nextInt(2, 5)
                val r = Random.nextInt(2, 4)
                AdvancedMathProblem(
                    question = "Next: $a, ${a * r}, ${a * r * r}, ?",
                    answer = (a * r * r * r).toString(),
                    topic = AdvancedMathTopic.SEQUENCE,
                    hint = "Common ratio = $r"
                )
            }
            MathDifficulty.EXTREME -> {
                val a = Random.nextInt(1, 4)
                val r = 2
                val n = Random.nextInt(3, 6)
                val sum = a * ((1 shl n) - 1)
                AdvancedMathProblem(
                    question = "Sum of first $n terms\nGP: $a, ${a * r}, ${a * r * r}, ...",
                    answer = sum.toString(),
                    topic = AdvancedMathTopic.SEQUENCE,
                    hint = "Sum = a(rⁿ - 1)/(r - 1)"
                )
            }
        }
    }

    private fun fact(n: Int): Int = if (n <= 1) 1 else n * fact(n - 1)
    private fun nPr(n: Int, r: Int): Int = fact(n) / fact(n - r)
    private fun nCr(n: Int, r: Int): Int = nPr(n, r) / fact(r)
}
