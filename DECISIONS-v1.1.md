# SniperRex — Keputusan Arsitektur v1.1 (AMANDEM AGENTS.md)

> Dokumen ini **meng-override** AGENTS.md v1.0 pada poin yang bertabrakan.
> Tempel blok di bawah ke bagian atas AGENTS.md agar dibaca coding agent
> sebelum spec lain.

## 5 KEPUTUSAN FINAL (Anto, 27 Okt 2026)

| # | Isu v1.0 | Keputusan v1.1 | Konsekuensi |
|---|---|---|---|
| 1 | RR basis ambigu (TP1 vs TP final) | **`risk_reward` = RR TP1** (konstruksi = 2.00) | Field tampil tetap 2.00. Komponen skor 20 poin memakai `rr_extension` (TP3 = 4R) agar tidak jadi konstanta mati |
| 2 | Gate konfluensi 3/4 vs preset 3 filter | **Hybrid**: 4 filter → fixed 3; <4 filter → `ceil(0.66×n)`, minimal 2 | Preset default (RSI, ATR, OB) → butuh 2 dari 3, bukan 3 dari 3 |
| 3 | Persistensi backend | **In-memory** untuk MVP | Wajib dedupe per bar + TTL; signal hilang saat restart (didokumentasikan) |
| 4 | HTF trend untuk tier FREE | **M30 sebagai proxy** (M45 tidak ada di vendor Twelve Data) | FREE: M15 + M30 saja; HTF alignment dari M30. PRO: H1/H4 |
| 5 | Nama project | **SniperRex** | Namespace Kotlin `com.sniperrex.app`, appName "SniperRex" |

## Patch wajib lain (dari audit)

1. **SL tunggal**: hanya `risk/levels.py` yang menghitung SL (`sl_buffer_atr=0.2`). `ob_buffer_atr` dihapus.
2. **pip ≠ point**: `pip_size` XAUUSD = 0.1, `point_size` = 0.01. Semua pembulatan pakai `point_size`.
3. **FVG validity**: gap invalid hanya bila candle setelahnya **menutup penuh** zona. Entry retest 50% tetap valid.
4. **CHOCH**: `structure_event: BOS | CHOCH | NONE` dipisah dari `core_strategy`.
5. **Dedupe key**: `pair:timeframe:strategy:direction:bar_start` + grace 90 detik.
6. **ATR floor per kategori**: Metal 0.05%, Forex 0.03%, JPY 0.02%, Crypto 0.10%.
7. **Disclaimer**: `disclaimer` wajib di root `SignalEnvelope` / `SignalListEnvelope`.
8. **AI vs quant**: AI **tidak bisa** menaikkan status. Matriks final: `SKIP` bila `quant < 50` atau `ai_verdict == SKIP`; selain itu `TAKE` bila keduanya ≥ 60, else `WAIT`.
9. **pandas-ta**: hanya `dev-requirements.txt`, dilarang di runtime production.
10. **Sesi**: UTC — ASIA 00–07, LONDON 07–12, NEW_YORK 12–21, SYDNEY 21–24.

## Status Definition of Ready

- [x] Backend skeleton jalan (`pytest`: 37 hijau, coverage 89%)
- [x] Schema Signal + envelope
- [x] Client Twelve Data + cache TTL
- [x] Risk calculator + quant score
- [x] 5 strategi + 4 konfirmasi + gate hybrid (Sprint 2)
- [x] E2E scan teruji via `httpx.MockTransport` (offline)
- [ ] Validasi feed nyata — **butuh TWELVE_DATA_API_KEY**
- [ ] Mirror SHORT untuk FVG & OB+BOS (batasan sadar, belum ada)

## Sumber data

Keputusan Anto (27 Okt): **data live Twelve Data saja**, fixture CSV dibatalkan.
Konsekuensi: golden test diganti test sintetis deterministik + mock transport;
verifikasi manual tetap wajib di TradingView untuk validasi visual setup.
