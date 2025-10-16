package barilyuk.batterytemperature;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;

public class RollingChartView extends View {
    private final LinkedList<Float> values = new LinkedList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // horizontal span. maxPoints=update interval in seconds / horizontal span in seconds
    //x-axis 60 seconds, 2-second updates: maxPoints == 60 / 2 == 30
    private int maxPoints = 30;
    private int updateInterval = 2; // update interval in seconds
    private float minY = 20f, maxY = 40f; // fallback range

    public RollingChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setColor(0xFF00FF00); // green
        linePaint.setStrokeWidth(3f);
        linePaint.setStyle(Paint.Style.STROKE);

        axisPaint.setColor(0xFF888888);
        axisPaint.setStrokeWidth(1f);

        textPaint.setColor(0xFF888888);
        // scale text size according to screen density (DPI)
        float scaledSize = 20f * context.getResources().getDisplayMetrics().scaledDensity* 0.55f;
        textPaint.setTextSize(scaledSize);
    }

    public void addValue(float value) {
        if (values.size() >= maxPoints) {
            values.removeFirst();
        }
        values.add(value);

        // adjust vertical bounds around latest values
        minY = Float.MAX_VALUE;
        maxY = -Float.MAX_VALUE;
        for (float v : values) {
            if (v < minY) minY = v;
            if (v > maxY) maxY = v;
        }
        // pad so line doesn’t touch edges
        float padding = (maxY - minY) * 0.2f + 0.5f;
        minY -= padding;
        maxY += padding;

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.size() < 2) return;

        float w = getWidth();
        float h = getHeight();
        float yLabelMaxWidth = textPaint.measureText(String.format("%.1f", maxY)); // estimate widest Y label

        float chartLeft = getPaddingLeft() + yLabelMaxWidth + 15f;   // more space for Y labels
        float chartRight = getWidth() - getPaddingRight() - 25f;     // leave room for last X label
        float chartBottom = getHeight() - getPaddingBottom() - 30f;  // space for X labels
        float chartTop = getPaddingTop() + textPaint.getTextSize();  // space for top Y label

        float dx = (chartRight - chartLeft) / (maxPoints - 1);

        // --- draw axes ---
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint); // Y axis
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint); // X axis

        // --- Y axis ticks ---
        int yTicks = 3;
        for (int i = 0; i <= yTicks; i++) {
            float frac = i / (float) yTicks;
            float yVal = maxY - frac * (maxY - minY);
            float y = chartTop + frac * (chartBottom - chartTop);
            canvas.drawLine(chartLeft - 5, y, chartLeft, y, axisPaint);
            canvas.drawText(String.format("%.1f", yVal), chartLeft - 10f - textPaint.measureText(String.format("%.1f", yVal)), y + textPaint.getTextSize() / 3, textPaint);
        }

        // --- X axis ticks (time, seconds ago) ---
        int xTicks = 3;
        for (int i = 0; i <= xTicks; i++) {
            float frac = i / (float) xTicks;
            float x = chartLeft + frac * (chartRight - chartLeft);
            int secondsAgo = -(maxPoints - 1) * updateInterval + (int)(frac * (maxPoints - 1) * updateInterval);
            canvas.drawLine(x, chartBottom, x, chartBottom + 5, axisPaint);
            float labelWidth = textPaint.measureText(secondsAgo + "s");
            float xOffset = (i == 0) ? labelWidth * 0.3f : 0f; // nudge first label slightly right
            canvas.drawText(secondsAgo + "s", x - labelWidth / 2 + xOffset, h - 5, textPaint);
        }

        // --- draw temperature line ---
        path.reset();
        for (int i = 0; i < values.size(); i++) {
            float x = chartLeft + i * dx;
            float normY = (values.get(i) - minY) / (maxY - minY);
            float y = chartBottom - normY * (chartBottom - chartTop);
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        canvas.drawPath(path, linePaint);
    }
}