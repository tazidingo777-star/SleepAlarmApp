package com.sleepalarm.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.sleepalarm.app.utils.ThemeColors;

/**
 * iOS 时钟"就寝"风格环形时钟 View
 * - 12 小时制表盘，显示 1-12 数字刻度
 * - 粗橙色弧线表示睡眠时长
 * - 中心显示睡眠时长 "X小时"
 * - 可拖拽的就寝/起床圆点
 */
public class CircleClockView extends View {

    // iOS 风格颜色（由 ThemeColors 动态解析）
    private int colorBackground;
    private int colorTrack;
    private int colorTick;
    private int colorNumber;
    private int colorSleepArc;
    private int colorDotBorder;
    private int colorDotFill;
    private int colorDotIcon;
    private int colorCenterHour;
    private int colorCenterUnit;

    // 尺寸
    private float centerX, centerY;
    private float radius;           // 橙色弧线半径
    private float numberRadius;     // 数字半径
    private float tickOuterRadius;  // 刻度外半径
    private float tickInnerRadius;  // 刻度内半径

    private float arcWidth = 63f;   // 橙色弧线宽度（1.5倍）
    private float dotRadius = 33f;  // 圆点半径（1.5倍）
    private float dotBorderWidth = 4.5f; // 1.5倍
    private float tickLength = 12f;

    // 时间状态（24小时制）
    private int bedtimeHour = 23;
    private int bedtimeMinute = 0;
    private int wakeHour = 6;
    private int wakeMinute = 0;

    // 触摸状态 0=none, 1=bedtime, 2=wake, 3=arc
    private int activeDrag = 0;
    private float dragStartAngle;
    private int dragStartBedtimeTotal;
    private int dragStartWakeTotal;
    private int dragStartSleepMinutes;

    private OnTimeChangedListener listener;

    public interface OnTimeChangedListener {
        void onTimesChanged(int bedtimeHour, int bedtimeMinute, int wakeHour, int wakeMinute);
    }

    // 画笔
    private Paint tickPaint;
    private Paint numberPaint;
    private Paint arcPaint;
    private Paint dotBorderPaint;
    private Paint dotFillPaint;
    private Paint iconPaint;
    private Paint centerHourPaint;
    private Paint centerUnitPaint;
    private Paint sleepBgPaint;

    private RectF arcRect = new RectF();

    public CircleClockView(Context context) {
        super(context);
        init();
    }

    public CircleClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 从 ThemeColors 解析所有颜色
        Context ctx = getContext();
        colorBackground = ThemeColors.getBackground(ctx);
        colorTrack = ThemeColors.getSurface(ctx);
        colorTick = ThemeColors.getTextSecondary(ctx);
        colorNumber = ThemeColors.getTextSecondary(ctx);
        colorSleepArc = ThemeColors.getAccent(ctx);
        colorDotBorder = ThemeColors.getAccent(ctx);
        colorDotFill = ThemeColors.getBackground(ctx);
        colorDotIcon = ThemeColors.getTextPrimary(ctx);
        colorCenterHour = ThemeColors.getTextPrimary(ctx);
        colorCenterUnit = ThemeColors.getTextSecondary(ctx);

        // 刻度
        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(1.5f);
        tickPaint.setColor(colorTick);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        // 数字 - 加大
        numberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numberPaint.setColor(colorNumber);
        numberPaint.setTextSize(38f);
        numberPaint.setTextAlign(Paint.Align.CENTER);

        // 睡眠弧线
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(arcWidth);
        arcPaint.setColor(colorSleepArc);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 圆点描边
        dotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBorderPaint.setStyle(Paint.Style.STROKE);
        dotBorderPaint.setStrokeWidth(dotBorderWidth);
        dotBorderPaint.setColor(colorDotBorder);

        // 圆点填充
        dotFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotFillPaint.setStyle(Paint.Style.FILL);
        dotFillPaint.setColor(colorDotFill);

        // 图标（用字符绘制）
        iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(colorDotIcon);
        iconPaint.setTextSize(28f);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        // 中心小时（2倍大小）
        centerHourPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHourPaint.setColor(colorCenterHour);
        centerHourPaint.setTextSize(128f);
        centerHourPaint.setTextAlign(Paint.Align.CENTER);
        centerHourPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // 中心单位
        centerUnitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerUnitPaint.setColor(colorCenterUnit);
        centerUnitPaint.setTextSize(128f);
        centerUnitPaint.setTextAlign(Paint.Align.CENTER);

        // 睡眠背景（刻度盘浅灰圆）
        sleepBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sleepBgPaint.setStyle(Paint.Style.FILL);
        sleepBgPaint.setColor(colorTrack);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = 48f;
        float size = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = (size / 2f) - padding - arcWidth / 2f;
        numberRadius = radius - arcWidth / 2f - 32f;
        tickOuterRadius = numberRadius + 12f;
        tickInnerRadius = numberRadius - 6f;
        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(colorBackground);

        // 1. 刻度盘背景圆
        canvas.drawCircle(centerX, centerY, numberRadius + 20f, sleepBgPaint);

        // 2. 小时刻度和数字
        drawTicksAndNumbers(canvas);

        // 3. 橙色睡眠弧线
        drawSleepArc(canvas);

        // 4. 圆点
        drawDot(canvas, bedtimeHour, bedtimeMinute, "🌙");
        drawDot(canvas, wakeHour, wakeMinute, "🔔");

        // 5. 中心睡眠时长
        drawCenterText(canvas);
    }

    private void drawTicksAndNumbers(Canvas canvas) {
        for (int h = 1; h <= 12; h++) {
            float angle = (h % 12) * 30f; // 12点=0度，顺时针
            float rad = (float) Math.toRadians(angle - 90);

            // 刻度线
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float x1 = centerX + tickInnerRadius * cos;
            float y1 = centerY + tickInnerRadius * sin;
            float x2 = centerX + tickOuterRadius * cos;
            float y2 = centerY + tickOuterRadius * sin;
            canvas.drawLine(x1, y1, x2, y2, tickPaint);

            // 数字
            float nx = centerX + numberRadius * cos;
            float ny = centerY + numberRadius * sin;
            String label = String.valueOf(h);
            canvas.drawText(label, nx, ny + 9, numberPaint);
        }
    }

    private void drawSleepArc(Canvas canvas) {
        float startAngle = timeToAngle(bedtimeHour, bedtimeMinute);
        int sleepMinutes = getSleepDurationMinutes();
        float sweepAngle = sleepMinutes / 2.0f; // 12小时=720分钟=360度

        canvas.drawArc(arcRect, startAngle - 90, sweepAngle, false, arcPaint);
    }

    private void drawDot(Canvas canvas, int hour, int minute, String icon) {
        float angle = timeToAngle(hour, minute);
        float rad = (float) Math.toRadians(angle - 90);
        float x = centerX + radius * (float) Math.cos(rad);
        float y = centerY + radius * (float) Math.sin(rad);

        // 黑色填充
        canvas.drawCircle(x, y, dotRadius, dotFillPaint);
        // 橙色描边
        canvas.drawCircle(x, y, dotRadius, dotBorderPaint);
        // 图标
        canvas.drawText(icon, x, y + 10, iconPaint);
    }

    private void drawCenterText(Canvas canvas) {
        int sleepMinutes = getSleepDurationMinutes();
        int hours = sleepMinutes / 60;
        int minutes = sleepMinutes % 60;

        String hourText = hours + "";
        String unitText = (minutes > 0) ? "小时 " + minutes + " 分" : "小时";

        // 上下排列：先画数字，再画单位
        float cy = centerY - 30;
        canvas.drawText(hourText, centerX, cy, centerHourPaint);
        canvas.drawText(unitText, centerX, cy + 110, centerUnitPaint);
    }

    // ===== 时间/角度转换 =====

    /**
     * 24小时制时间 -> 12小时制表盘角度（0=12点，顺时针）
     */
    private float timeToAngle(int hour, int minute) {
        int h12 = hour % 12;
        return (h12 * 30f + minute * 0.5f) % 360f;
    }

    /**
     * 表盘角度 + 当前小时 -> 24小时制时间
     */
    private int[] angleToTime24(float angle, int currentHour) {
        float normalized = normalizeAngle(angle);
        float totalMinutes12 = normalized / 360f * 720f;
        int h12 = ((int) (totalMinutes12 / 60f)) % 12;
        int minute = ((int) totalMinutes12) % 60;

        int h1 = h12;
        int h2 = (h12 + 12) % 24;

        int diff1 = Math.min(Math.abs(h1 - currentHour), 24 - Math.abs(h1 - currentHour));
        int diff2 = Math.min(Math.abs(h2 - currentHour), 24 - Math.abs(h2 - currentHour));

        int hour = diff1 <= diff2 ? h1 : h2;
        return new int[]{hour, minute};
    }

    private float normalizeAngle(float angle) {
        return ((angle % 360f) + 360f) % 360f;
    }

    private int getSleepDurationMinutes() {
        int bed = bedtimeHour * 60 + bedtimeMinute;
        int wake = wakeHour * 60 + wakeMinute;
        if (wake >= bed) return wake - bed;
        return (24 * 60 - bed) + wake;
    }

    // ===== 触摸处理 =====

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float touchAngle = pointToAngle(x, y);
        float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
        float touchSlop = 50f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float[] bp = getDotPosition(bedtimeHour, bedtimeMinute);
                if (distToPoint(x, y, bp[0], bp[1]) < dotRadius + touchSlop) {
                    activeDrag = 1;
                    dragStartAngle = touchAngle;
                    return true;
                }
                float[] wp = getDotPosition(wakeHour, wakeMinute);
                if (distToPoint(x, y, wp[0], wp[1]) < dotRadius + touchSlop) {
                    activeDrag = 2;
                    dragStartAngle = touchAngle;
                    return true;
                }
                if (Math.abs(dist - radius) < arcWidth / 2 + touchSlop) {
                    if (isAngleInSleepArc(touchAngle)) {
                        activeDrag = 3;
                        dragStartAngle = touchAngle;
                        dragStartBedtimeTotal = bedtimeHour * 60 + bedtimeMinute;
                        dragStartWakeTotal = wakeHour * 60 + wakeMinute;
                        dragStartSleepMinutes = getSleepDurationMinutes();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (activeDrag == 0) break;

                if (activeDrag == 1) {
                    // 拖拽就寝点：保持睡眠时长不变
                    int[] bt = angleToTime24(touchAngle, bedtimeHour);
                    bedtimeHour = bt[0];
                    bedtimeMinute = bt[1];
                    // 保持睡眠时长，重新计算起床时间
                    int wakeTotal = bedtimeHour * 60 + bedtimeMinute + getSleepDurationMinutes();
                    wakeTotal = wakeTotal % (24 * 60);
                    wakeHour = wakeTotal / 60;
                    wakeMinute = wakeTotal % 60;
                } else if (activeDrag == 2) {
                    // 拖拽起床点：改变睡眠时长
                    int[] wt = angleToTime24(touchAngle, wakeHour);
                    wakeHour = wt[0];
                    wakeMinute = wt[1];
                } else if (activeDrag == 3) {
                    // 整体拖拽：保持睡眠时长不变
                    float angleDelta = touchAngle - dragStartAngle;
                    int minuteDelta = (int) (angleDelta / 360f * 720f); // 12小时制角度转分钟
                    int newBedTotal = (dragStartBedtimeTotal + minuteDelta) % (24 * 60);
                    if (newBedTotal < 0) newBedTotal += 24 * 60;
                    bedtimeHour = newBedTotal / 60;
                    bedtimeMinute = newBedTotal % 60;
                    int newWakeTotal = (newBedTotal + dragStartSleepMinutes) % (24 * 60);
                    wakeHour = newWakeTotal / 60;
                    wakeMinute = newWakeTotal % 60;
                    dragStartAngle = touchAngle;
                    dragStartBedtimeTotal = bedtimeHour * 60 + bedtimeMinute;
                }

                // 防止就寝和起床重叠（至少15分钟）
                if (getSleepDurationMinutes() < 15) {
                    int bedTotal = bedtimeHour * 60 + bedtimeMinute;
                    int wakeTotal = (bedTotal + 15) % (24 * 60);
                    wakeHour = wakeTotal / 60;
                    wakeMinute = wakeTotal % 60;
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

    private float[] getDotPosition(int hour, int minute) {
        float angle = timeToAngle(hour, minute);
        float rad = (float) Math.toRadians(angle - 90);
        return new float[]{
                centerX + radius * (float) Math.cos(rad),
                centerY + radius * (float) Math.sin(rad)
        };
    }

    private float pointToAngle(float x, float y) {
        float dx = x - centerX;
        float dy = y - centerY;
        float rad = (float) Math.atan2(dy, dx);
        float deg = (float) Math.toDegrees(rad) + 90;
        if (deg < 0) deg += 360;
        return deg;
    }

    private boolean isAngleInSleepArc(float angle) {
        float start = timeToAngle(bedtimeHour, bedtimeMinute);
        float sweep = getSleepDurationMinutes() / 2.0f;
        float end = start + sweep;
        float a = angle;
        if (a < start) a += 360;
        return a >= start && a <= end;
    }

    private float distToPoint(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onTimesChanged(bedtimeHour, bedtimeMinute, wakeHour, wakeMinute);
        }
    }

    // ===== 公开方法 =====

    public void setOnTimeChangedListener(OnTimeChangedListener l) {
        this.listener = l;
    }

    public void setTimes(int bedtimeHour, int bedtimeMinute, int wakeHour, int wakeMinute) {
        this.bedtimeHour = bedtimeHour;
        this.bedtimeMinute = bedtimeMinute;
        this.wakeHour = wakeHour;
        this.wakeMinute = wakeMinute;
        invalidate();
    }

    public int getBedtimeHour() { return bedtimeHour; }
    public int getBedtimeMinute() { return bedtimeMinute; }
    public int getWakeHour() { return wakeHour; }
    public int getWakeMinute() { return wakeMinute; }
    public int getSleepDuration() { return getSleepDurationMinutes(); }
}
