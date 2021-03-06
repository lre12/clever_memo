package com.github.irshulx.wysiwyg.Utilities.DrawManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.DrawableContainer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.github.irshulx.wysiwyg.NLP.MemoLoadManager;

import java.util.ArrayList;

public class PaintViewManager {

    int lastId = 0;
    int LastTouchViewID;
    RelativeLayout scroll;
    ArrayList<MyPaintView> myPaintViewPool = new ArrayList<MyPaintView>();

    public int getLastTouchViewID() {
        return LastTouchViewID;
    }

    public void setLastTouchViewID(int lastTouchViewID) {
        LastTouchViewID = lastTouchViewID;
    }

    public PaintViewManager(Context context, RelativeLayout scroll, int numPage ,int width, int height) {
        for(int i = 0 ; i < numPage ; i++) {
            final MyPaintView myPaintView = new MyPaintView(context, null, i+1);
            myPaintView.setMinimumHeight(width);
            myPaintView.setMinimumWidth(height);
            myPaintView.setId(i +1);
            myPaintView.setOnTouchListener(new View.OnTouchListener(){
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.e("touchtouch","tocuh");
                    LastTouchViewID = myPaintView.getViewId();
                    for(MyPaintView p : myPaintViewPool){
                        if(LastTouchViewID != p.getViewId()){
                            p.noTouch();
                        }
                        else
                            p.lastTouch();
                    }
                    return false;
                }
            });
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(i != 0)
                layoutParams.addRule(RelativeLayout.BELOW, i);
            lastId = i+1;
            myPaintView.setLayoutParams(layoutParams);
            this.scroll =scroll;
            scroll.addView(myPaintView);
            myPaintViewPool.add(myPaintView);
        }
    }


    public ArrayList<MyPaintView> getMyPaintViewPool() {
        return myPaintViewPool;
    }

    public void setBitmapItSelfById(int id){
        MyPaintView myPaintView = getMyPaintViewById(id);
        myPaintView.setImageBitmap(myPaintView.getDrawContaioner().getmBit().getBitmap());
    }

    public void setContainerById(int id, DrawContaioner drawContaioner){
        MyPaintView myPaintView = getMyPaintViewById(id);
        myPaintView.setDrawContaioner(drawContaioner);
    }

    public void setupToUseById(int id, Context context){
        MyPaintView myPaintView = getMyPaintViewById(id);
    }

    public MyPaintView getMyPaintViewById(int id){
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            MyPaintView myPaintView = myPaintViewPool.get(i);
            if(myPaintView.getId() == id)
                return myPaintView;
        }
        return null;
    }

    public void remove(MyPaintView myPaintView){
        myPaintViewPool.remove(myPaintView);
    }

    public MyPaintView clearPaintViewByid(int id){
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            MyPaintView myPaintView = myPaintViewPool.get(i);
            if(myPaintView.getId() == id) {
                myPaintView.clear();
                myPaintViewPool.remove(myPaintView);
                return myPaintView;
            }
        }
        return  null;
    }


    public void addPaintViewById(int id, DrawContaioner drawContaioner){
        MyPaintView myPaintView = null;
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            myPaintView = myPaintViewPool.get(i);
            if(myPaintView.getId() == id)
                break;
        }
        myPaintView.setDrawContaioner(drawContaioner);
    }

    public int getLeastClearedViewId() {
        MyPaintView myPaintView = null;
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            myPaintView = myPaintViewPool.get(i);
            if(myPaintView.isCleared() == true)
                return myPaintView.getId();
        }
        return -1;
    }

    public void clearPaintViewByPageNum(int pageNum) {
        MyPaintView myPaintView = getPaintViewByPageNum(pageNum);
        myPaintView.clear();
    }

    public MyPaintView getPaintViewByPageNum(int pageNum){
        MyPaintView myPaintView = null;
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            MyPaintView tmpMyPaintView = myPaintViewPool.get(i);
            if(tmpMyPaintView.getDrawContaioner() != null)
                Log.e("correct", pageNum +" " +tmpMyPaintView.getDrawContaioner().getPageNum());
                if(tmpMyPaintView.getDrawContaioner().getPageNum() == pageNum) {

                    myPaintView = tmpMyPaintView;
                }

        }

        return myPaintView;
    }

    public void printAllViewId() {
        MyPaintView myPaintView = null;
        for(int i = 0 ; i < myPaintViewPool.size() ; i++) {
            myPaintView = myPaintViewPool.get(i);
           Log.e("View id", myPaintView.getId() + "");
        }
    }

    public void addNewViewById(Context context, DrawContaioner drawableContainer, int viewId) {
        final MyPaintView myPaintView = new MyPaintView(context, null,viewId);
        myPaintView.setDrawContaioner(drawableContainer);
        myPaintView.setId(viewId);
        myPaintView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("touchtouch","tocuh");
                LastTouchViewID = myPaintView.getViewId();
                for(MyPaintView p : myPaintViewPool){
                    if(LastTouchViewID != p.getViewId()){
                        p.noTouch();
                    }
                    else
                        p.lastTouch();
                }
                return false;
            }
        });
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, lastId);
        myPaintView.setLayoutParams(layoutParams);
        myPaintViewPool.add(myPaintView);
        lastId = viewId;
    }

    public void addPaintView(MyPaintView myPaintView){
        myPaintViewPool.add(myPaintView);
    }

    public void addViewToScrollById(int id){
        MyPaintView myPaintView = getMyPaintViewById(id);
        scroll.addView(myPaintView);

    }

    public void replace(MyPaintView removeView, Context context) {
        final MyPaintView myPaintView = new MyPaintView(context, null, removeView.getId());
        myPaintView.setId(removeView.getId());
        myPaintView.setMinimumHeight(removeView.getHeight());
        myPaintView.setMinimumWidth(removeView.getWidth());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.BELOW, removeView.getId()-1);
        myPaintView.setLayoutParams(layoutParams);
        scroll.addView(myPaintView);
        scroll.removeView(removeView);
        myPaintViewPool.remove(removeView);
        myPaintViewPool.add(myPaintView);

        myPaintView.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.e("touchtouch","tocuh");
                LastTouchViewID = myPaintView.getViewId();
                for(MyPaintView p : myPaintViewPool){
                    if(LastTouchViewID != p.getViewId()){
                        p.noTouch();
                    }
                    else
                        p.lastTouch();
                }
                return false;
            }
        });

    }
}
