"""
Gera o fundo da barra de cima da central (1280x72) com a identidade RotivMedia.

Nesta central (QF005 em tela deitada) a barra de cima é transparente: o firmware só
usa a pasta statusbarbg em modelos de tela em pé. Por isso a imagem vira o recurso
res/drawable-nodpi/barra_central.png, que a tela inicial desenha atrás da barra.
Ícones, relógio e botões da central ficam por cima; o centro fica livre para a marca.

Uso: python barra_central.py   (atualiza o recurso do app)
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, "..", "app", "src", "main", "res")
W, H = 1280, 72

DARK = (7, 11, 20)
DARK2 = (11, 17, 32)
ACC = (59, 130, 246)


def fonte(tam):
    f = ImageFont.truetype(os.path.join(RES, "font", "space_grotesk.ttf"), tam)
    try:
        f.set_variation_by_name("Bold")
    except Exception:
        pass
    return f


def gerar():
    img = Image.new("RGBA", (W, H), DARK + (255,))
    px = img.load()
    # leve degradê vertical: Dark em cima, Dark2 embaixo
    for y in range(H):
        t = y / (H - 1)
        c = tuple(round(DARK[i] + (DARK2[i] - DARK[i]) * t) for i in range(3))
        for x in range(W):
            px[x, y] = c + (255,)

    # brilho azul bem suave atrás da marca
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((W // 2 - 190, 8, W // 2 + 190, H + 40), fill=ACC + (34,))
    glow = glow.filter(ImageFilter.GaussianBlur(22))
    img = Image.alpha_composite(img, glow)

    # linha de baixo: azul no centro, sumindo nas pontas
    linha = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    lp = linha.load()
    for x in range(W):
        d = abs(x - W / 2) / (W / 2)
        a = max(0.0, 1 - d * 1.15) ** 1.6
        for y in (H - 2, H - 1):
            lp[x, y] = ACC + (round(200 * a),)
    img = Image.alpha_composite(img, linha)

    # marca: dragão + ROTIVMEDIA, centralizados
    logo = Image.open(os.path.join(RES, "drawable-nodpi", "logo_dragon.png")).convert("RGBA")
    lh = 34
    logo = logo.resize((round(logo.width * lh / logo.height), lh), Image.LANCZOS)
    f = fonte(17)
    texto = "ROTIVMEDIA"
    esp = 4  # espaço entre letras
    larg_letras = [f.getbbox(ch)[2] - f.getbbox(ch)[0] if ch != " " else 6 for ch in texto]
    larg_texto = sum(larg_letras) + esp * (len(texto) - 1)
    gap = 12
    total = logo.width + gap + larg_texto
    x0 = (W - total) // 2
    img.alpha_composite(logo, (x0, (H - lh) // 2))
    d = ImageDraw.Draw(img)
    asc, desc = f.getmetrics()
    ty = (H - (asc + desc)) // 2 + 1
    x = x0 + logo.width + gap
    for ch, lw in zip(texto, larg_letras):
        d.text((x - f.getbbox(ch)[0], ty), ch, font=f, fill=(255, 255, 255, 235))
        x += lw + esp

    destino = os.path.join(RES, "drawable-nodpi", "barra_central.png")
    img.convert("RGB").save(destino, optimize=True)
    print("ok:", destino)


if __name__ == "__main__":
    gerar()
