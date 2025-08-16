# Password Strengthener – Plan

## Scope & Constraints
- In-memory only for passwords and candidate history. No Room/DB. History is lost when the app is killed.
- Lightweight Service Locator (no Hilt).
- Use existing :smollm module (`SmolLM`, `GGUFReader`).
- Static weak passwords list in Kotlin (50 entries); on app open pick random 10.
- Async zxcvbn scoring at app launch; show a visual placeholder until ready.
- Hide “Improved:” label until an improved candidate exists.

## Architecture
- Single Activity (`PasswordStrengthenActivity`) hosting a Compose `NavHost`.
- Service Locator `AppServices` provides:
  - `LocalModelClient` (wraps `SmolLM`).
  - `PasswordScoringService` (zxcvbn wrapper; stub initially).
  - `WeakPasswordProvider` (returns 10 random from static list).
- In-memory repositories/state holders:
  - `PasswordSessionRepository` – holds the session’s list, candidates, selection and row states.
  - `ModelRepository` – SharedPreferences-backed model path only.

## Data Model (in-memory)
- `PasswordItemUi`
  - `id: String`
  - `original: String`
  - `score: ScoreState` → `Loading | Value(0..4)`
  - `currentImproved: String?` (null = not improved yet)
  - `state: RowState` → `Idle | Waiting | Loading | Success(timestamp)`
  - `candidates: List<CandidateUi>`
- `CandidateUi(id, value, score0to4, isSelected)`

## Dependencies
- Add Navigation-Compose
- Add Lifecycle ViewModel + Compose integration
- Add zxcvbn password strength lib (later; start with stub)
- Keep existing Compose + Material3 + :smollm

## Screens & Navigation
- `ModelPickerScreen`
  - Use SAF to pick `.gguf`. Persist permission. Copy to `filesDir`.
  - Save absolute path/filename to SharedPreferences.
  - Load model via `LocalModelClient`. On success → `DashboardScreen`.
- `DashboardScreen`
  - Header: Title + selected model name; top-right icon to reselect.
  - List: 10 random weak passwords.
    - Original
    - Score chip: “computing…” until ready; then color-coded score 0..4
    - Improve button
    - Right-corner indicator: ⏳ waiting → 🔄 loading → ✅ success (auto-hide 3s)
    - Crossfade when `currentImproved` appears
    - Hide “Improved:” until `currentImproved != null`
  - Bottom: “Improve All Passwords” (disabled while running)
- `HistoryBottomSheet`
  - Shows in-memory candidates with scores
  - Allows selecting a candidate → updates row selection

## Flows
- App launch
  - If no `model_path_abs` in SharedPreferences → `ModelPickerScreen`; else → `DashboardScreen`.
  - Create session list: random 10 from `StaticWeakPasswords.kt`.
  - Compute scores asynchronously with limited concurrency.
- Improve one
  - Set row to ⏳→🔄
  - Ensure model is loaded; reload if needed (verify handle).
  - Stream response via `LocalModelClient.getResponseAsFlow()`; filter disallowed suffixes.
  - On finish: compute score; append candidate to in-memory list; set as `currentImproved`; show ✅ for 3s.
- Improve all
  - Disable global button
  - Sequentially process each row and update states; show Snackbar when done
- Reselect model
  - Top-right icon → `ModelPickerScreen` and reload client

## Scoring
- `PasswordScoringService` wraps zxcvbn.
- Async score computation at launch; chip shows “computing…” then 0..4 colored.
- Recompute when a candidate is generated or selected.

## Animations & UX
- Crossfade for `currentImproved` appearance.
- Indicator transitions ⏳→🔄→✅ with 3s auto-hide.
- Snackbars/Toasts for global events and errors.

## Milestones
- M1: Nav skeleton + `ModelPickerScreen` + `DashboardScreen` (mock model client)
- M2: Persist model path + copy to files + guard redirect
- M3: Integrate `SmolLM` wrapper with system prompt & stop words; keep loaded
- M4: Dashboard list + per-row states + Improve one/all (sequential) + animations
- M5: zxcvbn integration; async scoring at launch; chip shows “computing…” until ready
- M6: In-memory candidate history + bottom sheet + selection
- M7: UX polish, snackbars, disable re-entry, ✅ auto-hide
- M8: Tests with in-memory repos + fake model client

## Acceptance Criteria
- First-run onboarding → pick model → success → dashboard.
- Dashboard shows 10 random passwords from `StaticWeakPasswords.kt`.
- Score chips show “computing…” then color-coded scores.
- Improve one/all: sequential, row state transitions, crossfade, ✅ auto-hide.
- History shows session candidates; selection updates the row.
- Only model path persists; all else lost on app kill.
- Reselect model works from dashboard.

## Notes linking to current code
- Reuse `copyModelFile()` and `HARDCODED_SYSTEM_PROMPT` in `PasswordStrengthenActivity.kt`.
- Maintain streaming + stop-words filtering pattern shown in `PasswordStrengthenScreen()`.
