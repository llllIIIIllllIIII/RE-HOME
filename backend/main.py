import random
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI
import requests
from typing import Dict, Union
from system_prompt import TRIP_PLANNER_SYSTEM_PROMPT
from result import FALLBACK_OPTIONS

app = FastAPI(title="AMD AI API", description="使用 AMD vLLM 的文字生成 API")

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
    """依據傳入的關鍵字尋找 mode 值內含該關鍵字的預設項目。"""
    keyword_norm = keyword.strip().lower()
    if not keyword_norm:
        return None

    # 先嘗試片段比對，再嘗試開頭字母比對（例如只傳 A/B/C）。
    for option in FALLBACK_OPTIONS:
        mode_value = option.get("mode", "").lower()
        if keyword_norm in mode_value:
            return option
    for option in FALLBACK_OPTIONS:
        mode_value = option.get("mode", "").lower()
        if mode_value.startswith(keyword_norm):
            return option
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
    return choose_fallback(request.mode_keyword)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
