package com.example.oujianfeng.firstapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Display;
import android.graphics.Point;
import android.widget.ImageView;
import android.content.Intent;
import android.view.MotionEvent;
import android.net.Uri;
import android.util.Log;
import android.util.DisplayMetrics;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity
{
    public static final int SELECT_IMAGE = 0;
    public static final String STARTCUT = "start cutting the inmage";
    protected static Bitmap bitmap = null;

    //转为灰度图
    protected int ARGBConvert2Gray(Bitmap origin, int x, int y)
    {
        int width = origin.getWidth();
        int height = origin.getHeight();
        if(x >= width || y >= height)
            return 0;
        int col = origin.getPixel(x, y);
        int red = (col & 0x00FF0000) >> 16;
        int green = (col & 0x0000FF00) >> 8;
        int blue = (col & 0x000000FF);
        int gray = (int) (red * 0.3 + green * 0.59 + blue * 0.11);

        return gray;
    }

    protected float getEdgeVariance(Bitmap origin)
    {
        float varianceSquared = 0.0f;
        int width = origin.getWidth();
        int height = origin.getHeight();
        int time = 0;
        int size = Math.min(width * height, 1000);
        for(time = 0; time < size; ++time)
        {
            int x = (int)(Math.random() * width);
            int y = (int)(Math.random() * height);

            int col = origin.getPixel(x, y);
            int r = (col & 0x00FF0000) >> 16;
            int g = (col & 0x0000FF00) >> 8;
            int b = (col & 0x000000FF);

            for(int i = -1; i < 2 && y + i <height && y + i >= 0; ++i)
            {
                for(int j = 0; j < 2 && x + j < width && x + j >=0; ++j)
                {
                    int ncol = origin.getPixel(x + j, y + i);
                    int nr = (ncol & 0x00FF0000) >> 16;
                    int ng = (ncol & 0x0000FF00) >> 8;
                    int nb = (ncol & 0x000000FF);

                    varianceSquared+= (b-nb)*(b-nb) + (g-ng)*(g-ng) + (r-nr)*(r-nr);
                }
            }
        }

        varianceSquared /= (time * 1.0);
        return varianceSquared;
    }
    //实现图割
    protected Bitmap graphcut(Bitmap origin)
    {
        Log.v("bitmap",origin.toString());
        Log.v("init","=================================");
        //////////////////////////////////////////////
        // 进行预处理，包括获取种子点，转化灰度信息 //
        //////////////////////////////////////////////
        List<Point> obj = ((ScribbleView)findViewById(R.id.imageView)).mask_;
        List<Point> bkg = ((ScribbleView)findViewById(R.id.imageView)).mask_bkg;
        Log.v("init done","=================================");
        Log.v("compute","=================================");
        int width = origin.getWidth();
        int height = origin.getHeight();
        float varianceSquared = getEdgeVariance(origin);
        Log.v("done","=================================");
        //double EDGE_STRENGTH_WEIGHT = 0.95f;
        //创建图
        Graph_ graph = new Graph_(width * height + 2);
        //初始化
        graph.init();
        Log.v("init done","=================================");
        Log.v("init edge","=================================");
        int col,ncol;
        int r,g,b,nr,ng,nb;
        double currEdgeStrength,currDist;
        short cap;
        int si,sj,ni,nj;
        int index,nNodeId;
        //为每一条边赋值，因为是双向赋值，所以只对上，右上，右下赋值，形成8邻域
        for(int j = 0; j < height; ++j)
        {
            for(int i =0; i < width; ++i)
            {
                index = j * width + i;

                col = origin.getPixel(i, j);
                r = (col & 0x00FF0000) >> 16;
                g = (col & 0x0000FF00) >> 8;
                b = (col & 0x000000FF);

                //不考虑边缘点的影响
                for(sj = -1; sj <2 && sj + i < height && sj + i >= 0; sj++)
                {
                    nj = sj + j;

                    if(nj < 0 || nj >= height)
                        continue;

                    for(si = 0; si < 2; si++) {
                        ni = si + i;
                        if (ni < 0 || ni >= width)
                            continue;
                        if (si >= 0 && sj == 0)//不计算本身和下方
                            continue;
                        nNodeId = (j + sj) * width + (i + si);

                        ncol = origin.getPixel(i + si, j + sj);
                        nr = (ncol & 0x00FF0000) >> 16;
                        ng = (ncol & 0x0000FF00) >> 8;
                        nb = (ncol & 0x000000FF);

                        currEdgeStrength = Math.exp(-((b - nb) * (b - nb) + (g - ng) * (g - ng) + (r - nr) * (r - nr)) / (2 * varianceSquared));
                        currDist = Math.sqrt((float) si * (float) si + (float) sj * (float) sj);

                        currEdgeStrength = currEdgeStrength / currDist;

                        cap = (short)(Math.ceil(currEdgeStrength * 1000 + 0.5));

                        graph.addEdge(index, nNodeId, cap, cap);
                    }
                }
            }
        }
        Log.v("init done","=================================");
        Log.v("init seed","=================================");
        //初始化种子点
        int indexOfSource = width * height;
        int indexOfSink = indexOfSource + 1;

        int x,y,i;
        for(i = 0; i < obj.size(); ++i)
        {
            x = obj.get(i).x;
            y = obj.get(i).y;
            graph.addEdge(y * width + x,indexOfSource,Short.MAX_VALUE, Short.MAX_VALUE);
        }

        for(i = 0; i < bkg.size(); ++i)
        {
            x = bkg.get(i).x;
            y = bkg.get(i).y;
            graph.addEdge( y * width + x,indexOfSink,Short.MAX_VALUE, Short.MAX_VALUE);
        }

        Log.v("init done","=================================");
        float flow = graph.maxflow(indexOfSource,indexOfSink);
        Log.v("maxflow","=================================");
        Log.v("flow",Float.toString(flow));
        Log.v("maxflow done","=================================");
        //可视化
        Bitmap res = origin.copy(Config.ARGB_8888,true);
        for(i=0; i < height; ++i)
        {
            for(int j =0; j < width; ++j)
            {
                if(graph.whatSegment(i * width + j))
                    res.setPixel(j,i, Color.GREEN);
            }
        }
        Log.v("visualize done","=================================");
        //解锁
        return res;
    }

    //计算采样的次数
    protected static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //由于Android存储空间有限，要先计算图片的大小，若太大则需要降采样
    //虽然降采样会损失信息，但由于设备的尺寸问题，我个人认为可以接受
    protected Bitmap loadImage(Uri uri) throws FileNotFoundException
    {
        //选项
        final BitmapFactory.Options ops = new BitmapFactory.Options();
        //文件流
        ContentResolver cr = this.getContentResolver();
        InputStream input = cr.openInputStream(uri);
        //先不读取文件流=true to check dimensions
        ops.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input,null,ops);
        try {
            assert input != null;
            if(input.markSupported()) {
                input.reset();
            }
            else
            {
                input.close();
                input = cr.openInputStream(uri);
            }
        }catch(IOException e)
        {
            e.printStackTrace();
        }
        //检查图片大小
        //计算设备尺寸
        int screenH = 0;
        int screenW = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display display = getWindowManager().getDefaultDisplay();
            Point point = new Point();
            display.getRealSize(point);
            screenH = point.y;
            screenW = point.x;
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            screenW = dm.widthPixels; //得到宽度
            screenH = dm.heightPixels; //得到高度
        }

        //计算降采样的程度

        ops.inSampleSize = calculateInSampleSize(ops,400,400);

        //通过降采样解码
        ops.inJustDecodeBounds = false;
        Bitmap res = BitmapFactory.decodeStream(input,null,ops);
        try
        {
            if(input != null)
            input.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }
        return res;
    }

    @SuppressLint("StaticFieldLeak")
    private class loadImageTask extends AsyncTask<Uri,Void,Bitmap>
    {
        protected Bitmap doInBackground(Uri... uris)
        {
            Bitmap res = null;
            try {
                res =  loadImage(uris[0]);
                if(res != null)
                {
                    bitmap = res.copy(Config.ARGB_8888,true);
                }
                else
                {
                    bitmap = null;
                }
                assert res != null;
                res.recycle();
                return bitmap;
            }catch (FileNotFoundException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        protected void onPostExecute(Bitmap res)
        {
            ScribbleView view = findViewById(R.id.imageView);
            view.setSourceImage(res);
        }
    }

    private class imageProcessTask extends AsyncTask<Bitmap,Void,Bitmap>
    {
        protected Bitmap doInBackground(Bitmap... bitmaps) {
            return graphcut(bitmaps[0]);
        }
        protected void onPostExecute(Bitmap res)
        {
            ScribbleView view = findViewById(R.id.imageView);
            view.setSourceImage(res);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return ((ScribbleView)findViewById(R.id.imageView)).onTouchEvent(event);
    }

    //增加涂鸦的画笔大小
    public void selectObjSeed(View view)
    {
        ((ScribbleView)findViewById(R.id.imageView)).setKindOfSeed(false);
    }
    //减少涂鸦的画笔大小
    public void selectBkgSeed(View view)
    {
        ((ScribbleView)findViewById(R.id.imageView)).setKindOfSeed(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** 用户按下选择图片的按钮*/
    public void selectImage(View view)
    {
        //隐式调用Intent
        Intent intent  = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,SELECT_IMAGE);
    }

    public void goBack(View view)
    {
        ((ScribbleView)findViewById(R.id.imageView)).back();
    }

    //启动图片处理
    public void startImageProcess(View view)
    {
        if(bitmap != null) {
            ((ScribbleView) findViewById(R.id.imageView)).setSourceImage(graphcut(bitmap));
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        //选取图片操作
        if(requestCode == SELECT_IMAGE)
        {
            if(resultCode == RESULT_OK)
            {
                //获取资源描述符
                Uri uri = data.getData();
                try {
                    new loadImageTask().execute(uri);
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                //操作错误或没有选择图片
                Log.i("MainActivtiy", "operation error");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
