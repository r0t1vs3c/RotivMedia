# Histórico de versões

## 0.6.4
- Os atalhos da direita deram lugar ao painel **Meu carro** com dados do OBD2 ao vivo: giro, motor, bateria, consumo agora, média e km da viagem. Temperatura e bateria ficam vermelhas fora da faixa, e códigos de falha aparecem como aviso.
- Na faixa de baixo, o botão "Meu carro" virou **Apps**.

## 0.6.3
- Corrigido: o RetroArch não abria. O quadro do Maps mudava 5 px ao abrir o RetroArch, o Maps era reaberto e roubava o foco da tela de carregamento do RetroArch. Agora o quadro do Maps tem o mesmo tamanho em todas as telas, e janelas já abertas só são redimensionadas, nunca reabertas.

## 0.6.0 – 0.6.2
- **YouTube Music fixo no centro**: o app de verdade, numa janela encaixada no quadro do meio (chave em Ajustes → Mapa).
- Se o app já estiver em tela cheia, ele é tirado dela e reaberto no quadro.
- O player ignora a sessão de mídia vazia do app Google e sessões paradas ou com erro.
- Abrir YouTube ou RetroArch tira o YouTube Music do centro, e ele volta quando eles fecham.

## 0.5.1 – 0.5.9
- Sem barra própria na tela inicial (duplicava a barra da central). A tela desenha atrás da barra transparente da central uma faixa com a marca RotivMedia.
- Faixa embaixo do Maps com os botões e os controles do app encaixado.
- O quadro do Maps ganhou a mesma borda arredondada dos outros quadros.
- O encaixe tenta de novo quando o quadro muda de tamanho logo depois de abrir.

## 0.5.0
- RetroArch encaixa à direita como o YouTube; fechar pede um segundo toque.
- Botões de fechar fora da área da janela, deixando o quadro maior.
- Boas-vindas só uma vez por boot (ou ao acordar depois de 5 min com a tela apagada).

## 0.4.0 – 0.4.1
- RotivMedia como launcher padrão da central.
- **Maps fixo**: Google Maps encaixado no quadro da esquerda em janela livre.
- YouTube encaixado à direita.
- Ajustes, Apps e Meu carro abrem por cima das janelas encaixadas.

## 0.3.0 – 0.3.6
- Nome RotivMedia.
- Meu carro: carro desenhado por código (4 carrocerias, 13 cores ou cor livre) ou foto.
- OBD2 com ELM327 (só leitura), consumo para gasolina E27 e etanol, códigos de falha em português e modo demonstração.
- ADB remoto: religa a depuração e o Tailscale a cada boot.

## 0.2.0
- Boas-vindas com o dragão, Ajustes, aviso sonoro de velocidade, modo de janelas 40/60 e diagnóstico do Android real.

## 0.1.0
- Primeira versão: GPS à esquerda, YouTube / YouTube Music à direita, atalhos e lista de apps.
