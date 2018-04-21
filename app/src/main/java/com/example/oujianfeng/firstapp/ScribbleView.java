package com.example.oujianfeng.firstapp;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.graphics.Point;

import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Build;
import android.util.Log;
import android.widget.ImageButton;

public class ScribbleView extends SurfaceView implements SurfaceHolder.Callback {
    public ReentrantLock lock = new ReentrantLock();
    //涂鸦类，帮助在画布上涂鸦
    public class Scribble
    {
        //涂鸦的颜色
        public int color;
        //涂鸦的轨迹
        private float startX;
        private float startY;
        private float stopX;
        private float stopY;
        //涂鸦的轨迹大小
        private int size;

        //默认构造函数
        Scribble()
        {
            color = Color.BLACK;
            startX = 0;
            startY = 0;
            stopX = 0;
            stopY = 0;
            size = 1;
        }
        //重载构造函数，接受新的size和color以及轨迹
        Scribble(float x, float y, int size, int color)
        {
            this.color = color;
            startX = x;
            startY = y;
            stopX = x;
            stopY = y;
            this.size = size;
        }

        public void draw(List<Point> mask, int width, int height)
        {
            int dx = (int)stopX-(int)startX;
            int dy = (int)stopY-(int)startY;
            int tempx = dx > 0 ? 1 : 0;
            int tempy = dy > 0 ? 1 : 0;
            int ux = (tempx << 1) - 1;
            int uy = (tempy << 1) - 1;
            int x = (int)startX, y = (int)startY,eps;//exp 为累计误差
            eps = 0;dx = Math.abs(dx); dy = Math.abs(dy);
            if (dx > dy)
            {
                for (x = (int)startX; x != (int)stopX; x += ux)
                {
                    if(x < width && y < height)
                        mask.add(new Point(x,y));
                    eps += dy;
                    if ((eps << 1) >= dx)
                    {
                        y += uy; eps -= dx;
                    }
                }
            }
            else
            {
                for (y = (int)startY; y != (int)stopY; y += uy)
                {
                    if(x < width && y < height) {
                        mask.add(new Point(x,y));
                    }
                    eps += dx;
                    if ((eps << 1) >= dy)
                    {
                        x += ux; eps -= dy;
                    }
                }
            }
        }

        public void move(float mx, float my)
        {
            stopX = mx;
            stopY = my;
        }
    }

    private class MergeTask extends AsyncTask<Bitmap,Void,Bitmap>
    {
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            return addMaskToScribble(bitmaps[0]);
        }

        protected void onPostExecute(Bitmap res) {
            scribble = res;
            Canvas canvas = mSurfaceHolder.lockCanvas();
            canvas.drawBitmap(scribble,0,0,mPaint);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private SurfaceHolder mSurfaceHolder = null;
    private int currentColor = Color.BLACK;//画笔颜色，黑色为bkg，红色为obj
    private int currentSize = 1;//画笔大小
    private Paint mPaint;//画笔

    private Bitmap source = null;//原图
    private Bitmap scribble = null;//涂鸦

    private boolean isBackground = false;

    public List<Point> mask_;//obj
    public List<Point> mask_bkg;//bkg
    private Scribble curScribble = null;

    //初始化函数
    private void init() {
        mSurfaceHolder = this.getHolder();
        mSurfaceHolder.addCallback(this);
        this.setFocusable(true);

        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(currentSize);
    }

    public ScribbleView(Context context)
    {
        super(context);
        init();
    }

    public ScribbleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ScribbleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public void setSourceImage(Bitmap bitmap)
    {
        //释放先前的资源
        if(source != null)
        {
            source.recycle();
            source = null;
        }
        if(scribble != null)
        {
            scribble.recycle();
            scribble = null;
        }
        if(mask_ != null)
        {
            mask_.clear();
        }
        if(mask_bkg != null)
        {
            mask_bkg.clear();
        }
        source = bitmap;

        //加载新图片
        Canvas canvas = mSurfaceHolder.lockCanvas();
        canvas.drawBitmap(source,0,0,mPaint);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if(source != null) {
            canvas.drawBitmap(source, 0, 0, mPaint);
        }
        else
            canvas.drawColor(Color.WHITE);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        mask_ = new ArrayList<Point>();
        mask_bkg = new ArrayList<Point>();
    }

    //nothing is done
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    //释放资源
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(source != null)
            source.recycle();
        source = null;
        if(scribble != null)
            scribble.recycle();
        scribble = null;
        mask_.clear();
        mask_bkg.clear();
        mSurfaceHolder.removeCallback(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(source == null)
            return super.onTouchEvent(event);

        int action = event.getAction();
        if(action == MotionEvent.ACTION_CANCEL)
        {
            return false;
        }
        //获取坐标
        float touchX = event.getRawX();
        float touchY = event.getRawY();
        switch (action) {
            //记录轨迹
            case MotionEvent.ACTION_DOWN:
                curScribble = new Scribble(touchX,touchY,currentSize,currentColor);
                break;

            case MotionEvent.ACTION_MOVE:
                //遮罩
                curScribble.move(touchX, touchY);
                if(isBackground)
                    curScribble.draw(mask_bkg,source.getWidth(),source.getHeight());
                else
                    curScribble.draw(mask_,source.getWidth(),source.getHeight());
                new MergeTask().execute(source);
                break;
            case MotionEvent.ACTION_UP:
                curScribble = null;
                break;

            default:
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 设置画笔的颜色
     * @param color
     */
    public void setColor(String color)
    {
        currentColor = Color.parseColor(color);
    }

    public void setKindOfSeed(boolean bkg)
    {
        isBackground = bkg;
    }

    /**
     * 设置画笔的粗细
     */
    public void addSize()
    {
        if(currentSize < 30)
            currentSize+=5;
    }

    public void narrowSize()
    {
        if(currentSize > 1)
            currentSize-=5;
    }

    //清空画布
    public void back()
    {
        lock.lock();
        if(mask_ != null && !mask_.isEmpty() )
        {
            mask_.clear();
        }
        if(mask_bkg != null && !mask_bkg.isEmpty() )
        {
            mask_bkg.clear();
        }
        Canvas canvas = mSurfaceHolder.lockCanvas();
        canvas.drawBitmap(source,0,0,mPaint);
        mSurfaceHolder.unlockCanvasAndPost(canvas);
        lock.unlock();
    }

    //将遮罩内容覆盖到原图像中
    protected Bitmap addMaskToScribble(Bitmap source_)
    {
        lock.lock();
        Bitmap res = null;
        if(source_ != null) {
            //obj
            int size = mask_.size();
            res = source_.copy(Bitmap.Config.ARGB_8888, true);
            for (int i = 0; i < size; ++i) {
                int x = mask_.get(i).x;
                int y = mask_.get(i).y;
                res.setPixel(x, y, Color.RED);
            }
            //bkg
            size = mask_bkg.size();
            for (int i = 0; i < size; ++i) {
                int x = mask_bkg.get(i).x;
                int y = mask_bkg.get(i).y;
                res.setPixel(x, y, Color.GREEN);
            }
        }
        lock.unlock();
        return res;
    }
}
