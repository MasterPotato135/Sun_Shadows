package com.limelight.mods;

public final class BitrateReduceFloorDivisorConfig {
    /**
     * Divisor aplicado ao bitrate alvo original para calcular o piso mínimo
     * do bitrate reduzido. Ex: divisor 4 → floor em 25% do bitrate original.
     * Evita que a redução de cena estática leve o encoder a um bitrate inviável.
     */
    public static final int BITRATE_REDUCE_FLOOR_DIVISOR = 4;
}
