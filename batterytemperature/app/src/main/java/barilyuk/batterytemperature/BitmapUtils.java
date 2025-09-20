package barilyuk.batterytemperature;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;


public class BitmapUtils {

    public static Bitmap textToBitmap(String text, int textColor) {
        //int textColor = Color.BLACK;
        int textSize = 120;
        Paint numberPaint = new Paint();
        numberPaint.setTextSize(textSize);
        numberPaint.setColor(textColor);
        // Use setARGB to explicitly define the color
        //numberPaint.setARGB(255, 0, 0, 0); // Equivalent to Color.BLACK
        numberPaint.setAntiAlias(true);
        numberPaint.setTextAlign(Paint.Align.LEFT); // Align text center
        //numberPaint.setTypeface(Typeface.DEFAULT_BOLD);
        // Use a more compact font: MONOSPACE is generally more compact for numbers
        //numberPaint.setTypeface(Typeface.MONOSPACE);
        // Set the typeface to bold monospace
        numberPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        //numberPaint.setLetterSpacing(-0.01f); // Adjust letter spacing for tighter character fit
        //paint.setSubpixelText(true);

        Paint dotPaint = new Paint(numberPaint); // Copy number paint properties
        dotPaint.setTextSize(textSize * 0.6f); // Make dot smaller (e.g., 60% of text size)

        // Split the text into parts: before, dot, after
        //String[] parts = text.split("\\°");
        String integerPart = text;
        /*
        String fractionalPart = "";
        if (parts.length > 1) {
            fractionalPart = parts[1];
        }
         */

        // Create a 190x190 pixel bitmap
        int width = 190;
        int height = 190;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        //canvas.drawColor(Color.argb(200, 255, 255, 255)); //was canvas.drawColor(Color.TRANSPARENT);

        if (textColor == Color.BLACK) {
            canvas.drawColor(Color.WHITE);
        } else {
            canvas.drawColor(Color.TRANSPARENT);
        }

        // Stretch the canvas vertically
        canvas.save(); // Save the current canvas state
        canvas.scale(1.0f, 1.8f, width / 2f, height / 2f); // Stretch height by 80%

        // Calculate the initial x position for centering
        float numberWidth = numberPaint.measureText(integerPart);
        float dotWidth = dotPaint.measureText("°");
        float x = (width - (numberWidth + dotWidth)) / 2;

        // Calculate y position
        Paint.FontMetrics metrics = numberPaint.getFontMetrics();
        float y = (height - metrics.ascent - metrics.descent) / 2;

        // Draw the integer part
        canvas.drawText(integerPart, x, y, numberPaint);
        x += numberPaint.measureText(integerPart); // Update x for dot position

        // Draw the dot with smaller size
        // draw degree symbol shifted upwards
        x = x - dotWidth/4;
        float degreeYOffset = dotPaint.getFontMetrics().ascent * 0.5f;
        canvas.drawText("°", x, y + degreeYOffset, dotPaint);

        // Draw the fractional part
        //canvas.drawText(fractionalPart, x, y, numberPaint);


        canvas.restore(); // Restore the original canvas scale

        return bitmap;
    }
}