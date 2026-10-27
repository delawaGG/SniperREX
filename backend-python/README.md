# SniperRex — Backend (Sprint 1-2)

Signal-only trading API. **Tidak ada** endpoint `/order`, `/execute`, `/buy`, `/sell`.
Tidak ada integrasi broker. Bukan nasihat finansial.

## Status: Sprint 2 selesai — 47 test hijau, coverage ~88%

| Sprint | Isi | Status |
|---|---|---|
| 1 | Scaffold, models, Twelve Data client, risk, score, in-memory store, routes | SELESAI |
| 2 | 5 strategi + 4 konfirmasi + gate + E2E scan (data live, tanpa fixture CSV) | SELESAI |
| 3 | AI layer (OpenAI-compatible) | BERIKUTNYA |
| 4-6 | Android (Kotlin/Compose) | MENUNGGU |
| 7 | Pine v6 research (paralel) | MENUNGGU |

## Struktur

```
backend-python/
  app/
    config.py                  # semua threshold (no magic numbers)
    symbols.py                 # katalog pair, pip vs point, ATR floor
    indicators.py              # ema, rma, atr, rsi manual (no black-box)
    models/signal.py           # Signal (frozen), envelope, ScanRequest
    clients/twelve_data_client.py
    risk/levels.py             # SATU-SATUNYA otoritas SL/TP
    scoring/confluence_score.py
    services/scan_service.py   # fetch -> context -> (strategi Sprint 2)
    store/memory_store.py      # in-memory + dedupe + TTL
    api/routes_*.py
    main.py                    # guard endpoint eksekusi
  tests/                       # 14 test, semua hijau
```

## Jalankan

```bash
python3 -m pip install -r requirements.txt -r dev-requirements.txt
cp .env.example .env          # isi TWELVE_DATA_API_KEY
python3 -m uvicorn app.main:app --reload --port 8000
python3 -m pytest -q
```

## Endpoint aktif

```
GET  /api/health
GET  /api/health/twelve
GET  /api/health/ai            (501 sampai Sprint 3)
GET  /api/tier-info
GET  /api/pairs
POST /api/scan                 (pipeline jalan; strategi Sprint 2)
GET  /api/signals
GET  /api/signals/{id}
PUT  /api/signals/{id}/status
POST /api/ai/analyze           (501 sampai Sprint 3)
```

## Catatan teknis (disadari, bukan bug)

1. **In-memory store** — signal hilang saat restart. Migrasi SQLite sebelum rilis.
2. **RR field = TP1** (selalu 2.00). Skor RR memakai TP3 extension, lihat `DECISIONS-v1.1.md`.
3. **M45 tidak ada di Twelve Data** — proxy HTF tier FREE = M30.
4. **Kredit Twelve Data FREE = 8/menit** — cache TTL per TF wajib; jangan scan multi-pair paralel di FREE.
5. **pip ≠ point** untuk XAUUSD (pip 0.1, point 0.01).

## Mode API key (penting untuk dibagikan)

Dua mode, keduanya tanpa menyimpan key di source code:

| Mode | Cara | Dipakai untuk |
|---|---|---|
| Server-side | `TWELVE_DATA_API_KEY` di `.env` (gitignored) | dev lokal / instance pribadi Anto |
| Per-request | header `X-Twelve-Key: <key user>` | aplikasi dibagikan — tiap teman pakai key sendiri, server tidak menyimpan apa pun |

Android: key dari DataStore → dipasang sebagai header `X-Twelve-Key` di OkHttp
Interceptor. Server hanya cache client di memori (maks 32 key), tidak menulis ke disk
dan tidak pernah me-log key. Pesan error disanitasi (`key -> ***`) — ada test khusus.

Endpoint bantu untuk tombol "Test Connection": `GET /api/tier-check`.

## Verifikasi live (butuh API key)

Keputusan Anto: **data live Twelve Data**, tanpa fixture CSV. Test memakai
`httpx.MockTransport` sehingga CI tetap offline. Untuk validasi ke feed nyata:

```bash
# 1. simpan key
cp .env.example .env   # isi TWELVE_DATA_API_KEY

# 2. cek tier + koneksi
curl -s localhost:8000/api/tier-info
curl -s localhost:8000/api/health/twelve

# 3. scan nyata (FREE: M15/M30 saja)
curl -s -X POST localhost:8000/api/scan -H 'content-type: application/json' \
  -d '{"pair":"XAUUSD","timeframe":"M15","core_strategy":"FVG",
       "confirmations":["RSI","ATR","OB"]}'
```

Aturan `no_setup`: strategi hanya mengevaluasi **bar tertutup terakhir** —
kalau bar terakhir tidak membentuk setup, hasilnya `no_setup`, bukan sinyal paksa.

## Cakupan strategi (v1.2 — semua dua arah)

| Strategi | Arah | Catatan |
|---|---|---|
| FVG | LONG + SHORT | zona invalid hanya bila tertutup penuh; entry 50% |
| ORDER_BLOCK_BOS | LONG + SHORT | BOS + retest zona OB; overextended >1.5 ATR ditolak |
| EMA_CROSS | LONG + SHORT | butuh slope EMA slow searah |
| MACD_CROSS | LONG + SHORT | cross tepat di bar tertutup terakhir |
| TREND_STRUCTURE | LONG + SHORT | HH+HL / LH+LL + break swing |

### Koreksi teknis v1.2 (dari uji feed nyata)

1. **Twelve Data tidak menerima `M15`** — vendor pakai `15min`, `30min`, `45min`,
   `1h`, `4h`, `1day`. Mapping ada di `VENDOR_INTERVAL`.
2. **M45 ternyata TERSEDIA** di vendor → proxy HTF tier FREE sekarang `M45`
   (sebelumnya M30). Konfigurasi: `free_timeframes = M15, M30, M45`.
3. **Simbol harus pakai slash** — `XAU/USD`, bukan `XAUUSD`. Field `td_symbol` di katalog.
4. **Blokir H1/H4/D1 di FREE adalah kebijakan produk, bukan batas vendor** — key
   basic memang bisa menarik `4h`/`1day`. Anggap ini gate fitur, bukan keterbatasan data.
5. **Rate limit 8 kredit/menit** ditangani rate limiter lokal (menunggu, bukan
   menabrak 429). Kalau HTF gagal, skor trend = `mixed` dan alasannya masuk `warnings`,
   bukan didiamkan.
