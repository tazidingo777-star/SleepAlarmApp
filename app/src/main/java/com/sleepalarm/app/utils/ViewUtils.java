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

    public static void setNumberPickerTextColor(NumberPicker picker, int color) {
        Field[] fields = NumberPicker.class.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals("mInputText") || field.getName().equals("mSelectorWheelPaint")) {
                field.setAccessible(true);
                try {
                    Object obj = field.get(picker);
                    if (obj instanceof android.widget.EditText) {
                        ((android.widget.EditText) obj).setTextColor(color);
                    } else if (obj instanceof android.graphics.Paint) {
                        ((android.graphics.Paint) obj).setColor(color);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
}
