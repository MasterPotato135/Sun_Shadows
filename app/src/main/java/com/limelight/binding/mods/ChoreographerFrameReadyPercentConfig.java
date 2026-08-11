package com.limelight.mods;

public final class ChoreographerFrameReadyPercentConfig {
    /**
     * Percentual do intervalo de frame (em décimos, base 1000) usado para
     * calcular o delta mínimo antes de o Choreographer renderizar o próximo frame.
     * 800/1000 = 80% → renderiza quando já passou 80% do intervalo esperado,
     * evitando microstutter em streams com FPS diferente do refresh rate do display.
     */
    public static final int CHOREOGRAPHER_FRAME_READY_PERMILLE = 800;
}
