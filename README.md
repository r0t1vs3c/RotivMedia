# RotivMedia

Launcher Android feito do zero para central multimídia de carro, com a identidade visual da **RotivSecurity**.

Foi feito e testado numa central **ROCO K706 / firmware QF005 (Qianfeng)**, vendida também como H-Tech HT-9864CA: Android 10 real (API 29, embora a central diga "Android 13"), tela de 1280×800 deitada, chip Unisoc. Deve funcionar em outras centrais Android 10+, mas o encaixe das janelas depende do modo de janelas livres (freeform) estar disponível.

> Projeto pessoal, sem relação com a QF, a ROCO ou a H-Tech. Use por sua conta e risco.

## Tela inicial

```
┌──────────── barra da central (com a marca RotivMedia desenhada atrás) ────────────┐
│ ┌────────────────────┐ ┌─────────────────────────┐ ┌──────────────────┐            │
│ │                    │ │                         │ │ MEU CARRO        │            │
│ │   Google Maps      │ │   YouTube Music         │ │ Giro    Motor    │            │
│ │   (janela real,    │ │   (janela real,         │ │ Bateria Agora    │            │
│ │    encaixada)      │ │    encaixada)           │ │ Média   Viagem   │            │
│ │                    │ │                         │ │                  │            │
│ └────────────────────┘ │                         │ │                  │            │
│ [ Apps ] [ Ajustes ]   └─────────────────────────┘ └──────────────────┘            │
└────────────────────────────────────────────────────────────────────────────────────┘
```

- **Maps à esquerda** e **YouTube Music no centro** são os próprios apps, abertos em janelas livres e posicionados em cima dos quadros da tela. Dá para usar os dois com toque normalmente.
- **YouTube e RetroArch** abrem no lado direito, ocupando o centro e os dados do carro. Os controles deles (tocar/pausar e **Fechar**) ficam na faixa de baixo, sem cobrir o vídeo ou o jogo. Fechar o RetroArch pede um segundo toque, para não perder o jogo sem querer. Ao fechar, o YouTube Music volta para o centro.
- **Dados do carro** ao vivo pelo OBD2. Tocar no painel abre a tela Meu carro.
- **Faixa de baixo:** Apps (lista de aplicativos) e Ajustes.
- A barra de cima da central é transparente neste firmware. A tela inicial desenha atrás dela uma faixa com o dragão e "ROTIVMEDIA", gerada por [`tools/barra_central.py`](tools/barra_central.py).

## Funções

| Parte | Como funciona |
| --- | --- |
| Boas-vindas | Animação com o dragão e "BEM-VINDO, SEU NOME" ao ligar a central (uma vez por boot, ou ao acordar depois de 5 min ou mais com a tela apagada). Toque para pular. |
| Maps fixo | Google Maps encaixado no quadro da esquerda. Pode ser desligado em Ajustes → Mapa (aí aparece um mapa desenhado com velocidade e atalhos para Radar, Só mapa e Dirigir). |
| YouTube Music fixo | YouTube Music encaixado no quadro do centro. Desligado, aparece um player próprio (capa, progresso, curtir, anterior/tocar/próxima). |
| YouTube e RetroArch | Abrem encaixados à direita, pela lista de Apps. |
| Trava de vídeo | Opcional: pausa o YouTube acima de uma velocidade escolhida (GPS ou OBD2) e volta a tocar com o carro parado. |
| Aviso de velocidade | Bipe acima do limite escolhido (60 a 120 km/h). |
| Meu carro | Carro desenhado por código (hatch, sedã, SUV, picape) em 13 cores ou cor livre, ou a foto do seu carro. |
| OBD2 | Adaptador ELM327 Bluetooth clássico: giro, velocidade, temperatura do motor, bateria, carga e admissão. Conecta sozinho ao ligar a central. |
| Consumo | Média e instantâneo em km/l, litros, distância, tempo e custo da viagem. Gasolina (E27) ou etanol. Carros sem sensor MAF usam a pressão do coletor e a cilindrada. |
| Luz de injeção | Lê os códigos de falha e explica em português. **Só leitura**: o app nunca apaga códigos nem envia comandos ao carro. |
| Modo demonstração | Dados simulados de OBD2 para ver tudo funcionando sem adaptador. |
| ADB remoto | Religa a depuração e o Tailscale a cada boot, para atualizar a central pela rede (veja [Segurança](#segurança)). |

## OBD2

1. Use um **ELM327 Bluetooth clássico** (v1.5 ou v2.1). Os "BLE / Bluetooth 4.0 / iOS" a maioria das centrais não enxerga.
2. Encaixe no conector OBD2 (embaixo do painel, lado do motorista) e ligue a ignição.
3. Nas configurações de Bluetooth da **central**, pareie com ele (senha 1234 ou 0000).
4. No RotivMedia: Ajustes → Adaptador OBD2 → **Buscar adaptadores pareados** → toque no adaptador.
5. Para o consumo ficar certo: escolha gasolina ou etanol, informe o preço do litro e a cilindrada.

## Gerar o APK

Requisitos: JDK 17+ e Android SDK (platform 35, build-tools 35). Crie um `local.properties` com `sdk.dir=` apontando para o seu SDK.

```bash
./gradlew assembleDebug
```

O APK sai em `app/build/outputs/apk/debug/app-debug.apk`. O APK pronto de cada versão fica em [Releases](../../releases).

Para atualizar por cima de uma versão já instalada, o APK precisa ser assinado com a mesma chave. Se você compilar no seu PC, desinstale a versão da release antes.

## Instalar na central

**Pendrive:** copie o APK, abra no gerenciador de arquivos da central e permita "instalar apps desconhecidos".

**ADB:**
```bash
adb connect IP_DA_CENTRAL:PORTA
adb install -r app-debug.apk
```

### Primeira configuração

1. **Tela inicial:** aperte Home e escolha "RotivMedia" → "Sempre". Na QF005 o launcher padrão fica no `config_service` do sistema (veja [docs/central-qf005.md](docs/central-qf005.md)).
2. **Localização e notificações:** permita quando o app pedir.
3. **Acesso à música:** toque em "Liberar música" na faixa de baixo e ative "RotivMedia · música". Pelo ADB:
   ```bash
   adb shell cmd notification allow_listener com.rotivsecurity.launcher/com.rotivsecurity.launcher.MediaListener
   ```
4. **Janelas livres** (necessário para o Maps e o YouTube Music encaixados):
   ```bash
   adb shell settings put global force_resizable_activities 1
   adb shell settings put global enable_freeform_support 1
   ```
   e reinicie a central.
5. **ADB remoto (opcional):**
   ```bash
   adb shell pm grant com.rotivsecurity.launcher android.permission.WRITE_SECURE_SETTINGS
   ```
6. **Maps offline:** no Maps, foto do perfil → Mapas off-line → baixe sua região.

## Segurança

- Na QF005 o `adbd` roda como **root** e com `ro.adb.secure=0`: qualquer aparelho na mesma rede consegue se conectar **sem pedir confirmação**. Com o ADB remoto ligado, só conecte a central a redes suas (por exemplo, o roteador do seu celular) e acesse de fora apenas por uma VPN privada como o Tailscale. Nunca deixe a porta do ADB exposta na internet.
- O OBD2 é só leitura. O projeto não mexe no MCU nem no barramento CAN do carro.
- Não precisa de conta Google na central para o launcher funcionar.

## Estrutura do código

| Arquivo | Conteúdo |
| --- | --- |
| `Activities.kt` | Tela inicial (`HomeActivity`, desenhada atrás da barra da central), `OverlayActivity` (Ajustes, Apps e Meu carro por cima das janelas) e painel da tela dividida |
| `MapDock.kt` | Encaixe das janelas: `WindowDock` (abre em freeform no retângulo do quadro, corrige o tamanho com `am task resize`, tira da tela cheia), `MapDock`, `MusicDock`, `YtDock`, `RetroDock` |
| `LocalShell.kt` | Cliente do protocolo ADB para rodar comandos `am` na própria central |
| `RemoteAdb.kt` | Religa a depuração e pede ao Tailscale para conectar a cada boot |
| `ui/LauncherScreen.kt` | Tela inicial: quadros, faixa de baixo, painel do carro, player, YouTube, lista de apps |
| `ui/Theme.kt` | Cores, fonte e escala (layout desenhado em 400 "dp" de altura) |
| `ui/Welcome.kt` | Animação de boas-vindas e controle de uma vez por boot |
| `ui/SettingsScreen.kt` | Ajustes, permissões e diagnóstico |
| `ui/CarScreen.kt`, `ui/CarArt.kt`, `CarPhoto.kt` | Tela Meu carro e desenho/foto do carro |
| `obd/Obd.kt`, `obd/Dtc.kt` | ELM327: PIDs do modo 01, consumo, códigos de falha (modo 03) e modo demonstração |
| `Media.kt` | Sessões de mídia (música tocando, controles) |
| `Drive.kt` | Velocidade pelo GPS, trava de vídeo e serviço em primeiro plano |
| `Apps.kt` | Lista de apps, abrir apps, tela dividida |
| `Prefs.kt` | Ajustes salvos |
| `tools/barra_central.py` | Gera a imagem da faixa da barra de cima (Pillow) |

Notas técnicas sobre a central (encaixe de janelas, SystemUI, launcher padrão, ADB): [docs/central-qf005.md](docs/central-qf005.md). Histórico de versões: [CHANGELOG.md](CHANGELOG.md).

## Licença

O código está sob a [licença MIT](LICENSE): pode usar, modificar e distribuir, mantendo o aviso de copyright.

**Não** entram na licença os nomes RotivSecurity e RotivMedia, o logo do dragão, o ícone do app e a faixa da marca (`barra_central.png`). Esses são todos os direitos reservados. Quem fizer um fork deve trocá-los pelos seus.

## Créditos

- Fonte **Space Grotesk** (Florian Karsten), licença SIL Open Font License 1.1.
- Marca, dragão e identidade visual: RotivSecurity.
