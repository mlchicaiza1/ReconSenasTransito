package com.example.reconsenastransito.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import com.example.reconsenastransito.R;



import java.util.LinkedList;
import java.util.List;

public class OverlayView extends View {
    private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();

    private List<Classifier.Recognition> results;
    public String palabra;
    public Drawable imagen;
    public Drawable imagen1;
    private long lastProcessingTimeMs;

    ClassifierActivity classifierActivity;
    public OverlayView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        imagen=context.getDrawable(R.drawable.like);
        imagen1=context.getDrawable(R.drawable.error);
    }

    /**
     * Interface defining the callback for client classes.
     */
    public interface DrawCallback {
        public void drawCallback(final Canvas canvas);
    }


    public void addCallback(final DrawCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public synchronized void draw(final Canvas canvas) {
        for (final DrawCallback callback : callbacks) {
            callback.drawCallback(canvas);
        }


        int ancho=imagen.getIntrinsicWidth();
        int alto=imagen.getIntrinsicHeight();


        int cont=1;
        imagen.setBounds(350,350,ancho+350,alto+350);
        imagen1.setBounds(350,350,ancho+350,alto+350);

        if (results!=null){
            long startTime = SystemClock.uptimeMillis();
            for (int i=0; i<results.size(); i++){
                String title=results.get(i).getTitle();

                String[] parts = title.split(" ");
                String part1 = parts[0];
                float con=results.get(i).getConfidence();



            }

            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
        }




    }

    public void setResults(final List<Classifier.Recognition> results){
        this.results=results;
        postInvalidate();
    }
    public void getResults(String palabra){
        this.palabra=palabra;
        postInvalidate();

    }
}

