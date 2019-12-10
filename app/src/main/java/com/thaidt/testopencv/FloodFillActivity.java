package com.thaidt.testopencv;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.thaidt.testopencv.utils.QueueLinearFloodFiller;

import java.util.LinkedList;
import java.util.Queue;

public class FloodFillActivity extends AppCompatActivity implements View.OnTouchListener {
    private RelativeLayout drawingLayout;
    private MyView myView;
    Button red, blue, yellow;
    Paint paint;
    final Point mainPoint = new Point();
    public ImageView mainImageView;
    Bitmap mainBitmap;

    /**
     * Called when the activity is first created.
     */
    /*
     *
     * private ImageView imageView; private Canvas cv; private Bitmap mask,
     * original, colored; private int r,g,b; private int sG, sR, sB;
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flood_fill);

        mainImageView = findViewById(R.id.coringImage);
        myView = new MyView(this);
        drawingLayout = (RelativeLayout) findViewById(R.id.relative_layout);
        drawingLayout.addView(myView);

        red = (Button) findViewById(R.id.btn_red);
        blue = (Button) findViewById(R.id.btn_blue);
        yellow = (Button) findViewById(R.id.btn_yellow);

        red.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                paint.setColor(Color.RED);
            }
        });

        yellow.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                paint.setColor(Color.YELLOW);
            }
        });
        blue.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                paint.setColor(Color.BLUE);
            }
        });

        mainImageView.setOnTouchListener(this);

        //Init default
//        mainBitmap = BitmapFactory.decodeResource(getResources(),
//                R.drawable.room3).copy(Bitmap.Config.ARGB_8888, true);
//        mainImageView.setImageBitmap(mainBitmap);
//        mPaint = new Paint();
//        mPaint.setAntiAlias(true);
//        mPaint.setStyle(Paint.Style.STROKE);
//        mPaint.setStrokeJoin(Paint.Join.ROUND);
//        mPaint.setStrokeWidth(5f);
//        mPaint.setColor(Color.GREEN);
    }

    public class MyView extends View {

        private Path path;
        Bitmap mBitmap;
        ProgressDialog pd;
        final Point p1 = new Point();
        Canvas canvas;

        // Bitmap mutableBitmap ;
        public MyView(Context context) {
            super(context);

            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int screenWidth = metrics.widthPixels;
            int screenHeight = metrics.heightPixels;

            paint = new Paint();
            paint.setAntiAlias(true);
            pd = new ProgressDialog(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(5f);
            mBitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.room5).copy(Bitmap.Config.ARGB_8888, true);
            mBitmap = Bitmap.createScaledBitmap(mBitmap, screenHeight, screenWidth, true);
            this.path = new Path();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            this.canvas = canvas;
            paint.setColor(Color.GREEN);

            canvas.drawBitmap(mBitmap, 0, 0, paint);

        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                p1.x = (int) x;
                p1.y = (int) y;
                final int sourceColor = mBitmap.getPixel((int) x, (int) y);
                final int targetColor = paint.getColor();
                new TheTask(mBitmap, p1, sourceColor, targetColor).execute();
                invalidate();
            }
            return true;
        }

        public void clear() {
            path.reset();
            invalidate();
        }

        public int getCurrentPaintColor() {
            return paint.getColor();
        }

        class TheTask extends AsyncTask<Void, Integer, Bitmap> {

            Bitmap bmp;
            Point pt;
            int replacementColor, targetColor;

            public TheTask(Bitmap bm, Point p, int sc, int tc) {
                this.bmp = bm;
                this.pt = p;
                this.replacementColor = tc;
                this.targetColor = sc;
                pd.setMessage("Filling....");
                pd.show();
            }

            @Override
            protected void onPreExecute() {
                pd.show();

            }

            @Override
            protected void onProgressUpdate(Integer... values) {

            }

            @Override
            protected Bitmap doInBackground(Void... params) {
                FloodFill f = new FloodFill();
                f.newFloodFill(bmp, pt, targetColor, replacementColor);
//                QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(bmp, targetColor, replacementColor);
//                queueLinearFloodFiller.setTolerance(20);
//                queueLinearFloodFiller.floodFill(pt.x, pt.y);
                return null;
//                return f.newFloodFill(bmp, pt, oldColor, newColor);
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    canvas.drawBitmap(result, 0, 0, paint);
                }
                pd.dismiss();
                invalidate();
            }
        }
    }

// flood fill

    public class FloodFill {
        int ANTILAISING_TOLERANCE = 10;

        void floodFill(Bitmap image, Point node, int targetColor,
                       int replacementColor) {
            int width = image.getWidth();
            int height = image.getHeight();
            int target = targetColor;
            int replacement = replacementColor;
            if (target != replacement) {
                Queue<Point> queue = new LinkedList<Point>();
                do {

                    int x = node.x;
                    int y = node.y;
                    while (x > 0 && image.getPixel(x - 1, y) == target) {
                        x--;

                    }
                    boolean spanUp = false;
                    boolean spanDown = false;
                    while (x < width && image.getPixel(x, y) == target) {
                        image.setPixel(x, y, replacement);
                        if (!spanUp && y > 0
                                && image.getPixel(x, y - 1) == target) {
                            queue.add(new Point(x, y - 1));
                            spanUp = true;
                        } else if (spanUp && y > 0
                                && image.getPixel(x, y - 1) != target) {
                            spanUp = false;
                        }
                        if (!spanDown && y < height - 1
                                && image.getPixel(x, y + 1) == target) {
                            queue.add(new Point(x, y + 1));
                            spanDown = true;
                        } else if (spanDown && y < height - 1
                                && image.getPixel(x, y + 1) != target) {
                            spanDown = false;
                        }
                        x++;
                    }
                } while ((node = queue.poll()) != null);
            }
        }

        Bitmap newFloodFill(Bitmap image, Point node, int targetColor,
                            int replacementColor) {
            Bitmap colored = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(colored);
            cv.drawBitmap(image, 0, 0, null);

            int sG = (targetColor & 0x0000FF00) >> 8;
            int sR = (targetColor & 0x00FF0000) >> 16;
            int sB = (targetColor & 0x000000FF);

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int g = (image.getPixel(x, y) & 0x0000FF00) >> 8;
                    int r = (image.getPixel(x, y) & 0x00FF0000) >> 16;
                    int b = (image.getPixel(x, y) & 0x000000FF);
                    if (Math.abs(sR - r) < ANTILAISING_TOLERANCE
                            && Math.abs(sG - g) < ANTILAISING_TOLERANCE
                            && Math.abs(sB - b) < ANTILAISING_TOLERANCE)
                        colored.setPixel(x, y, replacementColor);
                }
            }
            Canvas cv2 = new Canvas(image);
            cv2.drawBitmap(colored, 0, 0, null);
            return colored;
        }

        boolean compareColor(int selectedColor, int pixelColor) {
            int sG = (selectedColor & 0x0000FF00) >> 8;
            int sR = (selectedColor & 0x00FF0000) >> 16;
            int sB = (selectedColor & 0x000000FF);

            int g = (pixelColor & 0x0000FF00) >> 8;
            int r = (pixelColor & 0x00FF0000) >> 16;
            int b = (pixelColor & 0x000000FF);
            return Math.abs(sR - r) < ANTILAISING_TOLERANCE
                    && Math.abs(sG - g) < ANTILAISING_TOLERANCE
                    && Math.abs(sB - b) < ANTILAISING_TOLERANCE;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mainPoint.x = (int) x;
            mainPoint.y = (int) y;
            final int sourceColor = mainBitmap.getPixel((int) x, (int) y);
            final int targetColor = paint.getColor();
            new MainTask(mainBitmap, mainPoint, sourceColor, targetColor).execute();
            mainImageView.setImageBitmap(mainBitmap);
        }
        return false;
    }

    class MainTask extends AsyncTask<Void, Integer, Bitmap> {

        Bitmap bmp;
        Point pt;
        int replacementColor, targetColor;

        public MainTask(Bitmap bm, Point p, int sc, int tc) {
            this.bmp = bm;
            this.pt = p;
            this.replacementColor = tc;
            this.targetColor = sc;
//            pd.setMessage("Filling....");
//            pd.show();
        }

        @Override
        protected void onPreExecute() {
//            pd.show();

        }

        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            FloodFill f = new FloodFill();
            f.newFloodFill(bmp, pt, targetColor, replacementColor);
//            return null;
//            QueueLinearFloodFiller queueLinearFloodFiller = new QueueLinearFloodFiller(bmp, targetColor, replacementColor);
//            queueLinearFloodFiller.setTolerance(20);
//            queueLinearFloodFiller.floodFill(pt.x, pt.y);
            return bmp;
//                return f.newFloodFill(bmp, pt, oldColor, newColor);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
//            if (result != null) {
//                canvas.drawBitmap(result, 0, 0, mPaint);
//            }
//            pd.dismiss();
//            invalidate();
        }
    }
}
