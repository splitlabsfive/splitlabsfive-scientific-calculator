# Sci Calc — Android Scientific Calculator

A simple scientific calculator for Android, built with Kotlin and Jetpack Compose (Material 3).

## Features

- Basic arithmetic: `+ − × ÷`, parentheses, unary minus, decimals
- Powers: `^` (right-associative), square via 2nd-function `x²`
- Trigonometry: `sin`, `cos`, `tan` with DEG/RAD toggle; inverse functions via `2nd`
- Logs: `ln`, `log` (base 10)
- Roots: `√`
- Constants: `π`, `e`
- Postfix factorial: `!`
- Implicit multiplication: `2π`, `2(3)`, `4!(2+2)`, `(2)(3)`
- Live result preview while typing, `ans` key for the previous result
- Friendly errors instead of crashes (division by zero, unbalanced parentheses, invalid domains)

## Project layout

```
app/src/main/java/com/example/scicalculator/
├── MainActivity.kt                  # Compose UI (display, mode bar, keypad)
└── calculator/
    └── Evaluator.kt                 # tokenizer + shunting-yard + RPN evaluation
app/src/test/java/com/example/scicalculator/calculator/
└── EvaluatorTest.kt                 # JUnit tests for the evaluator
```

## Building

**CI:** every push to `main` runs [.github/workflows/build-apk.yml](.github/workflows/build-apk.yml),
which executes the unit tests, builds the debug APK, and uploads it as the
`calculator-debug-apk` artifact — download it from the repository's **Actions** tab.

Open the project in Android Studio (Ladybug or newer), or run:

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # evaluator unit tests
```

Requirements: JDK 17, Android SDK 35. `minSdk` is 24 (Android 7.0+).

Note: the Gradle wrapper JAR is not checked in; Android Studio will generate/sync it,
or run `gradle wrapper --gradle-version 8.7` once if you build from the CLI.

## How evaluation works

`Evaluator.kt` implements a classic three-stage pipeline:

1. **Tokenizer** — numbers, constants (`π`, `e`), functions, operators, parens, `!`;
   inserts implicit `×` where a value token follows another value-ending token.
2. **Shunting-yard** — converts to postfix (RPN). Unary minus (`u`) never pops `^`
   so that `-2^2 = -4` and `2^-1 = 0.5` both behave correctly; paren-less functions
   bind only to their immediate operand (`√4+9 = 11`).
3. **RPN evaluator** — applies DEG/RAD conversion for trig functions and validates
   domains (`ln`, `log`, `√`, factorial), throwing descriptive errors otherwise.
