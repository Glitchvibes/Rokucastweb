# Real-ESRGAN Cloud para Cast Roku Web

Este serviço processa **arquivos de vídeo HTTP/HTTPS públicos** em uma máquina Linux com GPU NVIDIA e devolve um MP4 para o Roku reproduzir. Ele usa o script oficial `inference_realesrgan_video.py` do projeto Real-ESRGAN.

## Implantação

É necessário um servidor GPU NVIDIA com Docker, NVIDIA Container Toolkit e porta HTTPS pública (coloque um proxy TLS como Caddy/Nginx na frente). O GitHub Actions apenas compila o APK; não hospeda GPU nem executa o modelo.

```bash
cp .env.example .env
# Edite .env e defina CAST_UPSCALE_API_TOKEN com um segredo forte
docker compose up -d --build
```

Teste `GET /health`. Configure no APK a URL pública HTTPS e o mesmo token.

## API
- `POST /jobs` com Bearer token e JSON `{"source_url":"https://.../video.mp4","model":"RealESRGAN_x4plus","scale":2}`
- `GET /jobs/{id}` com Bearer token: estados queued/downloading/processing/completed/failed
- `GET /media/{id}`: MP4 final para o player Roku

Modelos aceitos: `RealESRGAN_x4plus`, `RealESRGAN_x2plus`, `realesr-general-x4v3` e `realesr-animevideov3`. O modelo de anime é melhor para animação. O processamento é assíncrono; vídeos longos podem levar mais tempo que a duração original.

## Limitações
- O serviço baixa o vídeo completo antes de processar. Não é upscale ao vivo/tempo real.
- A fonte precisa ser acessível pelo servidor sem DRM, cookies ou cabeçalhos de sessão. Links M3U8/DASH e páginas protegidas não são aceitos como entrada direta por esta primeira versão; use um MP4 HTTP/HTTPS direto.
- Limite padrão: 2 GB e 30 minutos.
- O resultado fica em disco no servidor. Configure armazenamento persistente e política de limpeza para produção.
- Use HTTPS e um token privado. Não exponha o serviço sem autenticação.
- A saída é H.264 MP4 com áudio preservado quando a fonte tem áudio.

Licença do Real-ESRGAN: BSD-3-Clause. Consulte o repositório original: https://github.com/xinntao/Real-ESRGAN
