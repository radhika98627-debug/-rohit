package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Calculation
import com.example.data.CalculatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.*

class CalculatorViewModel(private val repository: CalculatorRepository) : ViewModel() {

    private val _displayText = MutableStateFlow("")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _resultText = MutableStateFlow("")
    val resultText: StateFlow<String> = _resultText.asStateFlow()

    private val _isRadians = MutableStateFlow(true)
    val isRadians: StateFlow<Boolean> = _isRadians.asStateFlow()

    // Query data from Room reactively
    val history: StateFlow<List<Calculation>> = repository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onButtonClick(symbol: String) {
        when (symbol) {
            "C" -> {
                _displayText.value = ""
                _resultText.value = ""
            }
            "⌫" -> {
                val current = _displayText.value
                if (current.isNotEmpty()) {
                    // Check if we are deleting functions to make it cleaner
                    val endsWithFunc = listOf("sin(", "cos(", "tan(", "log(", "sqrt(").firstOrNull { current.endsWith(it) }
                    if (endsWithFunc != null) {
                        _displayText.value = current.dropLast(endsWithFunc.length)
                    } else if (current.endsWith("ln(")) {
                        _displayText.value = current.dropLast(3)
                    } else {
                        _displayText.value = current.dropLast(1)
                    }
                }
            }
            "=" -> {
                val expression = _displayText.value
                if (expression.isNotBlank()) {
                    evaluateAndSave(expression)
                }
            }
            "sin", "cos", "tan", "ln", "log", "sqrt" -> {
                _displayText.value += "$symbol("
            }
            "x^y" -> {
                _displayText.value += "^"
            }
            "DEG", "RAD" -> {
                _isRadians.value = !_isRadians.value
                // Re-evaluate if there's an active expression to show the updated Deg/Rad value
                val current = _displayText.value
                if (current.isNotBlank() && _resultText.value.isNotBlank() && _resultText.value != "Error") {
                    try {
                        val result = ExpressionEvaluator(_isRadians.value).evaluate(current)
                        _resultText.value = formatResult(result)
                    } catch (e: Exception) {
                        // Keep previous preview if evaluation fails
                    }
                }
            }
            else -> {
                _displayText.value += symbol
                // Preview calculations in real-time as they type!
                updateRealtimePreview()
            }
        }
    }

    private fun updateRealtimePreview() {
        val currentExpression = _displayText.value
        if (currentExpression.isBlank()) {
            _resultText.value = ""
            return
        }

        // Only preview if it ends with a digit, closing parenthesis, π, or e (likely valid end states)
        val lastChar = currentExpression.last()
        if (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '.') {
            try {
                val result = ExpressionEvaluator(_isRadians.value).evaluate(currentExpression)
                _resultText.value = formatResult(result)
            } catch (e: Exception) {
                // Squelch background preview errors during typing
            }
        }
    }

    private fun evaluateAndSave(expression: String) {
        viewModelScope.launch {
            try {
                val resultDouble = ExpressionEvaluator(_isRadians.value).evaluate(expression)
                val formattedStr = formatResult(resultDouble)
                
                _resultText.value = formattedStr
                
                // Save to historical Room database
                repository.insert(
                    Calculation(
                        expression = expression,
                        result = formattedStr
                    )
                )
                // Set the display to show the result so the user can chain operations
                _displayText.value = formattedStr
            } catch (e: ArithmeticException) {
                _resultText.value = "Error: ${e.message ?: "Division by zero"}"
            } catch (e: Exception) {
                _resultText.value = "Error"
            }
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        
        // Return rounded integer if possible
        if (value % 1.0 == 0.0 && value < 1e9 && value > -1e9) {
            return value.roundToLong().toString()
        }
        
        // Clean up output formatting for floating points
        val formatted = String.format("%.8f", value)
            .replace(Regex("0+$"), "") // Remove trailing zeros
            .replace(Regex("\\.$"), "") // Remove dangling decimal point
        return if (formatted == "-0") "0" else formatted
    }

    fun onHistoryItemClick(calculation: Calculation) {
        _displayText.value = calculation.expression
        _resultText.value = calculation.result
    }

    fun onDeleteCalculation(calculation: Calculation) {
        viewModelScope.launch {
            repository.delete(calculation)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Standard Factory method to instantiate ViewModel with repository without dynamic dependency injection framework
    class Factory(private val repository: CalculatorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
                return CalculatorViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Robust mathematical recursive descent parser for expressions.
 * Supports basic operators: +, -, *, /
 * Multi-char functions: sin, cos, tan, ln, log, sqrt
 * Nested parentheses and operator priorities.
 */
class ExpressionEvaluator(private val isRadians: Boolean) {
    fun evaluate(expression: String): Double {
        // Preprocess user-friendly characters to computer math representation
        var prepared = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", Math.PI.toString())
            .replace("e", Math.E.toString())

        // Safety auto-close remaining unclosed parentheses
        val openCount = prepared.count { it == '(' }
        val closeCount = prepared.count { it == ')' }
        if (openCount > closeCount) {
            prepared += ")".repeat(openCount - closeCount)
        }

        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < prepared.length) prepared[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
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
                if (pos < prepared.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else break
                }
                return x
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor // division
                    } else break
                }
                return x
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return +parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = this.pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis")
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = prepared.substring(startPos, this.pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = prepared.substring(startPos, this.pos)
                    if (eat('('.code)) {
                        val arg = parseExpression()
                        if (!eat(')'.code)) throw RuntimeException("Missing closing parenthesis after $func")
                        x = when (func) {
                            "sin" -> if (isRadians) sin(arg) else sin(Math.toRadians(arg))
                            "cos" -> if (isRadians) cos(arg) else cos(Math.toRadians(arg))
                            "tan" -> if (isRadians) tan(arg) else tan(Math.toRadians(arg))
                            "ln" -> {
                                if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                                ln(arg)
                            }
                            "log" -> {
                                if (arg <= 0) throw ArithmeticException("Logarithm of non-positive number")
                                log10(arg)
                            }
                            "sqrt" -> {
                                if (arg < 0) throw ArithmeticException("Square root of a negative description")
                                sqrt(arg)
                            }
                            else -> throw RuntimeException("Unknown function: $func")
                        }
                    } else {
                        throw RuntimeException("Expected '(' after function name")
                    }
                } else {
                    throw RuntimeException("Unexpected character: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                return x
            }
        }.parse()
    }
}
