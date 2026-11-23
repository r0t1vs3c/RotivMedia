package com.rotivsecurity.launcher.obd

/** Códigos de falha (luz de injeção): leitura do modo 03 e explicação em português. */
object Dtc {

    /** Converte a resposta do comando "03" em códigos como "P0171". */
    fun parse(resp: String): List<String> {
        if (resp.contains("NODATA", ignoreCase = true)) return emptyList()
        val hex = resp.lines()
            .map { it.substringAfter(":") } // respostas CAN com várias linhas: "0:43..."
            .filter { it.length > 3 } // ignora a linha com o total de bytes
            .joinToString("") { line -> if (line.startsWith("43")) line.substring(2) else line }
        var data = hex
        // Em CAN o primeiro byte é a quantidade de códigos (sobra um byte ímpar).
        if ((data.length / 2) % 2 == 1) data = data.substring(2)
        val codes = mutableListOf<String>()
        var i = 0
        while (i + 4 <= data.length) {
            val b0 = data.substring(i, i + 2).toIntOrNull(16)
            val b1 = data.substring(i + 2, i + 4).toIntOrNull(16)
            i += 4
            if (b0 == null || b1 == null || (b0 == 0 && b1 == 0)) continue
            val letter = "PCBU"[(b0 shr 6) and 0x3]
            val code = "$letter${(b0 shr 4) and 0x3}${"%X".format(b0 and 0xF)}${"%02X".format(b1)}"
            if (code !in codes) codes.add(code)
        }
        return codes
    }

    fun explain(code: String): String {
        known[code]?.let { return it }
        return when {
            code.startsWith("P030") -> "Falha de ignição no cilindro ${code.last()}"
            code.startsWith("P0") -> "Motor ou câmbio (código genérico)"
            code.startsWith("P1") || code.startsWith("P3") -> "Código específico da montadora"
            code.startsWith("C") -> "Chassi (freios, suspensão, direção)"
            code.startsWith("B") -> "Carroceria (airbag, travas, conforto)"
            code.startsWith("U") -> "Comunicação entre módulos eletrônicos"
            else -> "Código não identificado"
        }
    }

    private val known = mapOf(
        "P0100" to "Sensor de fluxo de ar (MAF) com defeito",
        "P0101" to "Sensor de fluxo de ar (MAF) fora da faixa",
        "P0102" to "Sinal baixo do sensor de fluxo de ar (MAF)",
        "P0103" to "Sinal alto do sensor de fluxo de ar (MAF)",
        "P0105" to "Sensor de pressão do coletor (MAP) com defeito",
        "P0106" to "Sensor de pressão do coletor (MAP) fora da faixa",
        "P0107" to "Sinal baixo do sensor MAP",
        "P0108" to "Sinal alto do sensor MAP",
        "P0110" to "Sensor de temperatura do ar com defeito",
        "P0115" to "Sensor de temperatura do motor com defeito",
        "P0116" to "Sensor de temperatura do motor fora da faixa",
        "P0117" to "Sinal baixo do sensor de temperatura do motor",
        "P0118" to "Sinal alto do sensor de temperatura do motor",
        "P0120" to "Sensor de posição do acelerador (TPS) com defeito",
        "P0121" to "Sensor de posição do acelerador (TPS) fora da faixa",
        "P0130" to "Sonda lambda (sensor de oxigênio) 1 com defeito",
        "P0131" to "Sinal baixo da sonda lambda 1",
        "P0132" to "Sinal alto da sonda lambda 1",
        "P0133" to "Sonda lambda 1 respondendo devagar",
        "P0134" to "Sonda lambda 1 sem atividade",
        "P0135" to "Aquecimento da sonda lambda 1 com defeito",
        "P0136" to "Sonda lambda 2 (depois do catalisador) com defeito",
        "P0141" to "Aquecimento da sonda lambda 2 com defeito",
        "P0171" to "Mistura pobre: entrando ar demais ou pouco combustível",
        "P0172" to "Mistura rica: combustível demais",
        "P0300" to "Falhas de ignição aleatórias (velas, cabos, bobinas)",
        "P0325" to "Sensor de detonação com defeito",
        "P0335" to "Sensor de rotação do virabrequim com defeito",
        "P0340" to "Sensor de fase (comando de válvulas) com defeito",
        "P0401" to "Fluxo insuficiente da válvula EGR",
        "P0420" to "Catalisador com eficiência baixa",
        "P0440" to "Sistema de vapores do tanque (EVAP) com defeito",
        "P0441" to "Fluxo incorreto da purga do cânister",
        "P0442" to "Pequeno vazamento no sistema EVAP (tampa do tanque?)",
        "P0443" to "Válvula de purga do cânister com defeito",
        "P0455" to "Grande vazamento no sistema EVAP (tampa do tanque?)",
        "P0500" to "Sensor de velocidade do veículo com defeito",
        "P0505" to "Controle da marcha lenta com defeito",
        "P0506" to "Marcha lenta abaixo do esperado",
        "P0507" to "Marcha lenta acima do esperado",
        "P0560" to "Tensão do sistema elétrico anormal",
        "P0562" to "Tensão do sistema elétrico baixa (bateria ou alternador)",
        "P0563" to "Tensão do sistema elétrico alta",
        "P0601" to "Erro de memória da central do motor (ECU)",
        "P0700" to "Falha no sistema do câmbio automático",
    )
}
