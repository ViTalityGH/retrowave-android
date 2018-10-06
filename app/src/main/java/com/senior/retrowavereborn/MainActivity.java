package com.senior.retrowavereborn;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.github.pwittchen.swipe.library.Swipe;
import com.github.pwittchen.swipe.library.SwipeListener;

import com.senior.retrowavereborn.service.PlayerService;
import com.vansuita.gaussianblur.GaussianBlur;
import com.yarolegovich.slidingrootnav.SlidingRootNav;
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder;


import org.json.JSONObject;

import mehdi.sakout.fancybuttons.FancyButton;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    PlayerService.PlayerServiceBinder playerServiceBinder;
    public static MediaControllerCompat mediaController;
    static ServiceConnection mConnection;

    public static ImageView art;
    public static ImageView artBlur;
    public static ImageView logo;

    ImageView nextBtn;
    ImageView previousBtn;
    public static ImageView playBtn;

    public static TextView trackName;
    public static TextView startTime;
    public static TextView endTime;
    public static TextView slash;

    public static ImageView volumeDown;
    public static ImageView volumeUp;

    public static Handler handler;
    private static final int UPDATE_FREQUENCY = 500;

    public static AudioManager audioManager = null;
    public static SeekBar volume;
    public static int checkVolume;

    public static ImageView reelRight;
    public static ImageView reelLeft;
    public static boolean reeled = false;
    public static boolean stopEatBattery = false;

    public static boolean paused = true;

    public static float blurAlpha = 0.0f;
    public static boolean interrupt = false;


    static Timer timer;
    static SleepTimer timerTask;

    public static Context context;

    public static SharedPreferences settings;
    public static SharedPreferences.Editor editor;

    public SlidingRootNav slidingRootNav;
    public Toolbar toolbar;


    public static int timerMinutes = 15;
    public static Thread blurThread;

    public static FancyButton vkBtn;
    public static FancyButton siteBtn;
    public static FancyButton downloadBtn;
    public static FancyButton shareBtn;
    public static FancyButton timeSetBtn;
    public static ImageView colorLineMenu;
    public static FancyButton supportBtn;
    public static FancyButton aboutBtn;
    public static ImageView timePicture;
    public static TextView timeTxt;
    public static TextView funTxt;
    public static Switch wheelSwitch;
    public static Switch startupSound;
    public static Swipe swipe;
    public static MediaPlayer mediaPlayer;


    public static boolean touchVolume = false;


    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        swipe.dispatchTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if(Build.VERSION.SDK_INT < 21) {
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_main);

        mediaPlayer = MediaPlayer.create(this, R.raw.cassetteclap);

        context = getApplicationContext();

        art = (ImageView) findViewById(R.id.art);
        artBlur = (ImageView) findViewById(R.id.artBlur);
        logo = (ImageView) findViewById(R.id.logo);
        nextBtn = (ImageView) findViewById(R.id.nextBtn);
        previousBtn = (ImageView) findViewById(R.id.previousBtn);
        playBtn = (ImageView) findViewById(R.id.playBtn);

        volumeDown = (ImageView) findViewById(R.id.volumeDown);
        volumeUp = (ImageView) findViewById(R.id.volumeUp);
        volume = (SeekBar) findViewById(R.id.volumeSeekbar);

        trackName = (TextView) findViewById(R.id.trackName);
        trackName.setSelected(true);

        startTime = (TextView) findViewById(R.id.startTime);
        endTime = (TextView) findViewById(R.id.endTime);
        slash = (TextView) findViewById(R.id.slash);

        handler = new Handler();

        reelRight = (ImageView) findViewById(R.id.reelRight);
        reelLeft = (ImageView) findViewById(R.id.reelLeft);


        settings = getSharedPreferences("UserInfo", 0);
        editor = settings.edit();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);




        //Установка шрифта
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Newtown_Italic.ttf");
        trackName.setTypeface(typeface);
        startTime.setTypeface(typeface);
        endTime.setTypeface(typeface);
        slash.setTypeface(typeface);


        if (stopEatBattery)
        {
            stopEatBattery = false;
            reeled = false;
        }

        if (!reeled) {
            spinRightReel();
            spinLeftReel();
        }

        initializeControl();
        initializeVolume();

    }


    public void initializeControl()
    {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {


                playerServiceBinder = (PlayerService.PlayerServiceBinder) service;

                try {
                    mediaController = new MediaControllerCompat(MainActivity.this, playerServiceBinder.getMediaSessionToken());
                    mediaController.registerCallback(new MediaControllerCompat.Callback() {
                        @Override
                        public void onPlaybackStateChanged(PlaybackStateCompat state) {
                            if (state == null)
                                return;
                        }
                    });

                }
                catch (RemoteException e) {
                    mediaController = null;
                }

            }

            public void onServiceDisconnected(ComponentName className) {

                playerServiceBinder = null;
                mediaController = null;
            }

        };


        bindService(new Intent(this, PlayerService.class), mConnection, Context.BIND_AUTO_CREATE);

        art.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showArtDialog();
                return false;
            }
        });

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (PlayerService.currentState == PlaybackStateCompat.STATE_PAUSED || PlayerService.currentState == PlaybackStateCompat.STATE_STOPPED)
                {
                    if (!PlayerService.loading) {
                        playBtn.setImageResource(android.R.drawable.ic_media_pause);
                        if (mediaController != null)
                            mediaController.getTransportControls().play();
                    }
                }
                else
                {
                    playBtn.setImageResource(android.R.drawable.ic_media_play);
                    if (mediaController != null)
                        mediaController.getTransportControls().pause();
                }

                try {
                    mediaPlayer.stop();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                interrupt = true;


                if (mediaController != null)
                    mediaController.getTransportControls().skipToNext();

            }
        });

        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                interrupt = true;

                if (mediaController != null)
                    mediaController.getTransportControls().skipToPrevious();

            }
        });

        //Копирование названия трека
        trackName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (trackName.getText() == "") {

                    } else {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("label", PlayerService.titles.get(PlayerService.currentPosition));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getApplicationContext(), getText(R.string.copytobuffer), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        //Mute sound
        volumeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                volume.setProgress(0);
            }
        });




        slidingRootNav = new SlidingRootNavBuilder(this)
                .withDragDistance(270) //Horizontal translation of a view. Default == 180dp
                .withRootViewScale(1.0f) //Content view's scale will be interpolated between 1f and 0.7f. Default == 0.65f;
                .withRootViewElevation(10) //Content view's elevation will be interpolated between 0 and 10dp. Default == 8.
                .withRootViewYTranslation(0) //Content view's translationY will be interpolated between 0 and 4. Default == 0
                .withMenuLayout(R.layout.menu_left_drawer)
                .withToolbarMenuToggle(toolbar)
                .withMenuOpened(false)
                .inject();


        timeSetBtn = (FancyButton) findViewById(R.id.timeSetBtn);
        siteBtn = (FancyButton) findViewById(R.id.siteBtn);
        vkBtn = (FancyButton) findViewById(R.id.vkBtn);
        shareBtn = (FancyButton) findViewById(R.id.shareBtn);
        downloadBtn = (FancyButton) findViewById(R.id.downloadBtn);
        colorLineMenu = (ImageView) findViewById(R.id.colorLineMenu);
        supportBtn = (FancyButton) findViewById(R.id.supportBtn);
        aboutBtn = (FancyButton) findViewById(R.id.aboutBtn);
        timePicture = (ImageView) findViewById(R.id.timePicture);
        timeTxt = (TextView) findViewById(R.id.timeTxt);
        funTxt = (TextView) findViewById(R.id.funTxt);
        wheelSwitch = (Switch) findViewById(R.id.wheelSwitch);
        startupSound = (Switch) findViewById(R.id.soundSwitch);

        Date date = new Date();

        if (date.getHours() >= 6 && date.getHours() < 12)
        {
            timePicture.setImageResource(R.drawable.morning);
            timeTxt.setText(getText(R.string.goodmorning));
        }
        else if (date.getHours() >= 12 && date.getHours() < 18)
        {
            timePicture.setImageResource(R.drawable.day);
            timeTxt.setText(getText(R.string.goodday));
        }
        else if (date.getHours() >= 18 && date.getHours() < 24)
        {
            timePicture.setImageResource(R.drawable.evening);
            timeTxt.setText(getText(R.string.goodevening));
        }
        else
        {
            timePicture.setImageResource(R.drawable.night);
            timeTxt.setText(getText(R.string.goodnight));
        }


        if (settings.getString("wheel", "false").toString().equalsIgnoreCase("true") && !wheelSwitch.isChecked())
        {
            wheelSwitch.toggle();
        }

        if (settings.getString("sound", "false").toString().equalsIgnoreCase("true") && !startupSound.isChecked())
        {
            startupSound.toggle();
        }


        swipe = new Swipe();
        swipe.setListener(new SwipeListener() {
            @Override public void onSwipingLeft(final MotionEvent event) {

            }

            @Override public void onSwipedLeft(final MotionEvent event) {

                if (wheelSwitch.isChecked() && slidingRootNav.isMenuClosed() && slidingRootNav.getLayout().getDragProgress() == 0 && !touchVolume)
                {
                    if (mediaController != null)
                        mediaController.getTransportControls().skipToNext();
                }

            }

            @Override public void onSwipingRight(final MotionEvent event) {

            }

            @Override public void onSwipedRight(final MotionEvent event) {
                if (wheelSwitch.isChecked() && slidingRootNav.isMenuClosed() && slidingRootNav.getLayout().getDragProgress() == 0 && !touchVolume)
                {
                    if (mediaController != null)
                        mediaController.getTransportControls().skipToPrevious();
                }

            }

            @Override public void onSwipingUp(final MotionEvent event) {

            }

            @Override public void onSwipedUp(final MotionEvent event) {

            }

            @Override public void onSwipingDown(final MotionEvent event) {

            }

            @Override public void onSwipedDown(final MotionEvent event) {

            }
        });


        wheelSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (wheelSwitch.isChecked())
                {
                    editor.putString("wheel", "true");
                    Toast.makeText(getApplicationContext(), getString(R.string.swipeinfo), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    editor.putString("wheel", "false");
                }

                editor.commit();

            }
        });

        startupSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startupSound.isChecked())
                {
                    editor.putString("sound", "true");
                }
                else
                {
                    editor.putString("sound", "false");
                }

                editor.commit();
            }
        });


        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                slidingRootNav.closeMenu();

                try {
                    if (!PlayerService.loading) {
                        ShortURL.makeShortUrl("http://retrowave.ru" + PlayerService.urls.get(PlayerService.currentPosition), new ShortURL.ShortUrlListener() {
                            @Override
                            public void OnFinish(String url) {
                                try {

                                    if (url != null && 0 < url.length()) {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("text/plain");
                                        String shareBody = getText(R.string.listenthis) + PlayerService.titles.get(PlayerService.currentPosition) + "\n\n" + url;
                                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                                        startActivity(Intent.createChooser(shareIntent, getText(R.string.share)));

                                    } else {
                                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                        shareIntent.setType("text/plain");
                                        String shareBody = getText(R.string.listenthis) + PlayerService.titles.get(PlayerService.currentPosition) + "\n\n" + "http://retrowave.ru" + PlayerService.urls.get(PlayerService.currentPosition);
                                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                                        startActivity(Intent.createChooser(shareIntent, getText(R.string.share)));
                                    }
                                } catch (Exception e)
                                {
                                    Toast.makeText(getApplicationContext(), getText(R.string.error), Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }

                            }
                        });
                    }
                } catch (Exception e)
                {
                    Toast.makeText(getApplicationContext(), getText(R.string.error), Toast.LENGTH_LONG).show();
                }
            }
        });


        downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!PlayerService.loading) {
                    slidingRootNav.closeMenu();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        });


        timeSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                slidingRootNav.closeMenu();

                timerMinutes = 15;

                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    Toast.makeText(getApplicationContext(), getString(R.string.timerwasstop), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    showTimerDialog();
                }



            }

        });


        siteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingRootNav.closeMenu();
                try {
                    Intent site = new Intent(Intent.ACTION_VIEW, Uri.parse("https://retrowave.ru"));
                    startActivity(site);
                } catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });


        vkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingRootNav.closeMenu();
                try {
                    Intent vk = new Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/mine_fm"));
                    startActivity(vk);
                } catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        supportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingRootNav.closeMenu();
                try {
//                    Intent donate = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.donationalerts.ru/r/senior_junior"));
//                    startActivity(donate);
                    showDonateDialog();
                } catch (Exception e)
                {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        });


        aboutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                slidingRootNav.closeMenu();
                createInfoDialog();
            }
        });


        try {
            art.setImageBitmap(Bitmap.createBitmap(PlayerService.albumArt, 0, 0, 300, 150));
            setBlur(PlayerService.albumArt);
            setTrackData();
        } catch (Exception e)
        {

        }


    }

    //Подтверждение выхода из приложения
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(getText(R.string.exitdialog))
                .setCancelable(false)
                .setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        ExitActivity.exitApplication(context);

                    }
                })
                .setNegativeButton(getText(R.string.no), null)
                .show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                try {

                    DownloadManager mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

                    Uri downloadUri = Uri.parse("http://retrowave.ru/" + PlayerService.urls.get(PlayerService.currentPosition));
                    DownloadManager.Request request = new DownloadManager.Request(downloadUri);

                    request.setAllowedNetworkTypes(
                            DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setAllowedOverRoaming(false).setTitle(PlayerService.titles.get(PlayerService.currentPosition))
                            .setDescription(getText(R.string.loading))
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, PlayerService.titles.get(PlayerService.currentPosition) + ".mp3");



                    mgr.enqueue(request);


//                    final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
//                    downloadTask.execute("http://retrowave.ru/" + PlayerService.urls.get(PlayerService.currentPosition));



                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                return;
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        slidingRootNav.closeMenu(false);

        if (isFinishing()) {

            if (mediaController != null) {
                mediaController.getTransportControls().stop();
            }

            PlayerService.exoPlayer.stop();

            stopService(new Intent(this, PlayerService.class));

            try {
                unbindService(mConnection);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            System.exit(0);
            
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();


        if (PlayerService.currentState == PlaybackStateCompat.STATE_PLAYING)
        {
            playBtn.setImageResource(android.R.drawable.ic_media_pause);
        }

        if (stopEatBattery)
        {
            stopEatBattery = false;
            reeled = false;
        }

        if (!reeled) {
            spinRightReel();
            spinLeftReel();
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (isChangingConfigurations())
        {
            slidingRootNav.closeMenu(false);
        }

        stopEatBattery = true;
    }

    public static void setBlur(Bitmap bitmap)
    {
        try {
            artBlur.setImageBitmap(GaussianBlur.with(context).render(bitmap));
            artBlur.setAlpha(0.7f);
        } catch (Exception e)
        {
            artBlur.setImageResource(R.color.colorTransperent);
        }
    }


    public static void setTrackData()
    {
        try {



            String maxTime = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(PlayerService.getDuration()),
                    TimeUnit.MILLISECONDS.toSeconds(PlayerService.getDuration()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(PlayerService.getDuration())));

            if (PlayerService.getDuration() < 0)
            {
                startTime.setText("00:00");
                endTime.setText("00:00");
            }
            else {
                endTime.setText(maxTime);
                trackName.setText(PlayerService.titles.get(PlayerService.currentPosition));
            }

            updatePosition();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    //Поток для обновления времени трека
    public static final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
            //Обновление время трека
            try {
                String minTime = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(PlayerService.getCurrentTrackTime()),
                        TimeUnit.MILLISECONDS.toSeconds(PlayerService.getCurrentTrackTime()) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(PlayerService.getCurrentTrackTime())));
                startTime.setText(minTime);

                if (checkVolume != audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
                    volume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                }


            } catch (Exception e) {

            }
        }
    };


    //Обновления позиции
    public static void updatePosition() {

        try {
            handler.removeCallbacks(updatePositionRunnable);
            handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
        } catch (NullPointerException e)
        {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Громкость
    private void initializeVolume() {
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);


            volume.setMax(audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volume.setProgress(audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));


            volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                    touchVolume = false;
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                    touchVolume = true;
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                    checkVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void spinRightReel()
    {
        reeled = true;
        new Thread() {
            public void run() {
                while (!stopEatBattery)
                {

                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!paused) {
                                        reelRight.setRotation(reelRight.getRotation() - 2);
                                    }
                                }
                            });
                            Thread.sleep(10);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                }
            }
        }.start();
    }

    public void spinLeftReel()
    {
        reeled = true;
        new Thread() {
            public void run() {
                while (!stopEatBattery)
                {
                        try {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (!paused) {
                                        reelLeft.setRotation(reelLeft.getRotation() - 2);
                                    }
                                }
                            });
                            Thread.sleep(30);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                }
            }
        }.start();

    }


    public void createInfoDialog()
    {

        String version = "null";
        try {


            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;


        new MaterialStyledDialog.Builder(this)
                .setTitle("Retrowave Radio v." + version)
                .setDescription(getText(R.string.aboutinfo) + "\n")
                .setHeaderDrawable(R.drawable.photo)
                .setCancelable(true)
                .show();

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getText(R.string.error), Toast.LENGTH_SHORT).show();
        }

    }


    public void showTimerDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog);

        final TextView dialog_minutes = (TextView) dialog.findViewById(R.id.dialog_minutes);


        FancyButton dialog_up_btn = (FancyButton) dialog.findViewById(R.id.dialog_up_btn);
        dialog_up_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerMinutes < 120)
                {
                    timerMinutes = timerMinutes + 15;
                    dialog_minutes.setText(timerMinutes + "");
                }
            }
        });

        FancyButton dialog_down_btn = (FancyButton) dialog.findViewById(R.id.dialog_down_btn);
        dialog_down_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (timerMinutes > 15)
                {
                    timerMinutes = timerMinutes - 15;
                    dialog_minutes.setText(timerMinutes + "");
                }
            }
        });

        FancyButton dialog_start_btn = (FancyButton) dialog.findViewById(R.id.dialog_start_btn);
        dialog_start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = new Timer();
                timerTask = new SleepTimer();

                timer.schedule(timerTask, timerMinutes * 60000);

                dialog.dismiss();

                Toast.makeText(getApplicationContext(), getText(R.string.timersetinfo), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

    }


    public void showDonateDialog()
    {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_donate);

        final TextView btcWallet = (TextView) dialog.findViewById(R.id.btcWallet);
        btcWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", btcWallet.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), getText(R.string.copytobuffer), Toast.LENGTH_LONG).show();
            }
        });


        final TextView ethWallet = (TextView) dialog.findViewById(R.id.ethWallet);
        ethWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", ethWallet.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), getText(R.string.copytobuffer), Toast.LENGTH_LONG).show();
            }
        });

        FancyButton otherDonateBtn = (FancyButton) dialog.findViewById(R.id.otherDonateBtn);
        otherDonateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent donate = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.donationalerts.ru/r/senior_junior"));
                startActivity(donate);
            }
        });

        dialog.show();

    }

    public void showArtDialog()
    {
        try {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCancelable(true);
            dialog.setContentView(R.layout.dialog_art);

            ImageView dialogArt = (ImageView) dialog.findViewById(R.id.dialogArt);
            dialogArt.setImageBitmap(PlayerService.albumArt);

            dialog.show();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    class SleepTimer extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    ExitActivity.exitApplication(getApplicationContext());

                }
            });
        }
    }


    public void showAlphaBlur()
    {
        artBlur.setAlpha(0.0f);
        blurAlpha = 0.0f;

        blurThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!interrupt) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                artBlur.setAlpha(blurAlpha);
                                blurAlpha = blurAlpha + 0.05f;

                            }
                        });
                        Thread.sleep(50);

                        if (blurAlpha >= 0.7f)
                        {
                            break;
                        }

                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        });

        blurThread.start();

    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private NotificationManagerCompat notificationManager;
        private NotificationCompat.Builder mBuilder;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                File path = new File(Environment.getExternalStorageDirectory().toString() + "/Retrowave Radio/Cache");
                if (!path.exists()) {
                    path.mkdirs();
                }

                output = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/Retrowave Radio/Cache/" + PlayerService.titles.get(PlayerService.currentPosition));

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
            mWakeLock.acquire();

            notificationManager = NotificationManagerCompat.from(getApplicationContext());
            mBuilder = new NotificationCompat.Builder(getApplicationContext(), "Cache");
            mBuilder.setContentTitle("Picture Download")
                    .setContentText("Download in progress")
                    .setSmallIcon(R.drawable.play)
                    .setPriority(NotificationCompat.PRIORITY_LOW);

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            System.out.println(progress[0]);
            if (progress[0] == 0 || progress[0] == 25 || progress[0] == 50 || progress[0] == 75 || progress[0] == 100) {
                mBuilder.setProgress(100, progress[0], false);
                notificationManager.notify(333, mBuilder.build());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.e("Error", result);
            }
            else {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
                mBuilder.setContentText("Download complete");
                notificationManager.notify(333, mBuilder.build());
            }
        }
    }

}


