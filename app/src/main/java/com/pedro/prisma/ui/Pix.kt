package com.pedro.prisma.ui

/**
 * Monta o "copia e cola" do Pix (BR Code EMV estático) a partir da chave — pra o QR ser
 * escaneável no app do banco. Formato: campos id+tamanho+valor, com CRC16-CCITT no fim.
 */
fun pixPayload(key: String, name: String = "PRISMA", city: String = "SAO PAULO"): String {
    fun tlv(id: String, v: String) = id + "%02d".format(v.length) + v
    val gui = tlv("00", "br.gov.bcb.pix") + tlv("01", key)
    val body = tlv("00", "01") +                 // payload format
        tlv("26", gui) +                         // merchant account info (Pix)
        tlv("52", "0000") +                      // merchant category code
        tlv("53", "986") +                       // moeda BRL
        tlv("58", "BR") +                        // país
        tlv("59", name.take(25)) +               // nome
        tlv("60", city.take(15)) +               // cidade
        tlv("62", tlv("05", "***")) +            // txid
        "6304"                                   // CRC (id+len), valor calculado abaixo
    return body + crc16(body)
}

private fun crc16(s: String): String {
    var crc = 0xFFFF
    for (byte in s.toByteArray(Charsets.UTF_8)) {
        crc = crc xor ((byte.toInt() and 0xFF) shl 8)
        repeat(8) {
            crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
            crc = crc and 0xFFFF
        }
    }
    return "%04X".format(crc)
}
