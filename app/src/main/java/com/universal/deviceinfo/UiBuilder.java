package com.universal.deviceinfo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Builds the 10-foot ("TV") interface entirely in code so it renders identically
 * from Android 4.1 to the latest release without any AndroidX/support library.
 * Cards are focusable for D-pad navigation and highlight when focused.
 */
public final class UiBuilder {

    // ---- Palette: deep navy with a teal accent (high contrast for TV) ----
    public static final int BG        = 0xFF0B1020;
    public static final int CARD      = 0xFF16203A;
    public static final int CARD_FOCUS= 0xFF213158;
    public static final int STROKE    = 0xFF2A3B63;
    public static final int ACCENT    = 0xFF34E0C4;
    public static final int ACCENT_2  = 0xFF6EA8FE;
    public static final int TEXT      = 0xFFF2F6FF;
    public static final int MUTED     = 0xFF93A4C6;
    public static final int GOOD      = 0xFF5BD98A;
    public static final int WARN      = 0xFFFFC24B;

    private UiBuilder() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    private static int sp(Context c, float v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, v, c.getResources().getDisplayMetrics());
    }

    public static TextView title(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(TEXT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static TextView subtitle(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(MUTED);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return t;
    }

    public static TextView status(Context c) {
        TextView t = new TextView(c);
        t.setTextColor(ACCENT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setTypeface(Typeface.MONOSPACE);
        t.setPadding(0, dp(c, 8), 0, 0);
        return t;
    }

    public static Button button(Context c, String text) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(BG);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10));

        GradientDrawable normal = solid(ACCENT, dp(c, 10));
        GradientDrawable focused = solid(ACCENT_2, dp(c, 10));
        GradientDrawable pressed = solid(0xFFFFFFFF, dp(c, 10));
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressed);
        sld.addState(new int[]{android.R.attr.state_focused}, focused);
        sld.addState(new int[]{}, normal);
        setBg(b, sld);
        b.setFocusable(true);
        addFocusPop(b);
        return b;
    }

    /** Builds one focusable info card from a section. */
    public static View sectionCard(Context c, InfoSection section) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 16);
        card.setPadding(pad, dp(c, 12), pad, dp(c, 14));
        setBg(card, cardBackground(c));
        card.setFocusable(true);
        card.setFocusableInTouchMode(false);
        addFocusPop(card);

        // Accent title row: a small bar + the section title.
        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        View bar = new View(c);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(c, 5), dp(c, 22));
        barLp.rightMargin = dp(c, 10);
        bar.setLayoutParams(barLp);
        setBg(bar, solid(ACCENT, dp(c, 3)));
        titleRow.addView(bar);

        TextView tt = new TextView(c);
        tt.setText(section.getTitle());
        tt.setTextColor(ACCENT);
        tt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tt.setTypeface(Typeface.DEFAULT_BOLD);
        titleRow.addView(tt);
        card.addView(titleRow);

        View sep = new View(c);
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1)));
        sepLp.topMargin = dp(c, 8);
        sepLp.bottomMargin = dp(c, 6);
        sep.setLayoutParams(sepLp);
        sep.setBackgroundColor(STROKE);
        card.addView(sep);

        for (InfoItem it : section.getItems()) {
            card.addView(row(c, it));
        }
        return card;
    }

    private static View row(Context c, InfoItem it) {
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(c, 6), 0, dp(c, 6));

        TextView label = new TextView(c);
        label.setText(it.getLabel());
        label.setTextColor(MUTED);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        label.setAllCaps(true);
        row.addView(label);

        TextView value = new TextView(c);
        value.setText(it.getValue());
        value.setTextColor(TEXT);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        value.setTypeface(Typeface.MONOSPACE);
        value.setPadding(0, dp(c, 2), 0, 0);
        row.addView(value);
        return row;
    }

    // ---- drawable helpers ----

    private static GradientDrawable solid(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private static StateListDrawable cardBackground(Context c) {
        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setColor(CARD);
        normal.setCornerRadius(dp(c, 14));
        normal.setStroke(Math.max(1, dp(c, 1)), STROKE);

        GradientDrawable focused = new GradientDrawable();
        focused.setShape(GradientDrawable.RECTANGLE);
        focused.setColor(CARD_FOCUS);
        focused.setCornerRadius(dp(c, 14));
        focused.setStroke(Math.max(2, dp(c, 2)), ACCENT);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, focused);
        sld.addState(new int[]{android.R.attr.state_selected}, focused);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    /** Small scale + elevation bump when focused, the classic TV cue. */
    private static void addFocusPop(final View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                float s = hasFocus ? 1.03f : 1.0f;
                view.animate().scaleX(s).scaleY(s).setDuration(120).start();
                if (Build.VERSION.SDK_INT >= 21) {
                    view.setElevation(hasFocus ? dp(view.getContext(), 8) : 0);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void setBg(View v, android.graphics.drawable.Drawable d) {
        if (Build.VERSION.SDK_INT >= 16) {
            v.setBackground(d);
        } else {
            v.setBackgroundDrawable(d);
        }
    }

    /** LinearLayout.LayoutParams with bottom margin, MATCH_PARENT width. */
    public static LinearLayout.LayoutParams cardLp(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 14);
        return lp;
    }

    public static int ignore() {
        return Color.TRANSPARENT;
    }
}
