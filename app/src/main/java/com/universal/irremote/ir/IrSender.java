package com.universal.irremote.ir;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends IR patterns through the phone's IR blaster on a background thread. */
public final class IrSender {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private IrSender() {}

    public static ConsumerIrManager manager(Context ctx) {
        return (ConsumerIrManager) ctx.getApplicationContext().getSystemService(Context.CONSUMER_IR_SERVICE);
    }

    public static boolean hasEmitter(Context ctx) {
        ConsumerIrManager ir = manager(ctx);
        return ir != null && ir.hasIrEmitter();
    }

    public static void send(Context ctx, IrPattern p) {
        ConsumerIrManager ir = manager(ctx);
        Context app = ctx.getApplicationContext();
        if (ir == null || !ir.hasIrEmitter()) {
            Toast.makeText(app, "This phone has no IR blaster", Toast.LENGTH_SHORT).show();
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                ir.transmit(p.frequency, p.pattern);
            } catch (RuntimeException e) {
                MAIN.post(() -> Toast.makeText(app, "IR send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** Parses and sends a stored code string, showing a message if it is invalid. */
    public static void sendCode(Context ctx, String code) {
        try {
            send(ctx, IrCodes.parse(code));
        } catch (RuntimeException e) {
            Toast.makeText(ctx, "Bad code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
