# RE-HOME

這個倉庫包含兩個主要資料夾：

- `android/`：放 Android 相關專案（已存在於遠端）。
- `backend/`：FastAPI 後端服務（本 README 針對此資料夾說明）。

目前 `backend/` 的核心檔案：

- `main.py` — FastAPI 應用程式（API 入口）。
- `result.py` — 預設推薦選項清單（fallback options）。
- `system_prompt.py` — 模型 system prompt 文案。

其他檔案（測試、實驗用）已放入 `.gitignore`，遠端僅同步上述三個核心檔案。

---

快速啟動（伺服器）

1. 建議建立並啟用虛擬環境：
```bash
cd /home/devuser/RE-HOME/backend
python3 -m venv .venv
source .venv/bin/activate
```

2. 安裝必要套件：
```bash
pip install fastapi uvicorn openai requests
```

3. 啟動服務（開發/測試用）：
```bash
python main.py
# 或使用 uvicorn：
uvicorn main:app --host 0.0.0.0 --port 8000
```

4. 健康檢查
```bash
curl http://localhost:8000/health
```

---

主要 API

- `POST /generate` — 輸入 JSON `{ "message": "..." }`，回傳模型生成或 fallback（結構化字串或 JSON）。

範例：
```bash
curl -X POST http://<HOST>:8000/generate \
  -H "Content-Type: application/json" \
  -d '{"message":"我最近很焦慮，想去海邊放空"}'
```

- `POST /fallback` — 輸入 JSON `{ "mode_keyword": "..." }`，回傳 `result.py` 中對應的 fallback 物件（若找不到則隨機一筆）。

範例：
```bash
curl -X POST http://<HOST>:8000/fallback \
  -H "Content-Type: application/json" \
  -d '{"mode_keyword":"文化"}'
```

伺服器日誌會記錄 `/fallback` 被呼叫時的 `mode_keyword`（原文）以及系統選到的代碼（A/B/C/D/E），方便偵錯。

---

