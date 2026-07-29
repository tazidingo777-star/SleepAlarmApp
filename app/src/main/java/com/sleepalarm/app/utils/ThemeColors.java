package com.sleepalarm.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

/**
 * 根据当前主题（暗色/亮色）动态获取颜色值
 */
public class ThemeColors {

    public static int getBackground(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appBackground);
    }

    public static int getSurface(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appSurface);
    }

    public static int getSurfaceLight(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appSurfaceLight);
    }

    public static int getAccent(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appAccent);
    }

    public static int getTextPrimary(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appTextPrimary);
    }

    public static int getTextSecondary(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appTextSecondary);
    }

    public static int getDivider(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appDivider);
    }

    public static int getCardBg(Context ctx) {
        return getColor(ctx, com.sleepalarm.app.R.attr.appCardBg);
    }

    /**
     * 文本颜色用于铺在 accent 背景上：暗色主题返回黑色（橙色底+黑字），亮色主题返回白色（绿色底+白字）
     */
    public static int getOnAccent(Context ctx) {
        return new PreferencesHelper(ctx).isDarkTheme() ? Color.BLACK : Color.WHITE;
    }

    private static int getColor(Context ctx, int attr) {
        TypedValue tv = new TypedValue();
        ctx.getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }
}
