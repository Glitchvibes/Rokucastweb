import os, re, uuid, time, threading, subprocess, socket, ipaddress
from pathlib import Path
from urllib.parse import urlparse
import requests
from fastapi import FastAPI, Header, HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field

ROOT = Path("/workspace")
INPUTS = ROOT / "inputs"
OUTPUTS = ROOT / "outputs"
REAL_ESRGAN = ROOT / "Real-ESRGAN"
INPUTS.mkdir(parents=True, exist_ok=True)
OUTPUTS.mkdir(parents=True, exist_ok=True)
API_TOKEN = os.environ.get("CAST_UPSCALE_API_TOKEN", "")
MAX_BYTES = int(os.environ.get("MAX_VIDEO_BYTES", str(2 * 1024**3)))
MAX_SECONDS = int(os.environ.get("MAX_VIDEO_SECONDS", "1800"))
jobs = {}
lock = threading.Lock()
app = FastAPI(title="Cast Roku Web - Real-ESRGAN Cloud", version="1.0.0")

class JobRequest(BaseModel):
    source_url: str = Field(min_length=8, max_length=4096)
    model: str = Field(default="RealESRGAN_x4plus", pattern="^(RealESRGAN_x4plus|RealESRGAN_x2plus|realesr-general-x4v3|realesr-animevideov3)$")
    scale: int = Field(default=2, ge=2, le=4)

def authorize(value):
    if not API_TOKEN:
        raise HTTPException(503, "Cloud API não configurada: defina CAST_UPSCALE_API_TOKEN.")
    if value != f"Bearer {API_TOKEN}":
        raise HTTPException(401, "Token de API inválido.")

def safe_source(url):
    parsed = urlparse(url)
    if parsed.scheme not in ("http", "https") or not parsed.hostname:
        raise HTTPException(400, "A URL precisa ser HTTP ou HTTPS.")
    try:
        for item in socket.getaddrinfo(parsed.hostname, parsed.port or (443 if parsed.scheme == "https" else 80)):
            ip = ipaddress.ip_address(item[4][0])
            if not ip.is_global:
                raise HTTPException(400, "URL para rede privada/local não permitida.")
    except socket.gaierror:
        raise HTTPException(400, "Não foi possível resolver o host do vídeo.")

def run_job(job_id, req):
    src = INPUTS / f"{job_id}.mp4"
    out_dir = OUTPUTS / job_id
    out_dir.mkdir(parents=True, exist_ok=True)
    output = out_dir / "upscaled.mp4"
    try:
        with lock: jobs[job_id]["status"] = "downloading"
        with requests.get(req.source_url, stream=True, timeout=(15, 90), allow_redirects=True) as response:
            response.raise_for_status()
            length = int(response.headers.get("content-length", "0") or 0)
            if length > MAX_BYTES: raise RuntimeError("Vídeo excede o limite de tamanho.")
            total = 0
            with src.open("wb") as f:
                for chunk in response.iter_content(1024 * 1024):
                    if not chunk: continue
                    total += len(chunk)
                    if total > MAX_BYTES: raise RuntimeError("Vídeo excede o limite de tamanho.")
                    f.write(chunk)
        probe = subprocess.run(["ffprobe","-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",str(src)],capture_output=True,text=True,check=True)
        duration = float(probe.stdout.strip() or "0")
        if duration <= 0 or duration > MAX_SECONDS:
            raise RuntimeError(f"Duração não suportada. Limite: {MAX_SECONDS // 60} minutos.")
        with lock: jobs[job_id]["status"] = "processing"
        cmd = ["python", str(REAL_ESRGAN / "inference_realesrgan_video.py"), "-i", str(src), "-o", str(out_dir), "-n", req.model, "-s", str(req.scale), "--tile", "256", "--suffix", "upscaled"]
        if req.model == "realesr-animevideov3": cmd[cmd.index("-n")+1] = "realesr-animevideov3"
        subprocess.run(cmd, cwd=str(REAL_ESRGAN), check=True, timeout=MAX_SECONDS * 8)
        generated = out_dir / f"{src.stem}_upscaled.mp4"
        if not generated.exists():
            candidates = list(out_dir.glob("*_upscaled.mp4"))
            if not candidates: raise RuntimeError("O processamento terminou sem gerar o MP4.")
            generated = candidates[0]
        generated.replace(output)
        src.unlink(missing_ok=True)
        with lock:
            jobs[job_id].update(status="completed", output_url=f"/media/{job_id}", finished_at=time.time())
    except Exception as exc:
        src.unlink(missing_ok=True)
        with lock: jobs[job_id].update(status="failed", error=str(exc)[:500], finished_at=time.time())

@app.get("/health")
def health():
    return {"ok": True, "engine": "Real-ESRGAN", "gpu": subprocess.run(["nvidia-smi","-L"],capture_output=True,text=True).stdout.strip() or "GPU não detectada"}

@app.post("/jobs", status_code=202)
def create_job(req: JobRequest, authorization: str | None = Header(default=None)):
    authorize(authorization)
    safe_source(req.source_url)
    job_id = uuid.uuid4().hex
    with lock: jobs[job_id] = {"id":job_id,"status":"queued","created_at":time.time()}
    threading.Thread(target=run_job,args=(job_id,req),daemon=True).start()
    return {"id":job_id,"status":"queued","status_url":f"/jobs/{job_id}"}

@app.get("/jobs/{job_id}")
def get_job(job_id: str, authorization: str | None = Header(default=None)):
    authorize(authorization)
    with lock: job = jobs.get(job_id)
    if not job: raise HTTPException(404,"Job não encontrado ou expirado.")
    result = dict(job)
    if result.get("output_url"):
        result["output_url"] = result["output_url"]
    return result

@app.get("/media/{job_id}")
def get_media(job_id: str):
    path = OUTPUTS / job_id / "upscaled.mp4"
    if not path.is_file(): raise HTTPException(404,"Vídeo processado ainda não está disponível.")
    return FileResponse(path, media_type="video/mp4", filename="cast-roku-upscaled.mp4")
