package barilyuk.batterytemperature;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;

public class RollingChartView extends View {

    private String tempScale = "°C";
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

        // Convert min/max for display
        float displayMinY = convertTemp(minY);
        float displayMaxY = convertTemp(maxY);

        float yLabelMaxWidth = textPaint.measureText(String.format("%.0f", displayMaxY));

        float chartLeft = getPaddingLeft() + yLabelMaxWidth + 15f;
        float chartRight = getWidth() - getPaddingRight() - 25f;
        float chartBottom = getHeight() - getPaddingBottom() - 30f;
        float chartTop = getPaddingTop() + textPaint.getTextSize();

        float dx = (chartRight - chartLeft) / (maxPoints - 1);

        // Draw axes
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint);
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

        // Y axis ticks (display converted values)
        int yTicks = 3;
        for (int i = 0; i <= yTicks; i++) {
            float frac = i / (float) yTicks;
            float yVal = displayMaxY - frac * (displayMaxY - displayMinY);
            float y = chartTop + frac * (chartBottom - chartTop);
            canvas.drawLine(chartLeft - 5, y, chartLeft, y, axisPaint);
            String label = String.format("%.0f", yVal);
            canvas.drawText(label, chartLeft - 10f - textPaint.measureText(label), y + textPaint.getTextSize() / 3, textPaint);
        }

        // X axis ticks
        int xTicks = 3;
        for (int i = 0; i <= xTicks; i++) {
            float frac = i / (float) xTicks;
            float x = chartLeft + frac * (chartRight - chartLeft);
            int secondsAgo = -(maxPoints - 1) * updateInterval + (int)(frac * (maxPoints - 1) * updateInterval);
            canvas.drawLine(x, chartBottom, x, chartBottom + 5, axisPaint);
            float labelWidth = textPaint.measureText(secondsAgo + "s");
            float xOffset = (i == 0) ? labelWidth * 0.3f : 0f;
            canvas.drawText(secondsAgo + "s", x - labelWidth / 2 + xOffset, h - 5, textPaint);
        }

        // Draw temperature line (using original Celsius values for positioning)
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



    public void setTempScale(String scale) {
        this.tempScale = scale;
        invalidate();
    }

    private float convertTemp(float celsius) {
        if (tempScale.equals("°F") || tempScale.equals("F")) {
            return celsius * 9f / 5f + 32f;
        } else if (tempScale.equals("K")) {
            return celsius + 273.15f;
        }
        return celsius;
    }
}