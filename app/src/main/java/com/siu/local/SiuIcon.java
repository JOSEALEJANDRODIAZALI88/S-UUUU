package com.siu.local;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;

/** The same Font Awesome artwork bundled with the reference application. */
final class SiuIcon extends View {
    private static Typeface font;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String glyph;
    private final Rect bounds = new Rect();
    private final boolean back;
    private final Path backPath = new Path();

    SiuIcon(Context context, String name, int color) {
        super(context);
        if (font == null) font = Typeface.createFromAsset(context.getAssets(), "fonts/FontAwesomeSolid.otf");
        paint.setTypeface(font);
        paint.setColor(color);
        glyph = String.valueOf((char) codepoint(name));
        back = name.equals("back");
        if (back) {
            backPath.moveTo(20,12);backPath.lineTo(4,12);
            backPath.moveTo(11,5);backPath.lineTo(4,12);backPath.lineTo(11,19);
        }
        setContentDescription(name);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    static int codepoint(String name) {
        return switch (name) {
            case "back" -> 0xf060;
            case "menu" -> 0xf0c9;
            case "list" -> 0xf0ca;
            case "grid" -> 0xf00a;
            case "qr" -> 0xf029;
            case "refresh" -> 0xf2f9;
            case "Social" -> 0xf500;
            case "Materias", "Biblioteca" -> 0xf02d;
            case "Académica" -> 0xf19d;
            case "Recursos" -> 0xf0b1;
            case "Tip's" -> 0xf0eb;
            case "home" -> 0xf015;
            case "user" -> 0xf007;
            case "contacts" -> 0xf2b9;
            case "id" -> 0xf2c2;
            case "mail" -> 0xf0e0;
            case "location" -> 0xf3c5;
            case "phone" -> 0xf879;
            case "edit", "Consultas" -> 0xf044;
            case "suggestion" -> 0xf31c;
            case "pencil" -> 0xf304;
            case "clock" -> 0xf017;
            case "folder" -> 0xf07c;
            case "search" -> 0xf002;
            case "arrow" -> 0xf061;
            case "chevron" -> 0xf054;
            case "logout" -> 0xf2f5;
            case "settings" -> 0xf013;
            case "more" -> 0xf142;
            case "info" -> 0xf129;
            case "warning" -> 0xf06a;
            case "money" -> 0xf51e;
            case "discount" -> 0xf4c0;
            case "projects" -> 0xf0ae;
            case "payments" -> 0xf022;
            case "sun" -> 0xf185;
            case "moon" -> 0xf186;
            case "laptop" -> 0xf109;
            case "map" -> 0xf5a0;
            case "file" -> 0xf15b;
            default -> 0xf15c;
        };
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (back) {
            // Android's native app-bar arrow, rather than the heavier menu glyph.
            canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
            canvas.drawPath(backPath,paint);canvas.restore();return;
        }
        paint.setTextSize(100);
        paint.getTextBounds(glyph, 0, glyph.length(), bounds);
        if (bounds.width() == 0 || bounds.height() == 0) return;
        float scale = Math.min((float) getWidth() / bounds.width(), (float) getHeight() / bounds.height());
        paint.setTextSize(100 * scale);
        paint.getTextBounds(glyph, 0, glyph.length(), bounds);
        canvas.drawText(glyph, (getWidth() - bounds.width()) / 2f - bounds.left,
                (getHeight() - bounds.height()) / 2f - bounds.top, paint);
    }
}
