package com.github.irshulx.wysiwyg.NLP;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.github.irshulx.wysiwyg.Database.DatabaseManager;
import com.github.irshulx.wysiwyg.Model.Noun;
import com.github.irshulx.wysiwyg.NLP.Twitter;
import com.github.irshulx.wysiwyg.R;
import com.github.irshulx.wysiwyg.Utilities.DrawManager.MyPaintView;
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
import java.util.Vector;

import scala.util.control.TailCalls;
import yuku.ambilwarna.AmbilWarnaDialog;
import com.rtugeek.android.colorseekbar.ColorSeekBar;


public class MemoLoadManager extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int FILE_SELECT_CODE = 0;
    private ColorSeekBar mColorSeekBar;
    private SeekBar strokeSeekBar;
    private int change_stroke = 10; // 굵기 변경용
    private int change_color; // 색상 변경용
    String imageFilePath;
    Uri uri;
    int pagew, pageh;
    MyPaintView paintArr[];
    int pdfCount;
    int lastTouch;
    String memoName;
    int tColor = 0;
    NLPManager nlpManager;
    static final int LOAD_PAGE = 10;
    int loadedPageIndex;
    File memoFolder;

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            openNewMemo();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfcanvas);
        nlpManager = NLPManager.getInstance();
        imageFilePath = getFilesDir().toString() + "/memoImage";
        showFileChooser(); // 파일 선택
        Log.e("끝남", "ㅇㅁㄴㅇㄴㅁ");
    }


    public void showFileChooser() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File"),
                    FILE_SELECT_CODE);

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
                    File file = new File(RealPathUtil.getRealPath(getApplicationContext(), uri));
                    memoName = file.getName();
                    if (isExistMemo()) {
                        Toast.makeText(getApplicationContext(), "이미 존재하는 메모입니다", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        makeFolder();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Vector<Noun> nouns = null;
                                try {
                                    nouns = nlpManager.getResultFrequencyDataFromPdfFilePath(RealPathUtil.getRealPath(getApplicationContext(), uri));
                                    nlpManager.pushNewMemoAndResultFrequency(memoName, nouns, pdfCount);
//                                    nlpManager.saveMemoInDatabase();
//                                    nlpManager.saveWordInDatabase();
//                                    nlpManager.printWordDB();
//                                    nlpManager.printTfDB();
                                } catch (FileNotFoundException e) {
                                    Log.e("Error", e.getMessage());
                                }
                            }
                        }).start();

                        new Thread() {
                            public void run() {
                                Message msg = handler.obtainMessage();
                                handler.sendMessage(msg);
                            }
                        }.start();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    boolean isExistMemo() {
        String path = imageFilePath + "/" + memoName;
        File file = new File(path);
        if (file.exists() && file.isDirectory())
            return true;
        return false;
    }

    void makeFolder() {
        memoFolder = new File(imageFilePath, memoName);
        if (!memoFolder.exists()) {
            memoFolder.mkdirs();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Editor?")
                .setMessage("메모를 종료하시겠습니까?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for(int i = 0 ; i < pdfCount ; i++){
                            saveImage(paintArr[i].getCanvasBit(),i); //수정된 이미지 저장
                        }
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    void openNewMemo() {
        final RelativeLayout scroll = findViewById(R.id.scroll);
        ParcelFileDescriptor fd = null;
        try {
            fd = this.getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ;

        PdfiumCore pdfiumCore = new PdfiumCore(this);
        try {
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
            pdfCount = pdfiumCore.getPageCount(pdfDocument);
            paintArr = new MyPaintView[pdfCount];
            int pageNum = 0;
            for (int i = 0; i < pdfCount; i++) {
                pdfiumCore.openPage(pdfDocument, pageNum);

                int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNum);
                int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNum);

                if (width > 0 && height > 0) {
                    pagew = scroll.getMeasuredWidth();
                    pageh = height * pagew / width;
                    final Bitmap bitmap = Bitmap.createBitmap(pagew, pageh, Bitmap.Config.RGB_565);
                    pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNum, 0, 0,
                            scroll.getMeasuredWidth(), height * scroll.getMeasuredWidth() / width);
                    final int index = i;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            saveImage(bitmap, index); //이거안됨
                        }
                    });

                    MyPaintView img = new MyPaintView(getApplicationContext(), null, bitmap, i, pagew, pageh);
                    img.setId(i);
                    paintArr[i] = img;
                    final int nowTouch = i;
                    img.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            lastTouch = nowTouch;
                            return false;
                        }
                    });
                    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    layoutParams.addRule(RelativeLayout.BELOW, i - 1);
                    img.setLayoutParams(layoutParams);
                    scroll.addView(img);
                    pageNum++;
                    loadedPageIndex = i;
                }
            }
            pdfiumCore.closeDocument(pdfDocument);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void saveImage(Bitmap bitmap, int pageNum) {
        try {
        //    String path = imageFilePath + "/" + memoName;
            File file = new File(memoFolder ,pageNum+".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pdfmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stroke:
                showWriteSetup();
                break;
            case R.id.colorPick:
                eraserSetup();
                break;
            case R.id.undo:
                paintArr[lastTouch].onClickUndo();
                break;
            case R.id.redo:
                paintArr[lastTouch].onClickRedo();
                break;
            case R.id.erase:
                for(MyPaintView p : paintArr){
                    p.setEraseMode();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showWriteSetup() { // 색상/굵기 변경 설정

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewInDialog = inflater.inflate(R.layout.seekbar_color_stroke, null);
        final AlertDialog penDialog = new AlertDialog.Builder(this).setView(viewInDialog).create();

        penDialog.show();

        Window window = penDialog.getWindow(); // dialog 상단 배치
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);

        mColorSeekBar = (ColorSeekBar) viewInDialog.findViewById(R.id.colorSlider);
        mColorSeekBar.setColorSeeds(R.array.text_colors);
        strokeSeekBar = viewInDialog.findViewById(R.id.strokeSlider);

        final TextView textView = (TextView) viewInDialog.findViewById(R.id.textView);
        final ImageView oval_image = (ImageView) viewInDialog.findViewById(R.id.oval_image);
        final GradientDrawable drawable_color = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.shape);
        change_stroke=10;
//        drawable_color.setStroke(change_stroke,change_color);
//        oval_image.setImageDrawable(drawable_color);
        for (int i = 1; i < pdfCount; i++)
            paintArr[i].setStrokeWidth(change_stroke);

        mColorSeekBar.setMaxPosition(100);
        mColorSeekBar.setThumbHeight(30);
        mColorSeekBar.setDisabledColor(Color.GRAY);
        mColorSeekBar.setOnInitDoneListener(new ColorSeekBar.OnInitDoneListener() {
            @Override
            public void done() {
                Log.i(TAG,"done!");
            }
        });

        mColorSeekBar.setOnColorChangeListener(new ColorSeekBar.OnColorChangeListener() {
            @Override
            public void onColorChangeListener(int colorBarPosition, int alphaBarPosition, int color) { // 색상 변경
                change_color = mColorSeekBar.getColor();
                textView.setTextColor(change_color); // 텍스트 색
                drawable_color.setStroke(change_stroke ,change_color); // 테두리 굵기/색
                oval_image.setImageDrawable(drawable_color); // 적용
                for (int i = 0; i < pdfCount; i++)
                    paintArr[i].setColor(change_color);
            }
        });



        strokeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { // 굵기 변경
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                change_stroke=progress+10;
                drawable_color.setStroke(change_stroke,change_color); // 테두리 굵기/색
                oval_image.setImageDrawable(drawable_color); // 적용
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                for (int i = 1; i < pdfCount; i++) {
                    paintArr[i].setStrokeWidth(seekBar.getProgress()); // 굵기선택 끝날 시 일괄 변경
                }
            }
        });
    }






    private void eraserSetup() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, tColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                for (int i = 0; i < pdfCount; i++) {
                    paintArr[i].setColor(color);
                }
            }
        });
        colorPicker.show();
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
//            paintArr = new NewMemoLoadManager.MyPaintView[pageNum];
//            for(int i = 0 ; i < pageNum; i++){
//                BitmapFactory.Options option = new BitmapFactory.Options();
//                option.inPreferredConfig = Bitmap.Config.RGB_565;
//                Bitmap bitmap = BitmapFactory.decodeFile(getFilesDir().toString()+"/memoImage/" +memoName + "/" + i, option);
//                pageh = bitmap.getHeight();
//                pagew = bitmap.getWidth();
//
//
//                NewMemoLoadManager.MyPaintView img = new NewMemoLoadManager.MyPaintView(this,null,bitmap);
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

