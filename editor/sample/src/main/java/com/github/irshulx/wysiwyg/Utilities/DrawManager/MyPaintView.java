package com.github.irshulx.wysiwyg.Utilities.DrawManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.github.irshulx.wysiwyg.R;

import java.util.ArrayList;
import java.util.Vector;


public class MyPaintView extends View {
    private Canvas mCanvas;
    private CusmtomPath mPath;
    private Paint mPaint;
    private Paint erasePaint;
    private Bitmap mBit;
    private ArrayList<CusmtomPath> paths = new ArrayList<CusmtomPath>();
    private ArrayList<CusmtomPath> undonePaths = new ArrayList<CusmtomPath>();
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    int pageNum;
    int height;
    int width;
    Bitmap canvasBit;
    boolean eraseMode = false;
    boolean eraseFlag = false;
    CusmtomPath erasePath;
    int tempColor;
    float tempStroke;


    public float getTempStroke() {
        return mPaint.getStrokeWidth();
    }
    public MyPaintView(Context context, AttributeSet attributeSet, Bitmap mBit, int pageNum, int pagew, int pageh) {
        super(context, attributeSet);
        mPaint = new Paint();
        mPaint.setAntiAlias(true); // 경계면 부드럽게 처리
        mPaint.setDither(true); // 이미지의 색상을 낮추어 출력
        mPaint.setColor(Color.BLACK); // 펜 색깔
        this.mBit = mBit;
        mPaint.setStyle(Paint.Style.STROKE); // 채우기 없음
        mPaint.setStrokeJoin(Paint.Join.ROUND); // 선의 끝 모양
        mPaint.setStrokeCap(Paint.Cap.ROUND); // 선 끝나는 지점 둥글게
        mPaint.setStrokeWidth(6);
        mPath = new CusmtomPath();
        canvasBit =  Bitmap.createBitmap(pagew,pageh,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(canvasBit);
        height = pageh;
        width = pagew;
        this.pageNum = pageNum;
        erasePaint = new Paint();
        erasePaint.setAntiAlias(true);
        erasePaint.setDither(true);
        erasePaint.setColor(context.getColor(R.color.lightGray));
        erasePaint.setStyle(Paint.Style.STROKE);
        erasePaint.setStrokeJoin(Paint.Join.ROUND);
        erasePaint.setStrokeCap(Paint.Cap.ROUND);
        erasePaint.setStrokeWidth(20);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBit, 0, 0, null);
        getParent().requestDisallowInterceptTouchEvent(true);
        tempColor = mPaint.getColor();
        tempStroke = mPaint.getStrokeWidth();
        int i =0;
        for (Path p : paths){
            mPaint.setColor(paths.get(i).color);
            mPaint.setStrokeWidth(paths.get(i).stroke);
            canvas.drawPath(p, mPaint);
            i++;
        }
        mPaint.setColor(tempColor);
        mPaint.setStrokeWidth(tempStroke);
        if(eraseFlag&&eraseMode){
            canvas.drawPath(erasePath,erasePaint);
        }
        canvas.drawPath(mPath, mPaint);
        if(eraseFlag!=eraseMode)
            eraseMode = eraseFlag;
    }

    public void onClickUndo () {
        if (paths.size()>0) {
            undonePaths.add(paths.remove(paths.size()-1));
            invalidate();
        }else{
        }

    }

    public void setEraseMode(){
        eraseMode = !eraseMode;
        eraseFlag = !eraseFlag;
    }

    public void onClickRedo (){
        if (undonePaths.size()>0){
            paths.add(undonePaths.remove(undonePaths.size()-1));
            invalidate();
        }else {
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){

        float x = event.getX();
        float y = event.getY();


        if(event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER)
        {
            if(eraseMode == false) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        undonePaths.clear();
                        mPath.reset();
                        mPath.moveTo(x, y);
                        mPath.x.add(x);
                        mPath.y.add(y);
                        mX = x;
                        mY = y;
                        invalidate();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(x - mX);
                        float dy = Math.abs(y - mY);
                        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                            mX = x;
                            mY = y;
                        }
                        mPath.x.add(x);
                        mPath.y.add(y);
                        invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        mPath.lineTo(mX, mY);
                        mPath.color = mPaint.getColor();
                        mPath.stroke = mPaint.getStrokeWidth();
                        paths.add(mPath);
                        mPath = new CusmtomPath();
                        mPath.x.add(x);
                        mPath.y.add(y);
                        invalidate();
                        break;
                }

            }
            else{
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        erasePath = new CusmtomPath();
                        erasePath.reset();
                        erasePath.moveTo(x, y);
                        erasePath.x.add(x);
                        erasePath.y.add(y);
                        mX = x;
                        mY = y;
                        invalidate();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(x - mX);
                        float dy = Math.abs(y - mY);
                        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                            erasePath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                            mX = x;
                            mY = y;
                        }
                        erasePath.x.add(x);
                        erasePath.y.add(y);
                        invalidate();
                        break;

                    case MotionEvent.ACTION_UP:
                        erasePath.lineTo(mX, mY);
                        erasePath.x.add(x);
                        erasePath.y.add(y);
                        erase();
                        invalidate();
                        eraseMode = false;
                        invalidate();
                        break;
                }
            }
            return true;
        }
        return true;
    }
    public void setStrokeWidth(int width){
        mPaint.setStrokeWidth(width);
    }

    public void erase(){

        for(int i = 0 ; i < paths.size() ; i++){
            CusmtomPath path = paths.get(i);
            for(int j = 0 ; j < erasePath.x.size() ; j++){
                float x = erasePath.x.get(j);
                float y = erasePath.y.get(j);
                for(int z = 0 ; z < path.x.size() ; z++){
                    float exist_x = path.x.get(z);
                    float exist_y = path.y.get(z);
                    if((x - 20 <= exist_x && exist_x <= x+20)&& (y -20 <= exist_y && exist_y <= y +20))
                        paths.remove(path);
                    Log.e("exist_X", exist_x + " ");
                    Log.e("exist_Y", exist_y + " ");
                    Log.e("x", x + "");
                    Log.e("y", y + "");
                }
            }
        }
    }


    public void setColor(int color){
        mPaint.setColor(color);
    }
    public int getColor() {return mPaint.getColor();}
    public Bitmap getCanvasBit(){
        Canvas saveCanvas = new Canvas(mBit);
        int i =0;
        for (Path p : paths){
            mPaint.setColor(paths.get(i).color);
            mPaint.setStrokeWidth(paths.get(i).stroke);
            mCanvas.drawPath(p, mPaint);
            i++;
        }
        saveCanvas.drawBitmap(canvasBit,0,0,null);
        return mBit;
    }
}




class CusmtomPath extends Path{

    Vector<Float> x;
    Vector<Float> y;
    int color;
    float stroke;

    CusmtomPath() {
        x = new Vector<Float>();
        y = new Vector<Float>();
    }
}


