package com.universal.deviceinfo;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Light-theme UI toolkit (white background, near-black text, Roboto — the Android
 * system font, matching the download page). Everything is built in code so it
 * renders the same from Android 4.0 up, with no AndroidX. Interactive elements
 * are focusable so the app is still usable with a TV remote (D-pad).
 */
public final class UiBuilder {

    public static final int BG        = 0xFFFFFFFF;
    public static final int TEXT      = 0xFF202124;
    public static final int MUTED     = 0xFF5F6368;
    public static final int ACCENT    = 0xFF01875F;
    public static final int ACCENT_DK = 0xFF016B4B;
    public static final int LINE      = 0xFFDADCE0;
    public static final int CHIP      = 0xFFF1F3F4;
    public static final int FOCUS_BG  = 0xFFE6F4EE;

    private UiBuilder() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static TextView title(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(TEXT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    public static TextView subtitle(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(MUTED);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        return t;
    }

    public static TextView status(Context c) {
        TextView t = new TextView(c);
        t.setTextColor(ACCENT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setPadding(0, dp(c, 8), 0, 0);
        return t;
    }

    /** Accent-filled or outlined button (both focusable for D-pad). */
    public static Button button(Context c, String text, boolean filled) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setPadding(dp(c, 18), dp(c, 9), dp(c, 18), dp(c, 9));
        b.setMinHeight(0);
        b.setMinimumHeight(0);

        StateListDrawable sld = new StateListDrawable();
        if (filled) {
            b.setTextColor(0xFFFFFFFF);
            sld.addState(new int[]{android.R.attr.state_pressed}, solid(ACCENT_DK, dp(c, 8)));
            sld.addState(new int[]{android.R.attr.state_focused}, ring(ACCENT_DK, ACCENT, dp(c, 8), dp(c, 2)));
            sld.addState(new int[]{}, solid(ACCENT, dp(c, 8)));
        } else {
            b.setTextColor(ACCENT);
            sld.addState(new int[]{android.R.attr.state_focused}, ring(FOCUS_BG, ACCENT, dp(c, 8), dp(c, 2)));
            sld.addState(new int[]{}, ring(BG, LINE, dp(c, 8), dp(c, 1)));
        }
        setBg(b, sld);
        b.setFocusable(true);
        return b;
    }

    /** Rounded search input. */
    public static EditText searchField(Context c) {
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT);
        e.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        e.setHint("Buscar: cpu, ram, gpu, wifi, sd…");
        e.setHintTextColor(MUTED);
        e.setTextColor(TEXT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        e.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, ring(BG, ACCENT, dp(c, 24), dp(c, 2)));
        sld.addState(new int[]{}, ring(BG, LINE, dp(c, 24), dp(c, 1)));
        setBg(e, sld);
        return e;
    }

    /** A category chip (pill). Use {@link #styleChip} to set selected state. */
    public static TextView chip(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        styleChip(t, false);
        return t;
    }

    public static void styleChip(TextView chip, boolean selected) {
        Context c = chip.getContext();
        int r = dp(c, 20);
        if (selected) {
            chip.setTextColor(0xFFFFFFFF);
            setBg(chip, ringFocus(ACCENT, ACCENT_DK, r, dp(c, 2)));
        } else {
            chip.setTextColor(MUTED);
            setBg(chip, ringFocus(CHIP, LINE, r, dp(c, 1)));
        }
    }

    /** One focusable info card built from a section. */
    public static View sectionCard(Context c, InfoSection section) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(c, 16);
        card.setPadding(pad, dp(c, 13), pad, dp(c, 14));
        setBg(card, cardBackground(c));
        card.setFocusable(true);
        card.setFocusableInTouchMode(false);
        addFocusPop(card);

        LinearLayout titleRow = new LinearLayout(c);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(c);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(c, 4), dp(c, 20));
        barLp.rightMargin = dp(c, 10);
        bar.setLayoutParams(barLp);
        setBg(bar, solid(ACCENT, dp(c, 2)));
        titleRow.addView(bar);
        TextView tt = new TextView(c);
        tt.setText(section.getTitle());
        tt.setTextColor(ACCENT);
        tt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tt.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleRow.addView(tt);
        card.addView(titleRow);

        View sep = new View(c);
        LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(c, 1)));
        sepLp.topMargin = dp(c, 9);
        sepLp.bottomMargin = dp(c, 5);
        sep.setLayoutParams(sepLp);
        sep.setBackgroundColor(0xFFEEEFF1);
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
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        label.setAllCaps(true);
        row.addView(label);

        TextView value = new TextView(c);
        value.setText(it.getValue());
        value.setTextColor(TEXT);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
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

    private static GradientDrawable ring(int fill, int stroke, int radius, int strokeW) {
        GradientDrawable g = solid(fill, radius);
        g.setStroke(strokeW, stroke);
        return g;
    }

    /** Chip drawable that reacts to focus with an accent ring. */
    private static StateListDrawable ringFocus(int fill, int stroke, int radius, int strokeW) {
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused},
                ring(fill, ACCENT, radius, Math.max(strokeW, dp2(radius))));
        sld.addState(new int[]{}, ring(fill, stroke, radius, strokeW));
        return sld;
    }

    // small helper: 2px-ish focus stroke independent of radius
    private static int dp2(int radius) {
        return Math.max(2, radius / 10);
    }

    private static StateListDrawable cardBackground(Context c) {
        GradientDrawable normal = ring(0xFFFFFFFF, LINE, dp(c, 14), Math.max(1, dp(c, 1)));
        GradientDrawable focused = ring(FOCUS_BG, ACCENT, dp(c, 14), Math.max(2, dp(c, 2)));
        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_focused}, focused);
        sld.addState(new int[]{android.R.attr.state_selected}, focused);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    private static void addFocusPop(final View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (Build.VERSION.SDK_INT >= 21) {
                    view.setElevation(hasFocus ? dp(view.getContext(), 4) : 0);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static void setBg(View v, android.graphics.drawable.Drawable d) {
        v.setBackground(d);
    }

    public static LinearLayout.LayoutParams cardLp(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 12);
        return lp;
    }
}
