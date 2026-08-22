# Kotlin & Android Development Instructions

These are mandatory project rules for any AI tool (Claude Code, Cursor, Antigravity, etc.)
generating or modifying Kotlin/Android code in this repo. Follow them by default —
do not ask whether to apply them, just apply them. If a request conflicts with a rule
below, follow the rule unless the user explicitly overrides it.

---

## 1. Architecture

- Use **MVVM** with a light **Clean Architecture** split: `data/` (repositories, DB, network),
  `domain/` (use cases, models — optional for small features), `ui/` (Composables, ViewModels).
- **ViewModel** owns UI state. Expose state as `StateFlow<UiState>`, never raw `MutableStateFlow`
  from outside the ViewModel. Composables collect with `collectAsStateWithLifecycle()`
  (not `collectAsState()` — the lifecycle-aware version avoids collecting while backgrounded).
- **Repositories** are the single source of truth. Composables and ViewModels never call
  Retrofit/Room/DataStore directly.
- Use **Hilt** for dependency injection. Every ViewModel gets `@HiltViewModel` +
  constructor injection. No manual singletons, no static service locators.
- One-way data flow: state flows down (ViewModel → UI), events flow up (UI → ViewModel via
  function calls, not callbacks stored in state).

## 2. Jetpack Compose Performance Rules (most lag comes from here)

- **Every lambda passed into a composable must be stable.** Do not create new lambda
  instances inline inside a recomposing scope (e.g. inside `LazyColumn` items). Hoist
  them, or wrap with `remember(key1, key2) { ... }`.
- **Always provide a `key` in `LazyColumn`/`LazyRow`/`LazyGrid` items**, using a stable
  unique ID from the data model — never the list index.
- **Never do heavy work directly in composable body.** No object construction, formatting,
  sorting, filtering, or I/O inline. Wrap in `remember { }` (for pure derived state) or
  `LaunchedEffect` (for side effects / suspend calls).
- **Use `derivedStateOf`** when a value is computed from other state and read during
  composition/scroll (e.g. "is scrolled past item 5") — prevents recomposition on every
  pixel of scroll delta.
- **Mark data classes used in Compose state as `@Immutable` or `@Stable`** when applicable,
  and prefer `List` over `MutableList`/`ArrayList` in state models (Compose can't guarantee
  stability of mutable collections, which disables skipping).
- **Avoid deeply nested layouts.** Flatten `Box`/`Column`/`Row` nesting where possible;
  use `ConstraintLayout` for genuinely complex layouts instead of 5+ levels of nesting.
- **Do not put `Modifier` chains with `.clickable`, `.padding`, etc. behind conditional
  branches that change on every recomposition** — this invalidates modifier caching.
- Use **Compose compiler metrics / stability reports** or the **Layout Inspector recomposition
  counts** to verify fixes, not guesswork. If unsure whether a change helped, say so and
  suggest checking recomposition counts rather than assuming.

## 3. Threading & Coroutines

- **Never perform I/O (network, disk, DB) on the main thread**, even "quick" reads.
- All repository/data-layer suspend functions must specify or inherit `Dispatchers.IO`
  (Room and Retrofit-with-suspend already do this correctly — don't wrap them redundantly,
  but raw file/Socket work needs explicit `withContext(Dispatchers.IO)`).
- Launch coroutines from `viewModelScope` in ViewModels, never `GlobalScope`.
- Click handlers must return immediately (< 1 frame). If the action needs I/O, launch a
  coroutine and let the UI show a loading/pressed state instantly.
- Avoid unstructured concurrency: no floating `CoroutineScope()` without a clear owner
  and cancellation policy.

## 4. Lists, Images, and Media

- Use **Coil** (or Glide) for image loading with memory + disk cache enabled by default —
  never decode bitmaps manually in a composable.
- Specify explicit `size`/`Modifier.size()` on image requests to avoid loading
  full-resolution images into small thumbnails.
- For audio/video playback, use **Media3 (ExoPlayer)** with a foreground `MediaSessionService`
  for background playback — never try to keep playback alive via a WebView, plain Activity
  lifecycle, or a non-foreground Service (Android will kill it).
- Use `LazyColumn`/`LazyGrid` with `contentType` specified when items have mixed view types,
  so Compose can reuse compositions correctly.

## 5. State & Data Modeling

- UI state is a single sealed class/data class per screen (`Loading`, `Success(data)`, `Error`),
  not a scatter of independent booleans (`isLoading`, `hasError`, `data` as separate fields
  that can contradict each other).
- Never expose nullable "loading" flags that require the UI to infer state — make state
  explicit and exhaustive (`when` over a sealed class, not chained `if`s).

## 6. Database & Networking

- **Room**: all queries are `suspend fun` or return `Flow`, never blocking calls on main thread.
  Add indices for frequently queried columns. Use `@Transaction` for multi-table writes.
- **Retrofit/Ktor**: use `suspend fun` endpoints, not callback-based or `.execute()` blocking
  calls. Centralize error handling (network failure, timeout, 4xx/5xx) in a repository-level
  wrapper (e.g. a `Result`/sealed `ApiResult` type), not scattered try/catch per call site.
- Cache network responses appropriately (ETags, or a local DB as source-of-truth with
  network as a refresh trigger) rather than re-fetching on every screen visit.

## 7. Code Quality / Anti-Patterns to Avoid

AI tools commonly default to these — explicitly avoid them:

- Business logic inside Composables (put it in ViewModel/UseCase).
- `!!` non-null assertions as a shortcut — handle nullability explicitly.
- Catching `Exception` broadly and silently swallowing it — log and handle specific
  exceptions, or propagate as a typed error.
- Hardcoded strings/dimensions instead of `strings.xml` / `Dp` constants / theme values.
- Recreating ViewModels or repositories manually instead of using Hilt-provided instances.
- Passing `Context` into ViewModels or repositories (leaks Activity context) — use
  `@ApplicationContext` only when unavoidable, prefer passing data instead.
- Deep composable parameter lists (10+ params) — group related params into a data class.

## 8. Build & Tooling

- Keep Gradle version catalogs (`libs.versions.toml`) up to date; don't hardcode dependency
  versions inline in module `build.gradle.kts` files.
- Enable Compose compiler metrics/stability reports in debug builds when diagnosing
  performance issues (`-P plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=...`).
- Use `minSdk`/`targetSdk` consistent with the project's existing configuration — don't
  silently change these.

## 9. When Generating New Code

- Before writing a new screen/feature, state which layer each new file belongs to
  (ViewModel / Repository / UseCase / UI) rather than dumping everything into one file.
- If a requested feature implies background work (playback, sync, uploads), default to
  the correct Android component (`Service`, `WorkManager`) instead of trying to keep it
  alive via the UI layer.
- If performance-sensitive code is being touched (lists, scroll, playback), call out
  the specific rules above being applied, so it's easy to review.

---

*Keep this file updated as the project's conventions evolve — treat it as the source of
truth for how code in this repo should be written, not just a one-time checklist.*
