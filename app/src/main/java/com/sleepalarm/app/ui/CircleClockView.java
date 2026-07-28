package com.sleepalarm.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * iOS风格环形时钟View - 两个可拖拽圆点代表就寝和起床时间
 * 中间高亮弧线代表睡眠时长，点击弧线可整体拖拽
 */
public class CircleClockView extends View {

    // 颜色
    private final int colorBedtime = 0xFF5B7FFF;     // 蓝色 - 就寝
    private final int colorWake = 0xFFFF9F43;         // 橙色 - 起床
    private final int colorArcStart = 0xFF2D3B6B;     // 弧线起始色（深蓝）
    private final int colorArcEnd = 0xFF1A2240;       // 弧线结束色（更深蓝）
    private final int colorTrack = 0xFF2A2A3E;        // 轨道颜色
    private final int colorBg = 0xFF1C1C2E;           // 背景
    private final int colorText = 0xFFFFFFFF;
    private final int colorSubText = 0xFF8E8E9A;
    private final int colorHighlightArc = 0x335B7FFF; // 高亮弧线半透明

    // 尺寸
    private float centerX, centerY, radius;
    private float trackWidth = 28f;          // 轨道宽度
    private float dotRadius = 18f;           // 拖拽点半径
    private float dotStrokeWidth = 4f;       // 拖拽点描边宽度
    private float tickLength = 10f;          // 刻度长度

    // 就寝角度（度数，0=12点方向，顺时针）
    private float bedtimeAngle = 330f;  // 22:00 = 330度 (22/24*360)
    // 起床角度
    private float wakeAngle = 105f;     // 07:00 = 105度 (7/24*360)

    // 触摸状态
    private int activeDrag = 0; // 0=none, 1=bedtime dot, 2=wake dot, 3=arc
    private float dragStartAngle;
    private float dragStartBedtimeAngle;
    private float dragStartWakeAngle;

    // 回调
    private OnTimeChangedListener listener;

    // 画笔
    private Paint trackPaint;
    private Paint arcPaint;
    private Paint dotBedtimePaint;
    private Paint dotWakePaint;
    private Paint dotStrokePaint;
    private Paint tickPaint;
    private Paint textPaint;
    private Paint timeBigPaint;
    private Paint labelPaint;
    private Paint iconPaint;
    private Paint sleepArcPaint;

    // 弧形路径
    private RectF arcRect;
    private Path arcPath;

    public interface OnTimeChangedListener {
        void onTimesChanged(int bedtimeHour, int bedtimeMinute, int wakeHour, int wakeMinute);
    }

    public CircleClockView(Context context) {
        super(context);
        init();
    }

    public CircleClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 轨道画笔
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(trackWidth);
        trackPaint.setColor(colorTrack);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        // 高亮睡眠弧线
        sleepArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sleepArcPaint.setStyle(Paint.Style.STROKE);
        sleepArcPaint.setStrokeWidth(trackWidth);
        sleepArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 就寝点画笔
        dotBedtimePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBedtimePaint.setStyle(Paint.Style.FILL);
        dotBedtimePaint.setColor(colorBedtime);

        // 起床点画笔
        dotWakePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotWakePaint.setStyle(Paint.Style.FILL);
        dotWakePaint.setColor(colorWake);

        // 点描边
        dotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotStrokePaint.setStyle(Paint.Style.STROKE);
        dotStrokePaint.setStrokeWidth(dotStrokeWidth);
        dotStrokePaint.setColor(Color.WHITE);

        // 刻度画笔
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1.5f);
        tickPaint.setColor(0xFF4A4A5E);

        // 文字画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(colorSubText);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 时间大字画笔
        timeBigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        timeBigPaint.setColor(colorText);
        timeBigPaint.setTextSize(26f);
        timeBigPaint.setTextAlign(Paint.Align.CENTER);
        timeBigPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // 标签画笔
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(colorSubText);
        labelPaint.setTextSize(22f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // 图标画笔
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(28f);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        arcRect = new RectF();
        arcPath = new Path();
    }

    /**
     * 从时间计算角度（0=12点，顺时针）
     */
    private float timeToAngle(int hour, int minute) {
        float totalMinutes = hour * 60f + minute;
        return (totalMinutes / 1440f) * 360f;
    }

    /**
     * 从角度计算小时
     */
    private int angleToHour(float angle) {
        float totalMinutes = (angle % 360f + 360f) % 360f / 360f * 1440f;
        return ((int) (totalMinutes / 60f)) % 24;
    }

    /**
     * 从角度计算分钟
     */
    private int angleToMinute(float angle) {
        float totalMinutes = (angle % 360f + 360f) % 360f / 360f * 1440f;
        return ((int) totalMinutes) % 60;
    }

    /**
     * 角度转坐标
     */
    private float[] angleToPoint(float angle) {
        float rad = (float) Math.toRadians(angle - 90); // 0度在12点方向
        return new float[]{
                centerX + radius * (float) Math.cos(rad),
                centerY + radius * (float) Math.sin(rad)
        };
    }

    /**
     * 坐标转角度
     */
    private float pointToAngle(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float rad = (float) Math.atan2(dy, dx);
        float deg = (float) Math.toDegrees(rad) + 90; // 0度在12点
        if (deg < 0) deg += 360;
        return deg;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 60f;
        float size = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = (size / 2f) - padding - dotRadius;
        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(colorBg);

        // 1. 小时刻度
        drawTicks(canvas);

        // 2. 轨道圆环
        canvas.drawArc(arcRect, 0, 360, false, trackPaint);

        // 3. 睡眠高亮弧线（从就寝角到起床角，顺时针）
        float sleepStart = bedtimeAngle;
        float sleepSweep = (wakeAngle - bedtimeAngle + 360) % 360;
        sleepArcPaint.setShader(null);
        sleepArcPaint.setColor(colorHighlightArc);
        canvas.drawArc(arcRect, sleepStart, sleepSweep, false, sleepArcPaint);

        // 渐变高亮弧线（外层细线效果）
        Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(trackWidth * 0.6f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);

        // 用渐变绘制从就寝到起床的弧线
        float[] pts = angleToPoint(bedtimeAngle);
        float[] pte = angleToPoint(wakeAngle);
        SweepGradient gradient = new SweepGradient(centerX, centerY,
                new int[]{colorBedtime, colorWake},
                new float[]{bedtimeAngle / 360f, wakeAngle / 360f});
        glowPaint.setShader(gradient);
        glowPaint.setAlpha(180);
        canvas.drawArc(arcRect, sleepStart, sleepSweep, false, glowPaint);

        // 4. 拖拽圆点
        drawDot(canvas, bedtimeAngle, colorBedtime);
        drawDot(canvas, wakeAngle, colorWake);

        // 5. 中心文字
        drawCenterText(canvas);
    }

    private void drawTicks(Canvas canvas) {
        for (int h = 0; h < 24; h++) {
            float angle = timeToAngle(h, 0);
            float[] p1 = angleToPointAtRadius(angle, radius - trackWidth / 2 - 4);
            float[] p2 = angleToPointAtRadius(angle, radius - trackWidth / 2 - 4 - tickLength);
            canvas.drawLine(p1[0], p1[1], p2[0], p2[1], tickPaint);

            // 小时数字
            float[] tp = angleToPointAtRadius(angle, radius - trackWidth / 2 - 4 - tickLength - 16);
            String label;
            if (h == 0) label = "0";
            else if (h == 6) label = "6";
            else if (h == 12) label = "12";
            else if (h == 18) label = "18";
            else label = String.valueOf(h);

            textPaint.setTextSize(22f);
            canvas.drawText(label, tp[0], tp[1] + 8, textPaint);
        }
    }

    private float[] angleToPointAtRadius(float angle, float r) {
        float rad = (float) Math.toRadians(angle - 90);
        return new float[]{
                centerX + r * (float) Math.cos(rad),
                centerY + r * (float) Math.sin(rad)
        };
    }

    private void drawDot(Canvas canvas, float angle, int color) {
        float[] p = angleToPoint(angle);

        // 外阴影
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(color);
        shadowPaint.setAlpha(60);
        canvas.drawCircle(p[0], p[1], dotRadius + 6, shadowPaint);

        // 主体
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(color);
        canvas.drawCircle(p[0], p[1], dotRadius, dotPaint);

        // 白色描边
        canvas.drawCircle(p[0], p[1], dotRadius, dotStrokePaint);

        // 图标
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(dotRadius * 1.1f);
        String icon = (color == colorWake) ? "☀" : "☾";
        canvas.drawText(icon, p[0], p[1] + dotRadius * 0.35f, iconPaint);
    }

    private void drawCenterText(Canvas canvas) {
        float cy = centerY;

        // 就寝时间
        int bh = angleToHour(bedtimeAngle);
        int bm = angleToMinute(bedtimeAngle);
        String bTime = String.format("%02d:%02d", bh, bm);
        timeBigPaint.setColor(colorBedtime);
        canvas.drawText(bTime, centerX, cy - 4, timeBigPaint);

        // 起床时间
        int wh = angleToHour(wakeAngle);
        int wm = angleToMinute(wakeAngle);
        String wTime = String.format("%02d:%02d", wh, wm);
        timeBigPaint.setColor(colorWake);
        canvas.drawText(wTime, centerX, cy + 32, timeBigPaint);

        // 睡眠时长
        float totalMin = ((wakeAngle - bedtimeAngle + 360) % 360) / 360f * 1440f;
        int sleepH = (int) (totalMin / 60);
        int sleepM = (int) (totalMin % 60);
        String duration = sleepH + "h " + sleepM + "m";
        labelPaint.setTextSize(20f);
        labelPaint.setColor(0xFF6B6B7B);
        canvas.drawText("睡眠 " + duration, centerX, cy + 58, labelPaint);

        // 就寝和起床标签
        labelPaint.setTextSize(18f);
        labelPaint.setColor(colorBedtime);
        canvas.drawText("就寝", centerX, cy - 30, labelPaint);
        labelPaint.setColor(colorWake);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float touchAngle = pointToAngle(x, y);
        float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
        float touchSlop = 40f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 检查是否触摸了就寝点
                float[] bp = angleToPoint(bedtimeAngle);
                if (distToPoint(x, y, bp[0], bp[1]) < dotRadius + touchSlop) {
                    activeDrag = 1;
                    dragStartAngle = touchAngle;
                    return true;
                }
                // 检查是否触摸了起床点
                float[] wp = angleToPoint(wakeAngle);
                if (distToPoint(x, y, wp[0], wp[1]) < dotRadius + touchSlop) {
                    activeDrag = 2;
                    dragStartAngle = touchAngle;
                    return true;
                }
                // 检查是否触摸了弧线（靠近轨道）
                if (Math.abs(dist - radius) < trackWidth + touchSlop) {
                    // 检查触摸角度是否在睡眠弧线上
                    if (isAngleInSleepArc(touchAngle)) {
                        activeDrag = 3;
                        dragStartBedtimeAngle = bedtimeAngle;
                        dragStartWakeAngle = wakeAngle;
                        dragStartAngle = touchAngle;
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (activeDrag == 0) break;

                float deltaAngle = touchAngle - dragStartAngle;

                if (activeDrag == 1) {
                    // 拖拽就寝点：仅更新就寝角度
                    bedtimeAngle = (dragStartAngle + deltaAngle - bedtimeAngle + bedtimeAngle);
                    // 换算：保持初始的偏移量
                    bedtimeAngle = normalizeAngle(bedtimeAngle + deltaAngle - (touchAngle - dragStartAngle));
                    // 简化：直接使用当前触摸角度
                    bedtimeAngle = normalizeAngle(touchAngle);
                } else if (activeDrag == 2) {
                    wakeAngle = normalizeAngle(touchAngle);
                } else if (activeDrag == 3) {
                    // 整体拖拽：保持睡眠时长不变
                    float angleDelta = touchAngle - dragStartAngle;
                    bedtimeAngle = normalizeAngle(dragStartBedtimeAngle + angleDelta);
                    wakeAngle = normalizeAngle(dragStartWakeAngle + angleDelta);
                    dragStartAngle = touchAngle;
                    dragStartBedtimeAngle = bedtimeAngle;
                    dragStartWakeAngle = wakeAngle;
                }

                // 确保就寝和起床不重叠（至少10分钟间隔）
                float minGap = (10f / 1440f) * 360f;
                if (activeDrag == 1 || activeDrag == 3) {
                    float gap = (wakeAngle - bedtimeAngle + 360) % 360;
                    if (gap < minGap) {
                        bedtimeAngle = normalizeAngle(wakeAngle - minGap);
                    }
                }
                if (activeDrag == 2 || activeDrag == 3) {
                    float gap = (wakeAngle - bedtimeAngle + 360) % 360;
                    if (gap < minGap) {
                        wakeAngle = normalizeAngle(bedtimeAngle + minGap);
                    }
                }

                invalidate();
                notifyListener();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeDrag = 0;
                notifyListener();
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 检查角度是否在睡眠弧线范围内（从就寝顺时针到起床）
     */
    private boolean isAngleInSleepArc(float angle) {
        float start = bedtimeAngle;
        float end = wakeAngle;
        if (end < start) end += 360;
        float a = angle;
        if (a < start) a += 360;
        return a >= start && a <= end;
    }

    private float normalizeAngle(float angle) {
        return ((angle % 360) + 360) % 360;
    }

    private float distToPoint(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onTimesChanged(
                    angleToHour(bedtimeAngle), angleToMinute(bedtimeAngle),
                    angleToHour(wakeAngle), angleToMinute(wakeAngle)
            );
        }
    }

    // ===== 公开方法 =====

    public void setOnTimeChangedListener(OnTimeChangedListener l) {
        this.listener = l;
    }

    public void setBedtime(int hour, int minute) {
        this.bedtimeAngle = timeToAngle(hour, minute);
        invalidate();
    }

    public void setWakeTime(int hour, int minute) {
        this.wakeAngle = timeToAngle(hour, minute);
        invalidate();
    }

    public int getBedtimeHour() { return angleToHour(bedtimeAngle); }
    public int getBedtimeMinute() { return angleToMinute(bedtimeAngle); }
    public int getWakeHour() { return angleToHour(wakeAngle); }
    public int getWakeMinute() { return angleToMinute(wakeAngle); }

    public float getBedtimeAngle() { return bedtimeAngle; }
    public float getWakeAngle() { return wakeAngle; }
}
