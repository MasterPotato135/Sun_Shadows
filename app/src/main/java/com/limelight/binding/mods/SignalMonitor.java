package com.limelight.mods;

import android.content.Context;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.limelight.LimeLog;
import com.limelight.nvstream.StreamConfiguration;

/**
 * Monitora a qualidade do sinal LTE e ajusta automaticamente o {@code jumpFrameMode}
 * com base nos valores de RSRP (potência) e SINR (qualidade) reportados pelo sistema.
 *
 * Esta lógica foi extraída de {@code MediaCodecDecoderRenderer} (métodos
 * {@code startSignalMonitoring()} e {@code stopSignalMonitoring()}) para isolar
 * a dependência de telefonia do renderer de vídeo e centralizar os limiares
 * de sinal nos /mods/.
 *
 * Uso:
 *   monitor = new SignalMonitor(context, jumpFrameModeCallback, baseMode);
 *   monitor.start(bitrateOptimizer.getHandler());   // registra no Looper correto
 *   monitor.stop();                                 // para e restaura modo base
 */
public class SignalMonitor {

    // ── Constantes centralizadas nos /mods/ ──────────────────────────────────
    private static final int RSRP_GOOD      = SignalRsrpGoodConfig.SIGNAL_RSRP_GOOD;       // -95  dBm
    private static final int RSRP_MEDIUM    = SignalRsrpMediumConfig.SIGNAL_RSRP_MEDIUM;   // -105 dBm
    private static final int RSRP_POOR      = SignalRsrpPoorConfig.SIGNAL_RSRP_POOR;       // -115 dBm
    private static final int RSRP_MIN_VALID = SignalRsrpMinValidConfig.SIGNAL_RSRP_MIN_VALID; // -30 dBm
    private static final int SINR_LOW       = SignalSinrLowConfig.SIGNAL_SINR_LOW;         //  30 (= 3.0 dB × 10)

    // ── Callback para notificar mudança de modo ──────────────────────────────

    /** Interface implementada pelo renderer para receber mudanças de jumpFrameMode. */
    public interface JumpFrameModeCallback {
        void onJumpFrameModeChanged(int newMode);
    }

    // ── Estado interno ───────────────────────────────────────────────────────
    private final Context context;
    private final JumpFrameModeCallback callback;
    private final int baseJumpFrameMode;

    private TelephonyManager telephonyManager;
    private PhoneStateListener signalListener;
    private int currentMode;

    /**
     * @param context           contexto Android (Activity ou Application).
     * @param callback          receptor de mudanças de jumpFrameMode (tipicamente o renderer).
     * @param baseJumpFrameMode modo base configurado pelo usuário (antes do monitoramento escalar).
     */
    public SignalMonitor(Context context, JumpFrameModeCallback callback, int baseJumpFrameMode) {
        this.context         = context;
        this.callback        = callback;
        this.baseJumpFrameMode = baseJumpFrameMode;
        this.currentMode     = baseJumpFrameMode;
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    /**
     * Registra o PhoneStateListener no {@code looperHandler} fornecido.
     *
     * <p>O {@code PhoneStateListener} exige uma thread com Looper. {@code setup()} do
     * renderer é chamado a partir de uma thread sem Looper, por isso o registro é
     * despachado no Handler do {@link BitrateOptimizer}, que já possui Looper próprio.
     *
     * @param looperHandler Handler cujo Looper receberá os callbacks de sinal.
     *                      Normalmente {@code bitrateOptimizer.getHandler()}.
     */
    public void start(final Handler looperHandler) {
        looperHandler.post(new Runnable() {
            @Override
            public void run() {
                registerListener();
            }
        });
    }

    /**
     * Para o monitoramento de sinal e restaura o jumpFrameMode base do usuário.
     * Deve ser chamado no Looper onde o listener foi registrado (via {@code start()}),
     * ou seja, ainda dentro do Handler do BitrateOptimizer, antes de {@code stop()}.
     */
    public void stop() {
        if (telephonyManager != null && signalListener != null) {
            try {
                telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_NONE);
            } catch (Exception e) {
                // ignora — o listener pode já ter sido desregistrado
            }
            signalListener   = null;
            telephonyManager = null;
            // Restaura o modo original configurado pelo usuário
            if (callback != null) {
                callback.onJumpFrameModeChanged(baseJumpFrameMode);
            }
            LimeLog.info("SignalMonitor: parado, jumpFrameMode restaurado para " + baseJumpFrameMode);
        }
    }

    // ── Implementação privada ────────────────────────────────────────────────

    private void registerListener() {
        try {
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                LimeLog.warning("SignalMonitor: TelephonyManager indisponível");
                return;
            }

            signalListener = new PhoneStateListener() {
                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    if (signalStrength == null) return;

                    // Lê RSRP e SINR via reflexão — getLteRsrp()/getLteRssnr() existem desde
                    // API 17 (ocultos) e são públicos a partir do API 29. Reflexão resolve isso
                    // sem @RequiresApi e sem precisar elevar compileSdk.
                    int rsrp = Integer.MIN_VALUE;
                    int sinr = Integer.MAX_VALUE;
                    try {
                        java.lang.reflect.Method mRsrp = signalStrength.getClass().getMethod("getLteRsrp");
                        java.lang.reflect.Method mSinr = signalStrength.getClass().getMethod("getLteRssnr");
                        Object rsrpObj = mRsrp.invoke(signalStrength);
                        Object sinrObj = mSinr.invoke(signalStrength);
                        if (rsrpObj instanceof Integer) rsrp = (Integer) rsrpObj;
                        if (sinrObj instanceof Integer) sinr = (Integer) sinrObj;
                    } catch (Exception e) {
                        // Dispositivo não reporta LTE ou API não disponível; usa fallback getLevel()
                    }

                    boolean rsrpValid = rsrp != Integer.MIN_VALUE && rsrp < RSRP_MIN_VALID;
                    boolean sinrValid = sinr != Integer.MAX_VALUE;

                    int newMode = computeMode(rsrpValid, rsrp, sinrValid, sinr,
                            signalStrength.getLevel());

                    if (newMode != currentMode) {
                        LimeLog.info("SignalMonitor: RSRP=" + (rsrpValid ? rsrp + " dBm" : "n/a")
                                + " SINR=" + (sinrValid ? (sinr / 10.0) + " dB" : "n/a")
                                + " → jumpFrameMode " + currentMode + " → " + newMode);
                        currentMode = newMode;
                        if (callback != null) {
                            callback.onJumpFrameModeChanged(newMode);
                        }
                    }
                }
            };

            telephonyManager.listen(signalListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            LimeLog.info("SignalMonitor: iniciado (baseJumpFrameMode=" + baseJumpFrameMode + ")");

        } catch (SecurityException e) {
            // Permissão READ_PHONE_STATE não concedida; monitoramento não ativa.
            LimeLog.warning("SignalMonitor: permissão negada, monitoramento desativado");
            signalListener   = null;
            telephonyManager = null;
        }
    }

    /**
     * Mapeia os valores de RSRP / SINR (ou fallback getLevel()) para um jumpFrameMode.
     *
     * Limiares (centralizados nos /mods/):
     *   RSRP ≥ -95  dBm → bom  → baseJumpFrameMode
     *   RSRP ≥ -105 dBm → médio → max(base, LIGHT)
     *   RSRP ≥ -115 dBm → ruim  → max(base, MEDIUM)
     *   RSRP <  -115 dBm → crítico → HEAVY
     *   SINR < 3.0 dB (30 em décimos) → sobe um nível adicional
     */
    private int computeMode(boolean rsrpValid, int rsrp,
                            boolean sinrValid, int sinr,
                            int signalLevel) {
        int newMode;
        if (rsrpValid) {
            if (rsrp >= RSRP_GOOD) {
                newMode = baseJumpFrameMode;
            } else if (rsrp >= RSRP_MEDIUM) {
                newMode = Math.max(baseJumpFrameMode, StreamConfiguration.JUMPFRAME_MODE_LIGHT);
            } else if (rsrp >= RSRP_POOR) {
                newMode = Math.max(baseJumpFrameMode, StreamConfiguration.JUMPFRAME_MODE_MEDIUM);
            } else {
                newMode = StreamConfiguration.JUMPFRAME_MODE_HEAVY;
            }
            // Refinamento: SINR muito baixo → sobe um nível adicional
            if (sinrValid && sinr < SINR_LOW && newMode < StreamConfiguration.JUMPFRAME_MODE_HEAVY) {
                newMode = Math.min(newMode + 1, StreamConfiguration.JUMPFRAME_MODE_HEAVY);
            }
        } else {
            // Fallback: usa nível geral do sinal (0–4)
            int level = signalLevel;
            if (level < 0) level = 0;
            if (level > 4) level = 4;
            if (level >= 3) {
                newMode = baseJumpFrameMode;
            } else if (level == 2) {
                newMode = Math.max(baseJumpFrameMode, StreamConfiguration.JUMPFRAME_MODE_LIGHT);
            } else if (level == 1) {
                newMode = Math.max(baseJumpFrameMode, StreamConfiguration.JUMPFRAME_MODE_MEDIUM);
            } else {
                newMode = StreamConfiguration.JUMPFRAME_MODE_HEAVY;
            }
        }
        return newMode;
    }
}
