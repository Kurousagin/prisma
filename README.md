# Prisma

Emulador de clássicos para Android com cara de console: biblioteca com capas automáticas,
controles na tela e foco em **rodar liso em celulares de hardware modesto**.

Grátis, sem anúncios e **código aberto**. Você traz suas próprias ROMs — o Prisma não distribui jogos.

## Consoles (nativos, sem depender de outros apps)

NES · Super Nintendo · Game Boy / Color · Game Boy Advance · Mega Drive / Genesis / SMS / GG

## Como funciona

- Coloque suas ROMs em `/sdcard/Roms` (ou numa pasta `Roms` num pendrive USB).
- A biblioteca monta sozinha e baixa as capas.
- Save automático: fecha o jogo e volta de onde parou.
- Controles na tela (D-pad, A/B/X/Y, L/R, Start/Select) ou um 2º celular como controle via QR.

## Build

Projeto Gradle padrão (AGP 8.5 / Kotlin / Jetpack Compose). Emulação via
[LibretroDroid](https://github.com/Swordfish90/LibretroDroid) com cores libretro embutidos
em `app/src/main/jniLibs/arm64-v8a/`.

```
./gradlew assembleDebug
```

## Licença

Código do app sob **GPLv3** (LibretroDroid é GPLv3). Os cores de emulação pertencem aos
respectivos projetos libretro, sob suas próprias licenças. Projeto sem fins comerciais.

## Apoie

Grátis pra sempre. Se te ajudou, um Pix mantém o projeto vivo — a chave está na aba "Apoiar" do app.
