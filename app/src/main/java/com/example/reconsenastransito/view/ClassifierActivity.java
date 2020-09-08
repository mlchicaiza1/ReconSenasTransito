package com.example.reconsenastransito.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.support.annotation.NonNull;
import android.util.Size;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.reconsenastransito.MainActivityNavigation;
import com.example.reconsenastransito.env.ImageUtils;
import com.example.reconsenastransito.env.BorderedText;
import com.example.reconsenastransito.model.detecSenaTransito;

//import org.tensorflow.yolo.R;

import com.example.reconsenastransito.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;


import com.example.reconsenastransito.env.Logger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class ClassifierActivity extends CameraActivity implements ImageReader.OnImageAvailableListener, LocationListener {
    private static final Logger LOGGER = new Logger();

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    private static final String INPUT_NAME = "input";
    //    private static final String INPUT_NAME = "Placeholder";
    private static final String OUTPUT_NAME = "final_result";

    private static final String MODEL_FILE = "file:///android_asset/senasTransito.pb";
    private static final String LABEL_FILE = "file:///android_asset/senasTransito.txt";



    //private static final String LABEL_FILE2 = "file:///android_asset/labels.txt";
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private static final boolean MAINTAIN_ASPECT = true;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private Classifier classifier;

    private Integer sensorOrientation;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private byte[][] yuvBytes;
    private int[] rgbBytes = null;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private Bitmap cropCopyBitmap;

    private boolean computing = false;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private ResultsView resultsView;

    private BorderedText borderedText;

    private long lastProcessingTimeMs;

    private Bundle bundle;
    private OverlayView overlayView1;
    private String palabra;
    private String letra;
    private Intent intent;

    private Chronometer chronometer;
    private TextView txtTempor;
    private Switch switchMetric;
    private TextView textViewSpeed,textViewLatitude,textViewLongitude,textViewKm,resultTxt,resultTxt1;
    private ListView listViewResult;
    private ImageView imgViewAlarma,imgViewAccidente;
    private   String velocidad;
    private int count=300;
    private int posicion=35;
    private String idConductor;
    private LottieAnimationView animationView;
    public MediaPlayer ring;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;



    @Override
    //protected int getLayoutId() {
    // return R.layout.camera_connection_fragment;
    //}
    protected int getLayoutId() {
        return R.layout.fragment_camera_connection;
    }
    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    private static final float TEXT_SIZE_DIP = 10;

    private  void  inicializarFirebase(){
        FirebaseApp.initializeApp(this);
        firebaseDatabase =FirebaseDatabase.getInstance();
        databaseReference=firebaseDatabase.getReference();
    }
    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {

        //obtner tamaño del borderedTex para la visualizacion de los datos del reconocimento
        final float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());

        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);


        resultsView = (ResultsView) findViewById(R.id.results);
        overlayView1 = (OverlayView) findViewById(R.id.debug_overlay);
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        inicializarFirebase();



        bundle = this.getIntent().getExtras();

        //Reconocimiento Vocabulario
        if (bundle != null && bundle.getString("dato").length() > 3) {
            //overlayView.getResults(bundle.getString("dato"));
            Toast.makeText(this, "Seleccion: " + bundle.getString("dato"), Toast.LENGTH_SHORT).show();
            overlayView1.getResults(bundle.getString("dato"));
            palabra = bundle.getString("dato");

            classifier = TensorFlowImageClassifier.create(
                    getAssets(),
                    MODEL_FILE,
                    LABEL_FILE,
                    INPUT_SIZE,
                    IMAGE_MEAN,
                    IMAGE_STD,
                    INPUT_NAME,
                    OUTPUT_NAME);
        }

        intent = new Intent(ClassifierActivity.this, MainActivityNavigation.class);


        switchMetric = findViewById(R.id.switchMetric);
        textViewSpeed = findViewById(R.id.textViewSpeed);
        textViewLatitude = findViewById(R.id.textViewLatitude);
        textViewLongitude = findViewById(R.id.textViewLongitude);
        textViewKm = findViewById(R.id.textView2);
        textViewKm.setText("km/h");
        imgViewAccidente = findViewById(R.id.imgViewAccidente);
        imgViewAlarma = findViewById(R.id.imgViewAlarma);

        resultTxt=findViewById(R.id.resultTxt);

        imgViewAlarma.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imgViewAlarma.setVisibility(View.VISIBLE);
            }
        });


        ring=MediaPlayer.create(ClassifierActivity.this,R.raw.prev);
        //check for gps permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);
        }else{
            //start the program if the permission is granted
            doStuff();
        }

        idConductor=bundle.getString("dato");
        this.updateSpeed(null);
        this.updateLocation(null);
        switchMetric.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ClassifierActivity.this.updateSpeed(null);

            }
        });
        final Display display = getWindowManager().getDefaultDisplay();
        final int screenOrientation = display.getRotation();

        LOGGER.i("Sensor orientation: %d, Screen orientation: %d", rotation, screenOrientation);
        sensorOrientation = rotation + screenOrientation;

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbBytes = new int[previewWidth * previewHeight];
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        INPUT_SIZE, INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        yuvBytes = new byte[3][];

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        renderDebug(canvas);
                    }
                });

    }

    @Override
    public void onImageAvailable(final ImageReader reader) {
        Image image = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) {return;}

            if (computing) {
                image.close();
                return;
            }
            computing = true;

            Trace.beginSection("imageAvailable");

            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);

            final int yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();
            ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0],
                    yuvBytes[1],
                    yuvBytes[2],
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes);
            image.close();
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = classifier.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        resultsView.setResults(results);

                        resultTxt.setText(results.toString());
                        overlayView1.setResults(results);

                        imgViewAlarma.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                imgViewAccidente.setVisibility(View.INVISIBLE);
                                imgViewAlarma.setVisibility(View.INVISIBLE);
                            }
                        });
                        if (results!=null){
                            for (int i=0; i<results.size(); i++){
                                String palabraRecog=results.get(i).getTitle();
                                float con=results.get(i).getConfidence();


                                double velocidadAuto = Double.parseDouble(textViewSpeed.getText().toString());

                                if( con >0.8 && velocidadAuto > 30 && palabraRecog.equalsIgnoreCase("treinta")){

                                    ring.start();
                                    try {
                                        synchronized (this) {
                                            wait(50);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    imgViewAccidente.setVisibility(View.VISIBLE);
                                                    imgViewAlarma.setVisibility(View.VISIBLE);


                                                }
                                            });

                                        }

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    detecSenaTransito dectSena=new detecSenaTransito();
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");


                                    Date date = new Date();
                                    String strDate = dateFormat.format(date).toString();

                                    Locale locale = new Locale("fr", "FR");
                                    DateFormat dateFormat1 = DateFormat.getTimeInstance(DateFormat.DEFAULT, locale);
                                    String time = dateFormat1.format(new Date());


                                    dectSena.setId(UUID.randomUUID().toString());
                                    dectSena.setIdConductor(idConductor);
                                    dectSena.setFechaDect(strDate);
                                    dectSena.setHora(time);
                                    dectSena.setPredicSena(con);
                                    dectSena.setSenaTransito(palabraRecog);
                                    databaseReference.child("SenaTransito").child(dectSena.getId()).setValue(dectSena);
                                }


                                if( con >0.8 && velocidadAuto > 70 && palabraRecog.equalsIgnoreCase("setenta")){

                                    ring.start();

                                    try {
                                        synchronized (this) {
                                            wait(50);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    imgViewAccidente.setVisibility(View.VISIBLE);
                                                    imgViewAlarma.setVisibility(View.VISIBLE);


                                                }
                                            });

                                        }

                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    detecSenaTransito dectSena=new detecSenaTransito();
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");


                                    Date date = new Date();
                                    String strDate = dateFormat.format(date).toString();

                                    Locale locale = new Locale("fr", "FR");
                                    DateFormat dateFormat1 = DateFormat.getTimeInstance(DateFormat.DEFAULT, locale);
                                    String time = dateFormat1.format(new Date());


                                    dectSena.setId(UUID.randomUUID().toString());
                                    dectSena.setIdConductor(idConductor);
                                    dectSena.setFechaDect(strDate);
                                    dectSena.setHora(time);
                                    dectSena.setPredicSena(con);
                                    dectSena.setSenaTransito(palabraRecog);
                                    databaseReference.child("SenaTransito").child(dectSena.getId()).setValue(dectSena);
                                }
                            }

                        }

                        requestRender();

                        computing = false;
                    }
                });

        Trace.endSection();
    }




    @Override
    public void onSetDebug(boolean debug) {
        classifier.enableStatLogging(debug);
    }

    private void renderDebug(final Canvas canvas) {
        if (!isDebug()) {
            return;
        }
        final Bitmap copy = cropCopyBitmap;
        if (copy != null) {
            final Matrix matrix = new Matrix();
            final float scaleFactor = 2;
            matrix.postScale(scaleFactor, scaleFactor);
            matrix.postTranslate(
                    canvas.getWidth() - copy.getWidth() * scaleFactor,
                    canvas.getHeight() - copy.getHeight() * scaleFactor);
            canvas.drawBitmap(copy, matrix, new Paint());

            final Vector<String> lines = new Vector<String>();
            if (classifier != null) {
                String statString = classifier.getStatString();
                String[] statLines = statString.split("\n");
                for (String line : statLines) {
                    lines.add(line);
                }
            }

            lines.add("Frame: " + previewWidth + "x" + previewHeight);
            lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
            lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
            lines.add("Rotation: " + sensorOrientation);
            lines.add("Inference time: " + lastProcessingTimeMs + "ms");

            borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
        }
    }



    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            CLocation myLocation = new CLocation(location,this.useMetricUnits());
            this.updateSpeed(myLocation);
            this.updateLocation(myLocation);
        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    @SuppressLint("MissingPermission")
    private void doStuff() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager != null){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        }
        Toast.makeText(this,"Waiting GPS Connection!", Toast.LENGTH_SHORT).show();
    }

    private void updateSpeed(CLocation location){
        float nCurrentSpeed =0;
        if(location!=null){
            location.setUserMetricUnits(this.useMetricUnits());
            nCurrentSpeed = location.getSpeed();
        }
        Formatter fmt = new Formatter(new StringBuilder());
        fmt.format(Locale.US,"%5.1f",nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();
        strCurrentSpeed =strCurrentSpeed.replace(" ","0");

        //String strUnits
        if(this.useMetricUnits()){
            textViewSpeed.setText(strCurrentSpeed);
        }else{

            velocidad =strCurrentSpeed;
            textViewSpeed.setText(strCurrentSpeed);
        }
    }

    private void updateLocation(CLocation location){

        double nLatitude =0,nLongitud =0;
        if(location!=null){
            nLatitude = location.getLatitude();
            nLongitud = location.getLongitude();
        }

        textViewLatitude.setText("Latitude: "+nLatitude);
        textViewLongitude.setText("Longitude: "+nLongitud);




    }
    private boolean useMetricUnits(){

        return switchMetric.isChecked();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode ==100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                this.doStuff();
            }else {
                finish();
            }
        }
    }
}
