package com.heramello.mcpe.helloneighbor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {

    private Thread gameThread;
    private SurfaceHolder holder;
    private boolean isPlaying;
    private Paint paint;

    private Spaceship spaceship;
    private ArrayList<Meteor> meteors;
    private ArrayList<Star> backgroundStars;
    private Random random;
    private Vibrator vibrator;

    private long startTime;
    private long survivalTime;
    private int nearMisses;
    private float meteorSpeed = 7f;
    private int screenWidth, screenHeight;

    private float shakeOffsetX = 0;
    private float shakeOffsetY = 0;
    private int shakeFrames = 0;

    private GameActivity gameActivity;

    public GameView(Context context, Vibrator vibrator) {
        super(context);
        gameActivity = (GameActivity) context;
        this.vibrator = vibrator;
        holder = getHolder();
        paint = new Paint();
        paint.setAntiAlias(true);
        random = new Random();
        meteors = new ArrayList<>();
        backgroundStars = new ArrayList<>();

        startTime = System.currentTimeMillis();
        nearMisses = 0;
    }

    @Override
    public void run() {
        while (isPlaying) {
            update();
            draw();
            control();
        }
    }

    private void update() {
        if (spaceship == null) return;

        survivalTime = (System.currentTimeMillis() - startTime) / 1000;

        // Increase difficulty
        meteorSpeed = 7f + (survivalTime * 0.15f);

        // Spawn meteors more frequently
        if (random.nextInt(100) < 4 + (survivalTime / 10)) {
            int x = random.nextInt(screenWidth - 100);
            int size = 40 + random.nextInt(60);
            meteors.add(new Meteor(x, -100, size, meteorSpeed + random.nextFloat() * 3));
        }

        // Update meteors
        for (int i = meteors.size() - 1; i >= 0; i--) {
            Meteor meteor = meteors.get(i);
            meteor.update();

            if (meteor.y > screenHeight) {
                meteors.remove(i);
            } else if (checkCollision(spaceship, meteor)) {
                // Direct hit - game over
                isPlaying = false;
                gameActivity.gameOver(survivalTime, nearMisses);
            } else if (isNearMiss(spaceship, meteor)) {
                // Near miss - shake screen
                if (!meteor.countedAsNearMiss) {
                    nearMisses++;
                    meteor.countedAsNearMiss = true;
                    triggerShake();
                }
            }
        }

        // Update background stars
        for (Star star : backgroundStars) {
            star.y += 2;
            if (star.y > screenHeight) {
                star.y = 0;
                star.x = random.nextInt(screenWidth);
            }
        }

        // Update shake effect
        if (shakeFrames > 0) {
            shakeFrames--;
            shakeOffsetX = (random.nextFloat() - 0.5f) * 20;
            shakeOffsetY = (random.nextFloat() - 0.5f) * 20;
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }
    }

    private void draw() {
        if (holder.getSurface().isValid()) {
            Canvas canvas = holder.lockCanvas();

            // Apply shake offset
            canvas.save();
            canvas.translate(shakeOffsetX, shakeOffsetY);

            // Space background
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < screenHeight; i++) {
                float ratio = (float) i / screenHeight;
                int color = interpolateColor(0xFF000510, 0xFF1A0520, ratio);
                paint.setColor(color);
                canvas.drawRect(0, i, screenWidth, i + 1, paint);
            }

            // Draw background stars
            paint.setColor(Color.WHITE);
            for (Star star : backgroundStars) {
                canvas.drawCircle(star.x, star.y, star.size, paint);
            }

            // Draw meteors
            for (Meteor meteor : meteors) {
                drawMeteor(canvas, meteor);
            }

            // Draw spaceship
            if (spaceship != null) {
                drawSpaceship(canvas, spaceship);
            }

            // Draw HUD
            drawHUD(canvas);

            canvas.restore();
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void drawSpaceship(Canvas canvas, Spaceship ship) {
        paint.setStyle(Paint.Style.FILL);

        // Spaceship body (triangular)
        Path shipPath = new Path();
        shipPath.moveTo(ship.x + ship.width / 2, ship.y); // Top point
        shipPath.lineTo(ship.x + ship.width, ship.y + ship.height); // Bottom right
        shipPath.lineTo(ship.x, ship.y + ship.height); // Bottom left
        shipPath.close();

        paint.setColor(Color.rgb(100, 200, 255));
        canvas.drawPath(shipPath, paint);

        // Spaceship cockpit
        paint.setColor(Color.rgb(50, 150, 255));
        canvas.drawCircle(ship.x + ship.width / 2, ship.y + ship.height / 2, ship.width / 4, paint);

        // Engine glow
        paint.setColor(Color.rgb(255, 100, 50));
        canvas.drawCircle(ship.x + ship.width / 4, ship.y + ship.height - 5, 8, paint);
        canvas.drawCircle(ship.x + 3 * ship.width / 4, ship.y + ship.height - 5, 8, paint);

        // Outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.WHITE);
        canvas.drawPath(shipPath, paint);
    }

    private void drawMeteor(Canvas canvas, Meteor meteor) {
        paint.setStyle(Paint.Style.FILL);

        // Meteor body (irregular circle)
        paint.setColor(Color.rgb(139, 69, 19));
        canvas.drawCircle(meteor.x + meteor.size / 2, meteor.y + meteor.size / 2, meteor.size / 2, paint);

        // Craters
        paint.setColor(Color.rgb(100, 50, 20));
        canvas.drawCircle(meteor.x + meteor.size * 0.3f, meteor.y + meteor.size * 0.3f, meteor.size * 0.15f, paint);
        canvas.drawCircle(meteor.x + meteor.size * 0.7f, meteor.y + meteor.size * 0.6f, meteor.size * 0.1f, paint);

        // Fire trail
        paint.setColor(Color.argb(100, 255, 100, 0));
        for (int i = 0; i < 3; i++) {
            float trailY = meteor.y - (i + 1) * 15;
            float trailSize = meteor.size * (0.4f - i * 0.1f);
            canvas.drawCircle(meteor.x + meteor.size / 2, trailY, trailSize, paint);
        }

        // Danger glow for near misses
        if (isNearMiss(spaceship, meteor)) {
            paint.setColor(Color.argb(50, 255, 0, 0));
            canvas.drawCircle(meteor.x + meteor.size / 2, meteor.y + meteor.size / 2, meteor.size * 0.8f, paint);
        }
    }

    private void drawHUD(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);

        // Survival time
        paint.setTextSize(50);
        paint.setColor(Color.argb(150, 0, 255, 255));
        canvas.drawText("TIME: " + survivalTime + "s", 52, 72, paint);
        paint.setColor(Color.rgb(0, 255, 255));
        canvas.drawText("TIME: " + survivalTime + "s", 50, 70, paint);

        // Near misses
        paint.setTextSize(40);
        paint.setColor(Color.argb(150, 255, 165, 0));
        canvas.drawText("⚠ " + nearMisses, 52, 132, paint);
        paint.setColor(Color.rgb(255, 165, 0));
        canvas.drawText("⚠ " + nearMisses, 50, 130, paint);
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int a = (int) ((Color.alpha(color1) * (1 - ratio) + Color.alpha(color2) * ratio));
        int r = (int) ((Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio));
        int g = (int) ((Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio));
        int b = (int) ((Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio));
        return Color.argb(a, r, g, b);
    }

    private void triggerShake() {
        shakeFrames = 10;
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(100);
        }
    }

    private void control() {
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        isPlaying = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        isPlaying = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (spaceship != null) {
            float touchX = event.getX();
            spaceship.x = touchX - spaceship.width / 2;

            if (spaceship.x < 0) spaceship.x = 0;
            if (spaceship.x > screenWidth - spaceship.width) {
                spaceship.x = screenWidth - spaceship.width;
            }
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        spaceship = new Spaceship(w / 2 - 40, h - 200, 80, 100);

        // Initialize background stars
        for (int i = 0; i < 50; i++) {
            backgroundStars.add(new Star(
                    random.nextInt(w),
                    random.nextInt(h),
                    1 + random.nextFloat() * 2
            ));
        }
    }

    private boolean checkCollision(Spaceship s, Meteor m) {
        float dx = (s.x + s.width / 2) - (m.x + m.size / 2);
        float dy = (s.y + s.height / 2) - (m.y + m.size / 2);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < (s.width / 2 + m.size / 2) * 0.7f;
    }

    private boolean isNearMiss(Spaceship s, Meteor m) {
        float dx = (s.x + s.width / 2) - (m.x + m.size / 2);
        float dy = (s.y + s.height / 2) - (m.y + m.size / 2);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float nearMissRange = (s.width / 2 + m.size / 2) * 1.5f;
        float collisionRange = (s.width / 2 + m.size / 2) * 0.7f;
        return distance < nearMissRange && distance > collisionRange;
    }

    class Spaceship {
        float x, y, width, height;

        Spaceship(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    class Meteor {
        float x, y, size, speed;
        boolean countedAsNearMiss;

        Meteor(float x, float y, float size, float speed) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.countedAsNearMiss = false;
        }

        void update() {
            y += speed;
        }
    }

    class Star {
        float x, y, size;

        Star(float x, float y, float size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}