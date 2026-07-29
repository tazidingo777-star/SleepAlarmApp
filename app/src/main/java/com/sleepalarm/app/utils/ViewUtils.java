package com.sleepalarm.app.utils;

import android.widget.NumberPicker;

import java.lang.reflect.Field;

public class ViewUtils {

    public static void setNumberPickerDividerColor(NumberPicker picker, int color) {
        Field[] fields = NumberPicker.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("mSelectionDivider")) {
                field.setAccessible(true);
                try {
                    field.set(picker, new android.graphics.drawable.ColorDrawable(color));
                } catch (Exception ignored) {
                }
                break;
            }
        }
    }
}
