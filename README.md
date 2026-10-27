# SniperRex

Aplikasi **signal-only** untuk forex/gold dengan pendekatan Smart Money Concepts
(FVG, Order Block + BOS/CHOCH) plus skor kuantitatif deterministik dan AI sebagai
pendapat kedua.

> Signal only. Not financial advice. Trade at your own risk.
> Tidak ada endpoint order, tidak ada koneksi broker, tidak ada auto-trade.

## Isi repo

| Folder | Isi | Status |
|---|---|---|
| `backend-python/` | FastAPI: 5 strategi, 4 konfirmasi, risk calculator, quant score, AI layer | **64 test hijau, coverage 87%** |
| `android-app/` | Kotlin + Compose + Hilt + Room + Retrofit, 6 layar, WorkManager scan 15 menit | **scaffold, belum di-build** |
| `.github/workflows/build.yml` | CI: pytest backend + build debug APK, artifact APK | siap |
| `DECISIONS-v1.1.md` | amandemen spec yang mengikat (RR basis TP1, gate hybrid, dst) | acuan |
| `AGENTS.v1.0-source.txt` | spec awal (arsip) | referensi |

## Kredensial — tidak ada key di repo

- `.env` **tidak** di-commit (gitignored). Contoh: `backend-python/.env.example`.
- Backend menerima key per request lewat header `X-Twelve-Key`, `X-AI-Key`,
  `X-AI-Base-Url`, `X-AI-Model` — jadi aplikasi bisa dibagikan tanpa membawa key.
- Android menyimpan key user di DataStore perangkat, bukan di kode.

## Menjalankan backend

```bash
cd backend-python
pip install -r requirements.txt
cp .env.example .env      # opsional: isi TWELVE_DATA_API_KEY
python -m uvicorn app.main:app --reload --port 8000
python -m pytest -q
```

## Build APK

Tanpa Android SDK lokal: push ke GitHub → **Actions → Build APK & Test Backend
→ Run workflow** → unduh artifact `sniperrex-debug-apk`.

Build lokal (JDK 17 + Android Studio): buka `android-app/`, jalankan
`gradle :app:assembleDebug`. Ganti `BASE_URL` di `app/build.gradle.kts` ke IP LAN
backend bila diuji di perangkat fisik.

## Keputusan desain penting

1. `risk_reward` = RR **TP1** (2.00). Komponen skor memakai perluasan TP3 agar
   bobot 20 poin tidak jadi konstanta.
2. Gate konfluensi hybrid: 4 filter → wajib 3; <4 filter → `ceil(0.66n)`, minimal 2.
3. AI **tidak bisa** menaikkan status: quant < 50 atau AI SKIP → SKIP.
4. Kuota Twelve Data FREE 8 kredit/menit **dan 800/hari** → worker merotasi 2 pair
   per siklus (384 kredit/hari).
5. Interval vendor: `15min/30min/45min/1h/4h/1day`; simbol `XAU/USD` (pakai slash).
