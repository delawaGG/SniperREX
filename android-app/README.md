# SniperRex — Android (Sprint 4 scaffold)

Kotlin 2.0.20 · Compose BOM 2024.09.03 · Hilt 2.52 · Retrofit 2.11 · Room 2.6.1.

**Status: scaffold, BELUM di-build.** Sandbox tidak punya JDK + Android SDK, jadi
`./gradlew assembleDebug` belum pernah dijalankan. Jangan anggap ini "kompilasi hijau"
sampai Anto build di Android Studio.

## Struktur (Clean Architecture, spec §4)

```
presentation/  Compose + ViewModel (StateFlow<UiState>)
   ↓
domain/        model + repository interface + 8 usecase (suspend/Flow)
   ↓
data/          Retrofit API, Room, DataStore, repository impl
```

Aturan yang dipatuhi:
- `presentation` tidak pernah import Retrofit/Room.
- `domain` tidak menyentuh framework Android (kecuali coroutines).
- Hanya `data` yang tahu HTTP & DB.

## Layar yang sudah ada

| Layar | Isi |
|---|---|
| DashboardScreen | TradingView WebView, signal terbaru, FAB Scan, banner disclaimer |
| SettingsScreen | input Twelve key + AI key/base/model, tombol Test & Simpan |
| PairScreen | 5 pair, chip timeframe (FREE: M15/M30/M45) |
| StrategyScreen | 5 strategi, 3 checkbox konfirmasi, preset SMC Combo, SCAN |
| SignalsScreen | journal dari Room |
| SignalDetailScreen | levels, konfluensi, ANALYZE WITH AI, SKIP |

Tidak ada tombol BUY/SELL/EXECUTE — constraint spec §14.3.

## Cara key dipakai (penting untuk sharing)

1. User isi key di SettingsScreen → disimpan di DataStore perangkat.
2. `ApiKeyInterceptor` menempelkan `X-Twelve-Key`, `X-AI-Key`, `X-AI-Base-Url`,
   `X-AI-Model` di setiap request.
3. Backend tidak menyimpan key; aplikasi juga tidak hardcode key apa pun
   (`BuildConfig.BASE_URL` hanya alamat server).

Logging OkHttp dipaksa `BASIC` dan header key di-redact — mencegah key bocor ke logcat.

## Scan latar belakang (WorkManager)

| File | Peran |
|---|---|
| `data/worker/SignalScanWorker.kt` | `@HiltWorker CoroutineWorker`: scan M15 preset FVG + RSI/ATR/OB |
| `data/worker/ScanScheduler.kt` | periodic 15 menit (unique work, `UPDATE`), plus `runNow()` |
| `data/worker/ScanNotifier.kt` | notifikasi signal baru (dibungkus izin POST_NOTIFICATIONS) |
| `data/worker/ScanConfig.kt` | preset + rotasi pair |

Pagar yang dipasang:
- **Tanpa API key → worker langsung `Result.success()`**, tidak ada request sia-sia
  (penting karena aplikasi dibagikan ke teman).
- **Toggle di Settings** (`auto_scan` di DataStore) mengatur `schedule()`/`cancel()`.
  Jadwal dipulihkan saat Dashboard dibuka (`AutoScanUseCase.restore()`), karena
  WorkManager bisa hilang setelah reboot / force stop.
- **Rotasi 2 pair per siklus**: kuota Twelve Data FREE = 8 kredit/menit **dan 800
  kredit/hari**. 4 pair x 2 request tiap 15 menit = 768/hari (96%) — terlalu mepet.
  Rotasi ini jadi 384/hari.
- **429 → `Result.retry()`** dengan backoff eksponensial, bukan dibuang.
- WorkManager diinisialisasi manual lewat `SniperRexApp : Configuration.Provider`
  (`HiltWorkerFactory`), initializer default dimatikan di manifest.

## Yang belum ada (jujur)

- Build/CI (belum pernah `./gradlew assembleDebug`), unit test ViewModel, Compose UI test.
- Worker belum diuji di perangkat: `@HiltWorker` + manifest initializer removal adalah
  bagian yang paling rawan gagal saat build pertama.
- AiChatScreen (V2), mini chart grid 4 pair, theming lanjutan.
- Mapper entity → domain belum menyimpan `confluence` rinci per filter.
- `SettingsViewModel` memakai helper `update` lokal — sebaiknya ganti ke
  `kotlinx.coroutines.flow.update` saat build pertama.

## Build

```bash
# backend harus jalan dan terjangkau dari emulator/perangkat
cd ../backend-python && python3 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
# lalu buka android-app di Android Studio (JDK 17) → Run
```
