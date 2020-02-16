package com.github.irshulx.wysiwyg.NLP;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.irshulx.wysiwyg.NLP.Twitter;
import com.github.irshulx.wysiwyg.R;
import com.github.irshulx.wysiwyg.Utilities.RealPathUtil;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import scala.util.control.TailCalls;
import yuku.ambilwarna.AmbilWarnaDialog;


public class MemoLoadManager extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int FILE_SELECT_CODE = 0;
    String imageFilePath;
    Uri uri;
    int pagew, pageh;
    MyPaintView paintArr[];
    int pdfCount;
    int lastTouch;
    String memoName;
    int tColor=0;

    public void showFileChooser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File"),
                    FILE_SELECT_CODE);
            Log.e("dsad", "not here");

        } catch (Exception e) {
            Log.e("error", e.getMessage());
//            Toast.makeText(this, "Please install a File Manager.",
//                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    uri = data.getData();
                    memoName = uri.getLastPathSegment();
                    if(isExistMemo()) {
                        Toast.makeText(getApplicationContext(), "이미 존재하는 메모입니다", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else{
                        makeFolder();
                        openNewMemo();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean isExistMemo(){
        String path = getFilesDir().toString()+"/" + "memoImage/" +memoName;
        File file = new File(path);
        if(file.exists() && file.isDirectory())
            return true;
        return false;
    }
    void makeFolder() {
        String path = getFilesDir().toString()+"/" + "memoImage";
        File file = new File(path,memoName);
        if(!file.exists()){
            file.mkdirs();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfcanvas);
        showFileChooser();
    }

    void openNewMemo() {
        RelativeLayout scroll = findViewById(R.id.scroll);
        ParcelFileDescriptor fd = null;
        try {
            fd = this.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ;
        int pageNum = 0;
        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfCount = pdfiumCore.getPageCount(pdfDocument);
            paintArr = new MyPaintView[pdfCount];

            for(int i = 0 ; i < pdfCount ; i++){
                pdfiumCore.openPage(pdfDocument, pageNum);

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

                if(width>0&&height>0){
                    pagew = scroll.getMeasuredWidth()-50;
                    pageh = height*pagew/width;
                    Log.e("w" , pagew+"");
                    Log.e("h" , pageh+"");
                    Bitmap bitmap = Bitmap.createBitmap(pagew, pageh, Bitmap.Config.RGB_565);
                    Log.e("info", pagew + " " + pageh);

                    pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                            scroll.getMeasuredWidth(), height*scroll.getMeasuredWidth()/width);
                    saveImage(bitmap, i);
                    MyPaintView img = new MyPaintView(this,null,bitmap);
                    img.setId(i);
                    paintArr[i] = img;
                    final int lastNum = i;
                    img.setOnTouchListener(new View.OnTouchListener(){
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            lastTouch = lastNum;
                            return false;
                        }
                    });
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.BELOW, i-1);
                    img.setLayoutParams(layoutParams);
                    scroll.addView(img);
                    pageNum++;
                }
            }
            pdfiumCore.closeDocument(pdfDocument);
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    void saveImage(Bitmap bitmap, int pageNum){
        try {
            String path = imageFilePath + memoName;
            File file = new File(path +"/"+ pageNum);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        }
        catch(Exception e){
            Log.e("Error" , e.getMessage());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pdfmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.stroke:
                showWriteSetup();
                break;
            case R.id.colorPick:
                openColorPicker();
                break;
            case R.id.undo:
                paintArr[lastTouch].onClickUndo();
                break;
            case R.id.redo:
                paintArr[lastTouch].onClickRedo();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showWriteSetup() {
        final EditText editText=new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);


        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setTitle("AlertDialog Title");
        builder.setMessage("굵기 입력");
        builder.setView(editText);
        builder.setPositiveButton("입력",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        for(int i = 0 ; i < pdfCount ; i++)
                        {
                            paintArr[i].setStrokeWidth(Integer.parseInt(editText.getText().toString()));
                        }

                    }
                });
        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.show();

    }

    private void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, tColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                for(int i = 0 ; i < pdfCount ; i++)
                {
                    paintArr[i].setColor(color);
                }
            }
        });
        colorPicker.show();
    }

    class MyPaintView extends View {
        private Canvas mCanvas;
        private Path mPath;
        private Paint mPaint;
        private Bitmap mBit;
        private ArrayList<Path> paths = new ArrayList<Path>();
        private ArrayList<Path> undonePaths = new ArrayList<Path>();
        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        public MyPaintView(Context context, AttributeSet attributeSet, Bitmap mBit) {
            super(context, attributeSet);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setDither(true);
            mPaint.setColor(Color.BLACK);
            this.mBit = mBit;
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(6);
            mPath = new Path();
            mCanvas = new Canvas();
        }
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = pageh;
            int width = pagew;
            setMeasuredDimension(width, height);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mBit != null) {
                Log.e("dd", "그립니다");
                canvas.drawBitmap(mBit, 0, 0, null);
            }
            else{
                Log.e("dd", "널입니다");
            }
            getParent().requestDisallowInterceptTouchEvent(true);
            super.onDraw(canvas);
            for (Path p : paths){
                canvas.drawPath(p, mPaint);
            }
            canvas.drawPath(mPath, mPaint);
        }

        public void onClickUndo () {
            if (paths.size()>0) {
                undonePaths.add(paths.remove(paths.size()-1));
                invalidate();
            }else{
            }
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
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    undonePaths.clear();
                    mPath.reset();
                    mPath.moveTo(x, y);
                    mX = x;
                    mY = y;
                    invalidate();
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(x - mX);
                    float dy = Math.abs(y - mY);
                    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                        mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                        mX = x;
                        mY = y;
                    }
                    invalidate();
                    break;

                case MotionEvent.ACTION_UP:
                    mPath.lineTo(mX, mY);
                    mCanvas.drawPath(mPath, mPaint);
                    paths.add(mPath);
                    mPath = new Path();
                    invalidate();
                    break;
            }
            return true;
        }
        public void setStrokeWidth(int width){
            mPaint.setStrokeWidth(width);
        }


        public void setColor(int color){
            mPaint.setColor(color);

        }
    }
}

////TODO 형태소분석 클래스에 합병
//    void analisysPDF(Intent data){
//        final Intent pdfData = data;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                uri = pdfData.getData();
//                String filePath = RealPathUtil.getRealPath(getApplicationContext(), uri);
//                File file = new File(filePath);
//                String text = "";
//                try {
//                    PdfReader reader = new PdfReader(new FileInputStream(file));
//                    int n = reader.getNumberOfPages();
//                    for(int i =0 ; i < n ; i++) {
//                        text += PdfTextExtractor.getTextFromPage(reader, i + 1) + "\n";
//                    }
//
//                }
//                catch(Exception e){
//                }
//            }
//        }).start();
//    }



//TODO 기존 메모 로드하는 클래스에 합병할 것.
//    void loadExistMemo(){
//        RelativeLayout scroll = findViewById(R.id.scroll);
//        File file = new File(getApplicationContext().getFilesDir().toString() + "/memoImage/" + memoName );
//        int pageNum = file.list().length;
//        Log.e("PageNum", pageNum + "");
//
//        try {
//            pdfCount = pageNum;
//            paintArr = new MemoLoadManager.MyPaintView[pageNum];
//            for(int i = 0 ; i < pageNum; i++){
//                BitmapFactory.Options option = new BitmapFactory.Options();
//                option.inPreferredConfig = Bitmap.Config.RGB_565;
//                Bitmap bitmap = BitmapFactory.decodeFile(getFilesDir().toString()+"/memoImage/" +memoName + "/" + i, option);
//                pageh = bitmap.getHeight();
//                pagew = bitmap.getWidth();
//
//
//                MemoLoadManager.MyPaintView img = new MemoLoadManager.MyPaintView(this,null,bitmap);
//                img.setId(i);
//                paintArr[i] = img;
//                final int lastNum = i;
//                img.setOnTouchListener(new View.OnTouchListener(){
//                    @Override
//                    public boolean onTouch(View v, MotionEvent event) {
//                        lastTouch = lastNum;
//                        return false;
//                    }
//                });
//                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                layoutParams.addRule(RelativeLayout.BELOW, i-1);
//                img.setLayoutParams(layoutParams);
//                scroll.addView(img);
//            }
//        } catch(Exception e) {
//            Log.e("Error",e.getMessage());
//        }
//    }