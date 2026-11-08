package com.pedro.prisma.ui

import com.swordfish.libretrodroid.Variable

/**
 * Ajustes por core pra rodar liso em hardware fraco.
 * As chaves/valores foram conferidos nos próprios .so (opções libretro válidas).
 * Frameskip não entra: o modo "auto" exige status do buffer de áudio que o
 * LibretroDroid não reporta, e frameskip fixo só geraria estalo no som.
 */
object CoreTuning {
    fun variables(core: String): Array<Variable> = when {
        // GBA (mgba) é o único core pesado do conjunto: remover loops ociosos
        // acelera bastante em CPU fraca, sem efeito colateral de imagem/som.
        core.contains("mgba") -> arrayOf(Variable("mgba_idle_optimization", "Detect and Remove"))
        // N64 (mupen64plus): perfil pra hardware modesto — resolução nativa (maior
        // ganho), RSP em HLE, recompilador dinâmico e filtro bilinear barato (3point,
        // o modo original do console). GLideN64 (gles3) é o plugin de vídeo padrão.
        core.contains("mupen64plus") -> arrayOf(
            Variable("mupen64plus-cpucore", "dynamic_recompiler"),
            Variable("mupen64plus-rsp-plugin", "hle"),
            Variable("mupen64plus-43screensize", "320x240"),
            Variable("mupen64plus-BilinearMode", "3point"),
        )
        else -> emptyArray()
    }
}
