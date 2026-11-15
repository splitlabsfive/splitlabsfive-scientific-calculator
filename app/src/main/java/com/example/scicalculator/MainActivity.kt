package com.example.scicalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scicalculator.calculator.AngleMode
import com.example.scicalculator.calculator.Evaluator
import com.example.scicalculator.calculator.HistoryEntry
import com.example.scicalculator.calculator.HistoryStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val historyStore = HistoryStore(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalculatorScreen(historyStore)
                }
            }
        }
    }
}

private data class Key(
    val label: String,
    val insert: String? = null,
    val action: (CalculatorState) -> Unit = { it.expression += it.pendingInsert },
    val tone: KeyTone = KeyTone.Digit
) {
    constructor(label: String, insert: String, tone: KeyTone) : this(label, insert, { s ->
        s.expression += insert
    }, tone)
}

private enum class KeyTone { Digit, Operator, Function, Control, Equals }

private class CalculatorState {
    var expression by mutableStateOf("")
    var result by mutableStateOf("")
    var angleMode by mutableStateOf(AngleMode.DEG)
    var secondFunc by mutableStateOf(false)
    var ans by mutableStateOf("0")
    var pendingInsert: String = ""
    var history by mutableStateOf(listOf<HistoryEntry>())
    var showHistory by mutableStateOf(false)
    var historyStore: HistoryStore? = null

    fun clear() {
        expression = ""
        result = ""
    }

    fun backspace() {
        if (expression.isNotEmpty()) expression = expression.dropLast(1)
    }

    fun equalsPressed() {
        if (expression.isBlank()) return
        val outcome = runCatching {
            Evaluator.formatResult(Evaluator.evaluate(expression, angleMode))
        }
        result = outcome.getOrElse { "Error" }
        if (outcome.isSuccess) {
            ans = result
            history = listOf(HistoryEntry(expression, result)) + history
            historyStore?.save(history)
            expression = result
        }
    }

    fun preview(): String? {
        if (expression.isBlank()) return null
        return runCatching {
            Evaluator.formatResult(Evaluator.evaluate(expression, angleMode))
        }.getOrNull()
    }
}

@Composable
fun CalculatorScreen(historyStore: HistoryStore? = null) {
    val state = remember {
        CalculatorState()
            .also { it.history = historyStore?.load() ?: emptyList() }
            .also { it.historyStore = historyStore }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(8.dp)
    ) {
        ModeBar(state)
        Display(state, modifier = Modifier.weight(1f))
        if (state.showHistory) {
            HistoryPanel(state)
        }
        Keypad(state)
    }
}

@Composable
private fun ModeBar(state: CalculatorState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        TextButton(onClick = { state.secondFunc = !state.secondFunc }) {
            Text(
                if (state.secondFunc) "2nd ON" else "2nd",
                color = if (state.secondFunc) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
        TextButton(onClick = {
            state.angleMode =
                if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        }) {
            Text(
                if (state.angleMode == AngleMode.DEG) "DEG" else "RAD",
                color = MaterialTheme.colorScheme.primary
            )
        }
        TextButton(onClick = { state.showHistory = !state.showHistory }) {
            Text(
                if (state.showHistory) "Hide history" else "History",
                color = if (state.showHistory) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun HistoryPanel(state: CalculatorState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Clear",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable {
                    state.history = emptyList()
                    state.historyStore?.save(state.history)
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (state.history.isEmpty()) {
                Text(
                    text = "No calculations yet",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            state.history.forEach { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { state.expression = entry.result }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = entry.expression,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                    Text(
                        text = "= ${entry.result}",
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun Display(state: CalculatorState, modifier: Modifier = Modifier) {
    val preview = state.preview()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = state.expression.ifEmpty { "0" },
            fontSize = 28.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        )
        Text(
            text = preview ?: state.result,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            color = if (state.result == "Error") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun Keypad(state: CalculatorState) {
    val second = state.secondFunc
    val keys: List<List<Key>> = listOf(
        listOf(
            Key("2nd", action = { state.secondFunc = !state.secondFunc }, tone = KeyTone.Control),
            Key(if (state.angleMode == AngleMode.DEG) "DEG" else "RAD", action = {
                state.angleMode =
                    if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
            }, tone = KeyTone.Control),
            Key("(", insert = "(", tone = KeyTone.Operator),
            Key(")", insert = ")", tone = KeyTone.Operator),
            Key("C", action = { state.clear() }, tone = KeyTone.Control),
        ),
        listOf(
            Key(if (second) "sin⁻¹" else "sin", insert = if (second) "asin(" else "sin(",
                tone = KeyTone.Function),
            Key(if (second) "cos⁻¹" else "cos", insert = if (second) "acos(" else "cos(",
                tone = KeyTone.Function),
            Key(if (second) "tan⁻¹" else "tan", insert = if (second) "atan(" else "tan(",
                tone = KeyTone.Function),
            Key("^", insert = "^", tone = KeyTone.Operator),
            Key("÷", insert = "÷", tone = KeyTone.Operator),
        ),
        listOf(
            Key("ln", insert = "ln(", tone = KeyTone.Function),
            Key("log", insert = "log(", tone = KeyTone.Function),
            Key(if (second) "x²" else "√", insert = if (second) "^2" else "√(",
                tone = KeyTone.Function),
            Key("×", insert = "×", tone = KeyTone.Operator),
            Key("⌫", action = { state.backspace() }, tone = KeyTone.Control),
        ),
        listOf(
            Key("7", "7"), Key("8", "8"), Key("9", "9"),
            Key("−", insert = "-", tone = KeyTone.Operator),
            Key("π", insert = "π", tone = KeyTone.Function),
        ),
        listOf(
            Key("4", "4"), Key("5", "5"), Key("6", "6"),
            Key("+", insert = "+", tone = KeyTone.Operator),
            Key("e", insert = "e", tone = KeyTone.Function),
        ),
        listOf(
            Key("1", "1"), Key("2", "2"), Key("3", "3"),
            Key(".", "."), Key("!", insert = "!", tone = KeyTone.Function),
        ),
        listOf(
            Key("ans", insert = state.ans, tone = KeyTone.Function),
            Key("0", "0"),
            Key("=", action = { state.equalsPressed() }, tone = KeyTone.Equals),
        ),
    )

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        keys.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                rowKeys.forEach { key ->
                    KeyButton(key, modifier = Modifier.weight(1f), state = state)
                }
            }
        }
    }
}

@Composable
private fun KeyButton(key: Key, modifier: Modifier, state: CalculatorState) {
    val container = when (key.tone) {
        KeyTone.Digit -> MaterialTheme.colorScheme.secondaryContainer
        KeyTone.Operator -> MaterialTheme.colorScheme.primaryContainer
        KeyTone.Function -> MaterialTheme.colorScheme.tertiaryContainer
        KeyTone.Control -> MaterialTheme.colorScheme.surfaceVariant
        KeyTone.Equals -> MaterialTheme.colorScheme.primary
    }
    val content = when (key.tone) {
        KeyTone.Equals -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(modifier = modifier) {
        Button(
            onClick = {
                state.pendingInsert = key.insert ?: ""
                key.action(state)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = key.label, fontSize = 18.sp, maxLines = 1)
        }
    }
}
