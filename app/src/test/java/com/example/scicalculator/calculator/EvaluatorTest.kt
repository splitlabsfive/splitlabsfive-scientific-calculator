package com.example.scicalculator.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.abs

class EvaluatorTest {

    private fun eval(expr: String, mode: AngleMode = AngleMode.DEG): Double =
        Evaluator.evaluate(expr, mode)

    private fun assertApprox(expected: Double, actual: Double, delta: Double = 1e-9) {
        assertEquals(expected, actual, delta)
    }

    @Test
    fun basicArithmetic() {
        assertApprox(7.0, eval("3+4"))
        assertApprox(-1.0, eval("3-4"))
        assertApprox(12.0, eval("3×4"))
        assertApprox(2.5, eval("5÷2"))
        assertApprox(6.0, eval("2*3"))
        assertApprox(0.5, eval("1/2"))
    }

    @Test
    fun precedenceAndAssociativity() {
        assertApprox(14.0, eval("2+3×4"))
        assertApprox(20.0, eval("(2+3)×4"))
        assertApprox(8.0, eval("2^3"))
        assertApprox(512.0, eval("2^3^2"))
        assertApprox(-4.0, eval("-2^2"))
        assertApprox(0.5, eval("2^-1"))
    }

    @Test
    fun unaryMinus() {
        assertApprox(-5.0, eval("-5"))
        assertApprox(-9.0, eval("-3×3"))
        assertApprox(3.0, eval("--3"))
        assertApprox(-7.0, eval("2-3-6"))
    }

    @Test
    fun parentheses() {
        assertApprox(10.0, eval("(2+3)×2"))
        assertApprox(2.0, eval("((2))"))
        assertApprox(9.0, eval("(1+2)^(1+1)"))
    }

    @Test
    fun implicitMultiplication() {
        assertApprox(6.0, eval("2(3)"))
        assertApprox(6.283185307, eval("2π"), 1e-8)
        assertApprox(5.436563657, eval("2e"), 1e-8)
        assertApprox(6.0, eval("(2)(3)"))
        assertApprox(120.0, eval("5!"))
        assertApprox(96.0, eval("4!(2+2)"))
    }

    @Test
    fun constants() {
        assertApprox(Math.PI, eval("π"))
        assertApprox(Math.E, eval("e"))
        assertApprox(Math.PI, eval("pi"))
    }

    @Test
    fun trigDegrees() {
        assertApprox(0.0, eval("sin(0)"))
        assertApprox(1.0, eval("sin(90)"), 1e-9)
        assertApprox(0.5, eval("sin(30)"), 1e-9)
        assertApprox(-1.0, eval("cos(180)"), 1e-9)
        assertApprox(1.0, eval("tan(45)"), 1e-9)
    }

    @Test
    fun trigRadians() {
        assertApprox(0.8414709848, eval("sin(1)", AngleMode.RAD), 1e-9)
        assertApprox(0.5403023059, eval("cos(1)", AngleMode.RAD), 1e-9)
        assertApprox(0.7853981634, eval("atan(1)", AngleMode.RAD), 1e-9)
    }

    @Test
    fun inverseTrigDegrees() {
        assertApprox(30.0, eval("asin(0.5)"), 1e-9)
        assertApprox(60.0, eval("acos(0.5)"), 1e-9)
        assertApprox(45.0, eval("atan(1)"), 1e-9)
    }

    @Test
    fun logsAndRoots() {
        assertApprox(2.0, eval("log(100)"))
        assertApprox(2.302585093, eval("ln(10)"), 1e-9)
        assertApprox(9.0, eval("√(81)"))
        assertApprox(5.0, eval("√25"))
        assertApprox(11.0, eval("√4+9"))
        assertApprox(18.0, eval("√81×2"))
        assertApprox(4.0, eval("2^2"))
    }

    @Test
    fun factorial() {
        assertApprox(1.0, eval("0!"))
        assertApprox(120.0, eval("5!"))
        assertApprox(720.0, eval("(3!)!"))
    }

    @Test
    fun complexExpressions() {
        assertApprox(11.0, eval("2(3)+5"))
        assertApprox(1.0, eval("sin(30)^2+cos(30)^2"), 1e-9)
        assertApprox(1024.0, eval("2^√(400÷4)"))
    }

    @Test
    fun divisionByZeroThrows() {
        assertThrows(ArithmeticException::class.java) { eval("1÷0") }
    }

    @Test
    fun unbalancedParenthesesThrow() {
        assertThrows(IllegalArgumentException::class.java) { eval("(1+2") }
        assertThrows(IllegalArgumentException::class.java) { eval("1+2)") }
    }

    @Test
    fun invalidDomainThrows() {
        assertThrows(ArithmeticException::class.java) { eval("ln(0)") }
        assertThrows(ArithmeticException::class.java) { eval("√(-1)") }
        assertThrows(ArithmeticException::class.java) { eval("(-0.5)!") }
    }

    @Test
    fun malformedExpressionThrows() {
        assertThrows(IllegalArgumentException::class.java) { eval("2+") }
        assertThrows(IllegalArgumentException::class.java) { eval("") }
        assertThrows(IllegalArgumentException::class.java) { eval("hello") }
    }

    @Test
    fun formatResult() {
        assertEquals("3", Evaluator.formatResult(3.0000000000001))
        assertEquals("0.5", Evaluator.formatResult(0.5))
        assertEquals("142", Evaluator.formatResult(142.0))
    }
}
