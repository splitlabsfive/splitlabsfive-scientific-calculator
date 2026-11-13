package com.example.scicalculator.calculator

enum class AngleMode { DEG, RAD }

object Evaluator {

    private sealed interface Token {
        data class Number(val text: String) : Token
        data class Constant(val value: Double) : Token
        data class Function(val name: String) : Token
        data class Operator(val symbol: Char) : Token
        data object LeftParen : Token
        data object RightParen : Token
        data object Factorial : Token
    }

    private val functionNames = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan", "ln", "log", "sqrt"
    )

    private val precedence = mapOf(
        '+' to 1, '-' to 1,
        '×' to 2, '÷' to 2, '*' to 2, '/' to 2,
        'u' to 3,
        '^' to 4,
    )

    private val rightAssociative = setOf('^', 'u')

    fun evaluate(expression: String, angleMode: AngleMode): Double {
        val tokens = tokenize(expression)
        if (tokens.isEmpty()) throw IllegalArgumentException("Empty expression")
        val rpn = toPostfix(tokens)
        return evalPostfix(rpn, angleMode)
    }

    private fun tokenize(expression: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val s = expression
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    var dots = 0
                    while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                        if (s[i] == '.') dots++
                        i++
                    }
                    if (dots > 1 || (i - start == 1 && s[start] == '.')) {
                        throw IllegalArgumentException("Invalid number")
                    }
                    tokens.add(Token.Number(s.substring(start, i)))
                }

                c == 'π' -> { tokens.add(Token.Constant(Math.PI)); i++ }
                c == '√' -> { tokens.add(Token.Function("sqrt")); i++ }

                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter()) i++
                    val word = s.substring(start, i).lowercase()
                    when (word) {
                        "pi" -> tokens.add(Token.Constant(Math.PI))
                        "e" -> tokens.add(Token.Constant(Math.E))
                        in functionNames -> tokens.add(Token.Function(word))
                        else -> throw IllegalArgumentException("Unknown symbol: $word")
                    }
                }

                c == '(' -> { tokens.add(Token.LeftParen); i++ }
                c == ')' -> { tokens.add(Token.RightParen); i++ }
                c == '!' -> { tokens.add(Token.Factorial); i++ }

                c == '+' || c == '-' || c == '*' || c == '/' ||
                    c == '^' || c == '×' || c == '÷' -> {
                    val isUnary = c == '-' &&
                        (tokens.isEmpty() || tokens.last() is Token.Operator ||
                            tokens.last() is Token.LeftParen)
                    tokens.add(Token.Operator(if (isUnary) 'u' else c))
                    i++
                }

                else -> throw IllegalArgumentException("Unexpected character: $c")
            }
        }
        insertImplicitMultiplication(tokens)
        return tokens
    }

    private fun insertImplicitMultiplication(tokens: MutableList<Token>) {
        var i = 1
        while (i < tokens.size) {
            val prev = tokens[i - 1]
            val cur = tokens[i]
            val prevEndsValue = prev is Token.Number || prev is Token.Constant ||
                prev is Token.RightParen || prev is Token.Factorial
            val curStartsValue = cur is Token.Number || cur is Token.Constant ||
                cur is Token.Function || cur is Token.LeftParen
            if (prevEndsValue && curStartsValue) {
                tokens.add(i, Token.Operator('×'))
                i++
            }
            i++
        }
    }

    private fun toPostfix(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = mutableListOf<Token>()
        var openParens = 0

        for (token in tokens) {
            when (token) {
                is Token.Number, is Token.Constant -> output.add(token)
                is Token.Factorial -> output.add(token)
                is Token.Function -> stack.add(token)
                is Token.Operator -> {
                    while (stack.lastOrNull() is Token.Function) {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                    if (token.symbol == 'u') { stack.add(token); continue }
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.Operator) {
                            val topPrec = precedence.getValue(top.symbol)
                            val curPrec = precedence.getValue(token.symbol)
                            val popIt = topPrec > curPrec ||
                                (topPrec == curPrec && token.symbol !in rightAssociative)
                            if (popIt) { output.add(stack.removeAt(stack.size - 1)); continue }
                        }
                        break
                    }
                    stack.add(token)
                }
                Token.LeftParen -> { stack.add(token); openParens++ }
                Token.RightParen -> {
                    if (openParens == 0) throw IllegalArgumentException("Unbalanced parenthesis")
                    while (stack.isNotEmpty() && stack.last() !is Token.LeftParen) {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                    if (stack.isEmpty()) throw IllegalArgumentException("Unbalanced parenthesis")
                    stack.removeAt(stack.size - 1)
                    openParens--
                    if (stack.lastOrNull() is Token.Function) {
                        output.add(stack.removeAt(stack.size - 1))
                    }
                }
            }
        }

        if (openParens != 0) throw IllegalArgumentException("Unbalanced parenthesis")
        while (stack.isNotEmpty()) {
            val top = stack.removeAt(stack.size - 1)
            if (top is Token.LeftParen) throw IllegalArgumentException("Unbalanced parenthesis")
            output.add(top)
        }
        return output
    }

    private fun evalPostfix(rpn: List<Token>, angleMode: AngleMode): Double {
        val stack = mutableListOf<Double>()
        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.add(token.text.toDouble())
                is Token.Constant -> stack.add(token.value)
                is Token.Factorial -> stack.add(pop(stack).factorial())
                is Token.Operator -> {
                    if (token.symbol == 'u') {
                        val v = pop(stack)
                        stack.add(-v)
                    } else {
                        val b = pop(stack)
                        val a = pop(stack)
                        stack.add(applyBinary(a, token.symbol, b))
                    }
                }
                is Token.Function -> {
                    val v = pop(stack)
                    stack.add(applyFunction(token.name, v, angleMode))
                }
                else -> throw IllegalArgumentException("Malformed expression")
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("Malformed expression")
        val result = stack[0]
        if (result.isNaN() || result.isInfinite()) {
            throw ArithmeticException("Result is not a finite number")
        }
        return result
    }

    private fun pop(stack: MutableList<Double>): Double =
        if (stack.isEmpty()) throw IllegalArgumentException("Malformed expression")
        else stack.removeAt(stack.size - 1)

    private fun applyBinary(a: Double, op: Char, b: Double): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*', '×' -> a * b
        '/', '÷' -> {
            if (b == 0.0) throw ArithmeticException("Division by zero")
            a / b
        }
        '^' -> Math.pow(a, b)
        else -> throw IllegalArgumentException("Unknown operator: $op")
    }

    private fun applyFunction(name: String, v: Double, mode: AngleMode): Double = when (name) {
        "sin" -> Math.sin(toRadians(v, mode))
        "cos" -> Math.cos(toRadians(v, mode))
        "tan" -> Math.tan(toRadians(v, mode))
        "asin" -> fromRadians(Math.asin(v), mode)
        "acos" -> fromRadians(Math.acos(v), mode)
        "atan" -> fromRadians(Math.atan(v), mode)
        "ln" -> {
            if (v <= 0) throw ArithmeticException("ln requires a positive argument")
            Math.log(v)
        }
        "log" -> {
            if (v <= 0) throw ArithmeticException("log requires a positive argument")
            Math.log10(v)
        }
        "sqrt" -> {
            if (v < 0) throw ArithmeticException("sqrt requires a non-negative argument")
            Math.sqrt(v)
        }
        else -> throw IllegalArgumentException("Unknown function: $name")
    }

    private fun toRadians(v: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG) Math.toRadians(v) else v

    private fun fromRadians(v: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG) Math.toDegrees(v) else v

    private fun Double.factorial(): Double {
        if (this < 0 || this != Math.floor(this)) {
            throw ArithmeticException("Factorial requires a non-negative integer")
        }
        if (this > 170) throw ArithmeticException("Factorial result too large")
        var result = 1.0
        var i = 2.0
        while (i <= this) { result *= i; i++ }
        return result
    }

    fun formatResult(value: Double): String {
        val rounded = Math.round(value * 1e12) / 1e12
        return if (rounded == Math.floor(rounded) && !rounded.isInfinite() &&
            Math.abs(rounded) < 1e15
        ) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}
