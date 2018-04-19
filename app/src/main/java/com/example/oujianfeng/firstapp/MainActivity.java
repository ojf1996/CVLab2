package com.example.oujianfeng.firstapp;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
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


import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity
{
    public static final int SELECT_IMAGE = 0;
    public static final String STARTCUT = "start cutting the inmage";
    protected static Bitmap bitmap = null;

    //转为灰度图
    protected Bitmap ARGBConvert2Gray(Bitmap origin)
    {
        int width = origin.getWidth();
        int height = origin.getHeight();
        Bitmap bitmap = origin.copy(Config.ARGB_8888,true);
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int col = bitmap.getPixel(i, j);
                int alpha = col & 0xFF000000;
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                int gray = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                int newColor = alpha | (gray << 16) | (gray << 8) | gray;
                bitmap.setPixel(i, j, newColor);
            }
        }
        return bitmap;
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

        ops.inSampleSize = calculateInSampleSize(ops,screenW,screenH);

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
            return ARGBConvert2Gray(bitmaps[0]);
        }

        protected void onPostExecute(Bitmap res) {
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
        /*
        if(bitmap != null)
        {
            new imageProcessTask().execute(bitmap);
        }
        */
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
