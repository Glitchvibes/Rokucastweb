# Cast Roku Web

App Android (Kotlin) que funciona como um navegador com um botao de
"transmitir" (cast), parecido com o Web Video Cast: voce navega em
qualquer site, o app detecta links de video/stream na pagina, e manda
o link escolhido pro canal Roku companheiro tocar.

## Como gerar o APK sem instalar nada (GitHub Actions)

Esse projeto ja vem com o Gradle Wrapper completo e um workflow pronto
em `.github/workflows/build.yml`. O GitHub compila o APK pra voce de
graca, num servidor deles.

1. Crie uma conta no [github.com](https://github.com) se ainda nao tiver.
2. Crie um repositorio novo, **privado** (botao verde "New" em
   github.com/new). Pode deixar vazio, sem README.
3. Suba os arquivos desta pasta pra esse repositorio. Duas formas:
   - **Mais facil (sem terminal)**: na pagina do repositorio vazio, clique
     em "uploading an existing file", arraste TODOS os arquivos e pastas
     desta pasta `CastRokuWeb/` (inclusive a pasta `.github/` - ela pode
     vir escondida, confira se marcou "mostrar arquivos ocultos" no seu
     computador antes de arrastar) e clique em "Commit changes".
   - **Com git instalado**: dentro da pasta `CastRokuWeb/`, rode:
     ```
     git init
     git add -A
     git commit -m "Primeiro commit"
     git branch -M main
     git remote add origin https://github.com/SEU_USUARIO/SEU_REPO.git
     git push -u origin main
     ```
4. Va na aba **Actions** do repositorio. Um workflow chamado "Build APK"
   deve comecar sozinho (ou clique em "Run workflow" se nao comecar).
5. Espere o circulo ficar verde (uns 3-5 minutos na primeira vez).
6. Clique no build concluido, desca ate "Artifacts" e baixe
   **CastRokuWeb-debug-apk** — e um .zip com o `app-debug.apk` dentro.
7. Transfira o `.apk` pro celular (ex: enviando por WhatsApp/email pra
   voce mesmo, ou Google Drive) e instale (ative "Instalar de fontes
   desconhecidas"/"Permitir deste app" quando o Android perguntar).

## Alternativa: Android Studio

Se preferir compilar local, instale o **Android Studio**, abra esta
pasta como projeto existente, espere o sync automatico, e use
`Build > Build Bundle(s) / APK(s) > Build APK(s)`. O APK fica em
`app/build/outputs/apk/debug/app-debug.apk`.

## Como funciona a deteccao de video

Duas tecnicas, porque uma sozinha nao pega tudo:

1. **Varredura do DOM** (JavaScript injetado apos a pagina carregar):
   olha as tags `<video>`/`<source>` da pagina principal.
2. **Observacao de rede** (`shouldInterceptRequest` na WebView): olha
   TODA requisicao HTTP que a pagina faz, inclusive as de dentro de
   `<iframe>` de outro site (onde a maioria dos players embutidos mora),
   e sinaliza qualquer URL terminada em `.m3u8`, `.mp4`, `.mkv`, `.webm`,
   `.mov`, `.ts` ou `.mpd`.

**Limitacao honesta**: sites que escondem o link real atras de DRM
(Widevine), que só streamam por WebSocket/protocolo proprietario, ou que
carregam o video dentro de um player nativo fora da WebView, nao vao ser
detectados — isso e uma limitacao de qualquer app desse tipo (inclusive
o Web Video Cast real), nao so deste.

## Protocolo de "cast" (o que o canal Roku companheiro precisa escutar)

Quando o usuario aperta "transmitir" num video, o app manda:

```
POST http://<IP_DA_ROKU>:8065/cast
Content-Type: application/json

{
  "url": "https://exemplo.com/video.m3u8",
  "title": "Nome da pagina",
  "from": "CastRokuWeb"
}
```

O IP e a porta sao configurados na tela de engrenagem do app (porta
padrao 8065, pode mudar).

**Pro canal Roku (proximo projeto)**: BrightScript nao tem um servidor
HTTP pronto pra escutar POST, mas da pra abrir um `roStreamSocket` em
modo listener nessa porta, aceitar a conexao, ler o corpo da requisicao
HTTP crua (os bytes chegam como texto HTTP normal: primeira linha
`POST /cast HTTP/1.1`, depois os headers, uma linha em branco, e o corpo
JSON), extrair o campo `url` do JSON com `ParseJson()`, e tocar no
`Video` node do canal. Quando formos construir esse canal, e por aqui
que comeca.

## Estrutura

- `MainActivity.kt` — navegador (WebView), barra de URL, deteccao de
  video, botao flutuante com contagem de videos encontrados.
- `VideoDetector.kt` — as duas tecnicas de deteccao.
- `CastClient.kt` — envia o POST pro canal Roku.
- `RokuPrefs.kt` — guarda IP/porta configurados.
- `SettingsActivity.kt` — tela pra configurar IP/porta da Roku.
- `DetectedVideosDialog.kt` / `VideoListAdapter.kt` — lista de videos
  encontrados na pagina atual.
