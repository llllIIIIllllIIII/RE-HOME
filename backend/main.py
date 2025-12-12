import random
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI
import requests
from typing import Dict, Union
from system_prompt import TRIP_PLANNER_SYSTEM_PROMPT
from result import FALLBACK_OPTIONS

app = FastAPI(title="AMD AI API", description="使用 AMD vLLM 的文字生成 API")

# logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')


# vLLM 伺服器設定
VLLM_BASE_URL = "http://210.61.209.139:45014/v1/"
VLLM_BASE_URL_2="http://210.61.209.139:45005/v1/"


# 初始化 OpenAI client（與模型查詢同一個 endpoint）
client = OpenAI(
    base_url=VLLM_BASE_URL_2,
    api_key="dummy-key"
)

# 取得模型名稱
def get_model_name():
    try:
        response = requests.get(VLLM_BASE_URL_2 + "models")
        models = response.json()
        if models.get("data"):
            return models["data"][0]["id"]
    except Exception as e:
        print(f"Error getting model name: {e}")
    return "gpt-oss-120b"  # 預設值


def is_structured(text: str) -> bool:
    required_keys = ["mood:", "mode:", "destination:", "activity:", "sensory:", "why:", "cta:"]
    return all(k in text for k in required_keys) and "!" * 5 not in text


def call_chat(model_name: str, message: str) -> str | None:
    response = client.chat.completions.create(
        model=model_name,
        messages=[
            {"role": "system", "content": TRIP_PLANNER_SYSTEM_PROMPT},
            {"role": "user", "content": message}
        ],
        max_tokens=180,
        temperature=0.1,
        top_p=0.85,
        stop=["\n\n", "cta:"]
    )
    return response.choices[0].message.content.strip()


def call_completion_fallback(model_name: str, message: str) -> str:
    prompt = (
        f"<|system|>{TRIP_PLANNER_SYSTEM_PROMPT}\n"
        f"<|user|>{message}\n"
        f"<|assistant|>"
    )
    response = client.completions.create(
        model=model_name,
        prompt=prompt,
        max_tokens=180,
        temperature=0.05,
        top_p=0.7,
        stop=["<|user|>", "\n\n"]
    )
    return response.choices[0].text.strip()


def find_fallback_by_mode_keyword(keyword: str) -> Dict[str, str] | None:
    """
    使用較寬鬆的比對規則找出對應的 fallback 項目：
    - 若傳入單字母（A/B/C/D/E）則直接比對 code。
    - 嘗試 mode 字串是否包含 keyword 或反向包含。
    - 嘗試以長度為2的子字串在中文中做共現比對（例如 keyword 包含「文化」，可命中「文化棲居者」）。
    - 若以上皆無，回傳 None。
    """
    keyword_norm = (keyword or "").strip().lower()
    if not keyword_norm:
        return None

    # 若直接傳入代碼字母（A/B/C/D/E），直接比對
    if len(keyword_norm) == 1 and keyword_norm in "abcde":
        for option in FALLBACK_OPTIONS:
            mode_value = option.get("mode", "")
            if mode_value.strip().lower().startswith(keyword_norm):
                return option

    # 逐一檢查每個 option 的名字部分是否能匹配
    for option in FALLBACK_OPTIONS:
        mode_value = option.get("mode", "").strip()
        mode_norm = mode_value.lower()
        # mode 通常格式為 "A 名稱"，嘗試取出名稱部分
        parts = mode_value.split(None, 1)
        mode_code = parts[0].lower() if parts else ""
        mode_name = parts[1].lower() if len(parts) > 1 else mode_code

        # 直接包含比對（任一方向）
        if keyword_norm in mode_name or mode_name in keyword_norm:
            return option

        # 若使用者輸入內含 mode code（例如 "A" 或 "a"），也可命中
        if mode_code and mode_code.startswith(keyword_norm):
            return option

        # 中文友好的二字子串共現比對：若 keyword 與 mode_name 共有任一連續兩字，視為 match
        # 只在長度大於等於2 時檢查
        if len(keyword_norm) >= 2 and len(mode_name) >= 2:
            try:
                for i in range(0, len(keyword_norm) - 1):
                    sub = keyword_norm[i : i + 2]
                    if sub and sub in mode_name:
                        return option
            except Exception:
                pass

    return None


def choose_fallback(mode_keyword: str | None = None) -> Dict[str, str]:
    """依照 mode 關鍵字選取對應項目，否則隨機挑選，最後使用第一筆作保底。"""
    matched = find_fallback_by_mode_keyword(mode_keyword) if mode_keyword else None
    if matched:
        return matched
    if FALLBACK_OPTIONS:
        return random.choice(FALLBACK_OPTIONS)
    return {
        "mood": "最近被工作和生活拉得有點太緊，只想讓腦袋暫時安靜下來",
        "mode": "A 森林沉靜者",
        "destination": "台北大安森林公園靠近湖邊的樹蔭長椅",
        "activity": "帶一本不用動腦的書或音樂，坐在樹蔭下慢慢呼吸，看著湖面上光影晃動",
        "sensory": "微風吹過臉頰、樹葉摩擦的沙沙聲、湖面反射的柔和日光",
        "why": "被綠意和水面包圍時，身體會自然放慢節奏，累積的壓力也比較容易鬆開",
        "cta": "如果今天還有一點點體力，就選一個傍晚時段去坐半小時，當作送給自己的小禮物",
    }


def _option_mode_code(option: Dict[str, str]) -> str:
    """從選項的 `mode` 欄位擷取 A/B/C/D/E code，找不到則回傳 '?'。"""
    if not option:
        return "?"
    mode = option.get("mode", "")
    if not mode:
        return "?"
    # 以第一個英文字母為準
    first = mode.strip()[0].upper()
    if first in {"A", "B", "C", "D", "E"}:
        return first
    return "?"


# 請求模型
class MessageRequest(BaseModel):
    message: str


# 回應模型
class MessageResponse(BaseModel):
    prompt: str
    generated_text: Union[str, Dict[str, str]]


class ModeRequest(BaseModel):
    mode_keyword: str


@app.post("/generate", response_model=MessageResponse)
async def generate_text(request: MessageRequest):
    """
    根據輸入的 message 生成文字
    """
    try:
        model_name = get_model_name()
        generated_text = call_chat(model_name, request.message)
        if not generated_text or not is_structured(generated_text):
            generated_text = call_completion_fallback(model_name, request.message)
        if not is_structured(generated_text):
            generated_text = choose_fallback()

        return MessageResponse(prompt=request.message, generated_text=generated_text)
        
    except Exception as e:
        fallback_payload = choose_fallback()
        raise HTTPException(status_code=500, detail={"error": str(e), "fallback": fallback_payload})


@app.get("/health")
async def health_check():
    """
    健康檢查端點
    """
    return {"status": "healthy"}


@app.get("/models")
async def list_models():
    """
    列出可用的模型
    """
    try:
        response = requests.get(VLLM_BASE_URL + "models")
        return response.json()
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"無法取得模型列表: {str(e)}")


@app.post("/fallback", response_model=Dict[str, str])
async def get_fallback_by_mode(request: ModeRequest):
    """依 mode 關鍵字回傳最符合的預設選項，找不到則回傳隨機/保底。"""
    chosen = choose_fallback(request.mode_keyword)
    code = _option_mode_code(chosen)
    logging.info("/fallback called - keyword=%r chosen=%s", request.mode_keyword, code)
    return chosen


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
