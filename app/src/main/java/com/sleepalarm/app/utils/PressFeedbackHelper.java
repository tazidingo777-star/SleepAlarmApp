package com.sleepalarm.app.utils;

import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * iOS 风格按压反馈：按下时透明度 80%，缩放 0.97，过渡 150ms
 */
public class PressFeedbackHelper {

    public static void apply(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            private boolean isPressed = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!isPressed) {
                            isPressed = true;
                            animatePress(v);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (isPressed) {
                            isPressed = false;
                            animateRelease(v);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private static void animatePress(View v) {
        v.animate()
                .alpha(0.8f)
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private static void animateRelease(View v) {
        v.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(150)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
