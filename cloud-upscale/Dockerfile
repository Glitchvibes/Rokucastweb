FROM pytorch/pytorch:2.1.2-cuda12.1-cudnn8-runtime
ENV DEBIAN_FRONTEND=noninteractive PYTHONUNBUFFERED=1
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg git libgl1 libglib2.0-0 && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
RUN git clone --depth 1 https://github.com/xinntao/Real-ESRGAN.git
WORKDIR /workspace/Real-ESRGAN
RUN pip install --no-cache-dir basicsr facexlib gfpgan -r requirements.txt && python setup.py develop
WORKDIR /workspace
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
EXPOSE 8080
CMD ["uvicorn","app:app","--host","0.0.0.0","--port","8080"]
