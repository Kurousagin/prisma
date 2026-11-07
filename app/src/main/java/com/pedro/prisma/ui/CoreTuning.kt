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
        else -> emptyArray()
    }
}
