---
applyTo: '**'
---
# Cursor Rules for Porting iOS Swift App → Android Kotlin/Jetpack Compose

general:
  - Do not invent code or features not explicitly provided in context.
  - Only translate or implement code that I provide, respecting the iOS app structure.
  - If information is missing or unclear, stop and ask for clarification.
  - Always follow Android conventions: lowercase, singular package names.
  - Do not optimize, simplify, or add animations unless explicitly asked. If the original code has animations, do not replicate them.
  - Assume the project uses MVVM: model → service → viewmodel → view.
  - Very importante: triple check all translations for accuracy and idiomatic Kotlin usage.

workflow:
  # Models
  - When I give you a Swift `struct` or `class`, translate it into a Kotlin `data class`.
  - Use Kotlinx Serialization for JSON parsing (`@Serializable`).
  - Keep property names identical unless conversion requires `camelCase`.

  # Services
  - When I provide Swift networking/service code, convert it into a Kotlin `service/` class.
  - Use OkHttp for WebSockets and Retrofit (if REST).
  - Keep method names and responsibilities as close as possible.
  - Do not add retries, logging, or background scheduling unless asked.

  # ViewModels
  - When I provide Swift `ObservableObject` or `@Published` logic, map it to `ViewModel` with `MutableStateFlow` / `StateFlow`.
  - Keep the state variables and functions names aligned with Swift version.
  - Do not introduce repositories or additional architecture unless asked.

  # Views
  - When I provide SwiftUI `View`, translate into Jetpack Compose `@Composable`.
  - Use only basic Compose primitives (`Column`, `Row`, `Text`, `Button`, etc.).
  - All state must come from the corresponding ViewModel.

validation:
  - Always explain what you translated and why.
  - If translation requires a library or Gradle dependency, list it clearly.
  - If something doesn’t exist in Kotlin/Compose, stop and ask for clarification instead of guessing.

context_handling:
  - Assume I will paste Swift code into the chat context.
  - Translate only what I give you (no assumptions about other parts of the app).
  - If code references something not in context, mark it as a TODO instead of inventing it.
