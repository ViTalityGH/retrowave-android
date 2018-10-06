package com.senior.retrowavereborn.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.app.NotificationCompat.MediaStyle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;
import com.senior.retrowavereborn.MainActivity;
import com.senior.retrowavereborn.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import okhttp3.OkHttpClient;

final public class PlayerService extends Service {

    private final int NOTIFICATION_ID = 404;
    private final String NOTIFICATION_DEFAULT_CHANNEL_ID = "retro_channel";



    private final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

    private final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    );

    private MediaSessionCompat mediaSession;

    private AudioManager audioManager;

    public static SimpleExoPlayer exoPlayer;
    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    public static ArrayList<String> titles = new ArrayList<String>();
    public static ArrayList<String> urls = new ArrayList<String>();
    public static ArrayList<String> arts = new ArrayList<String>();

    public static int currentPosition = 0;
    public static int currentState = PlaybackStateCompat.STATE_STOPPED;

    public static Context appContext;
    public static Bitmap albumArt;

    public static boolean error = false;
    public static boolean loading = false;


    public static MainActivity mainActivity = new MainActivity();

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager
                    mNotificationManager =
                    (NotificationManager) getApplicationContext()
                            .getSystemService(Context.NOTIFICATION_SERVICE);
            // The id of the channel.
            String id = NOTIFICATION_DEFAULT_CHANNEL_ID;
            // The user-visible name of the channel.
            CharSequence name = "Media playback";
            // The user-visible description of the channel.
            String description = "Media playback controls";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
            // Configure the notification channel.
            mChannel.setDescription(description);
            mChannel.setShowBadge(false);
            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            mNotificationManager.createNotificationChannel(mChannel);

        }

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, "PlayerService");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(mediaSessionCallback);

        appContext = getApplicationContext();

        Intent activityIntent = new Intent(appContext, MainActivity.class);
        mediaSession.setSessionActivity(PendingIntent.getActivity(appContext, 0, activityIntent, 0));

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, appContext, MediaButtonReceiver.class);
        mediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(appContext, 0, mediaButtonIntent, 0));

        exoPlayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());
        exoPlayer.addListener(exoPlayerListener);
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(new OkHttpClient(), Util.getUserAgent(this, getString(R.string.app_name)), null);
        Cache cache = new SimpleCache(new File(this.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)); // 100 Mb max
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        this.extractorsFactory = new DefaultExtractorsFactory();


        new NextTrackParse().execute();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSession.release();
        exoPlayer.release();
    }


    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        //int currentState = PlaybackStateCompat.STATE_STOPPED;

        @Override
        public void onPlay() {
            if (!exoPlayer.getPlayWhenReady()) {


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(getApplicationContext(), PlayerService.class));
                }
                else {
                    startService(new Intent(getApplicationContext(), PlayerService.class));
                }


                //updateMetadataFromTrack();
                //prepareToPlay(Uri.parse("https://retrowave.ru" + urls.get(currentPosition)));

                int audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    return;

                mediaSession.setActive(true); // Сразу после получения фокуса

                registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

                exoPlayer.setPlayWhenReady(true);


            }


            mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PLAYING;

            try {
                refreshNotificationAndForegroundStatus(currentState);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            MainActivity.playBtn.setImageResource(android.R.drawable.ic_media_pause);

            MainActivity.paused = false;

        }

        @Override
        public void onPause() {

            try {
                if (exoPlayer.getPlayWhenReady()) {
                    exoPlayer.setPlayWhenReady(false);
                    unregisterReceiver(becomingNoisyReceiver);
                }
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
                currentState = PlaybackStateCompat.STATE_PAUSED;
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            MainActivity.paused = true;

            refreshNotificationAndForegroundStatus(currentState);

            MainActivity.playBtn.setImageResource(android.R.drawable.ic_media_play);


        }

        @Override
        public void onStop() {
            if (exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }

            audioManager.abandonAudioFocus(audioFocusChangeListener);

            mediaSession.setActive(false);

            mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_STOPPED;

            MainActivity.paused = true;
            MainActivity.playBtn.setImageResource(android.R.drawable.ic_media_play);

            refreshNotificationAndForegroundStatus(currentState);

            stopSelf();
        }

        @Override
        public void onSkipToNext() {

            MainActivity.interrupt = true;

            MainActivity.art.setImageResource(R.color.colorTransperent);
            MainActivity.trackName.setText("");


            if (error)
            {
                error = false;
                exoPlayer.stop();

                new NextTrackParse().execute();
            }
            else {

                if (!loading) {

                    currentPosition++;

                    if (currentPosition > titles.size() - 1)
                    {
                        currentPosition = 0;
                    }

                    exoPlayer.stop();

                    prepareToPlay(Uri.parse("http://retrowave.ru" + urls.get(currentPosition)));
                    getArt();

                }

            }


            MainActivity.artBlur.setAlpha(0.0f);

        }

        @Override
        public void onSkipToPrevious() {

            MainActivity.interrupt = true;

            MainActivity.art.setImageResource(R.color.colorTransperent);
            MainActivity.trackName.setText("");


            currentPosition--;

            exoPlayer.stop();

            if (currentPosition < 0)
            {
                currentPosition = 0;
            }


            if (!loading) {
                updateMetadataFromTrack();

                try {
                    updateMetadataFromTrack();
                    refreshNotificationAndForegroundStatus(currentState);
                    prepareToPlay(Uri.parse("http://retrowave.ru" + urls.get(currentPosition)));
                    getArt();
                } catch (Exception e) {
                    Toast.makeText(appContext, getText(R.string.error), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }


            MainActivity.artBlur.setAlpha(0.0f);

        }
    };

    private void updateMetadataFromTrack() {
        try {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, albumArt);
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, titles.get(currentPosition));
            //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "stayretro");
            //metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durations.get(currentPosition));
            //metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, "TEST");
            mediaSession.setMetadata(metadataBuilder.build());
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void prepareToPlay(Uri uri) {

        //Онлайн
        ExtractorMediaSource mediaSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
        //Оффлайн
        //ExtractorMediaSource mediaSource = new ExtractorMediaSource(Uri.parse(Environment.getExternalStorageDirectory().toString() + "/Retrowave Radio/Cache/" + com.senior.retrowavereborn.Cache.files[currentPosition].getName()), new DefaultDataSourceFactory(context, Util.getUserAgent(context, "ExoPlayerOffline"), null), Mp3Extractor.FACTORY, null, null);

//        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(Environment.getExternalStorageDirectory().toString() + "/retrocache/" + "123");
//
//        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//        String track = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
//
//        Log.e("MetaData", artist + " - " + track);
//
//        byte[] artBytes =  mmr.getEmbeddedPicture();
//        if(artBytes!=null)
//        {
//            Bitmap bm = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
//            MainActivity.art.setImageBitmap(bm);
//        }

        exoPlayer.prepare(mediaSource);


    }


    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    //mediaSessionCallback.onPlay(); // Не очень красиво
                    exoPlayer.setVolume(1.0f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    exoPlayer.setVolume(0.1f);
                    break;
                default:
                    mediaSessionCallback.onPause();
                    break;
            }
        }
    };

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Disconnecting headphones - stop playback
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSessionCallback.onPause();
            }
        }
    };

    private ExoPlayer.EventListener exoPlayerListener = new ExoPlayer.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            MainActivity.setTrackData();
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, getDuration());
            mediaSession.setMetadata(metadataBuilder.build());
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mediaSessionCallback.onSkipToNext();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Toast.makeText(getApplicationContext(), getText(R.string.error), Toast.LENGTH_LONG).show();

        }

        @Override
        public void onPositionDiscontinuity() {
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }

    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlayerServiceBinder();
    }

    public class PlayerServiceBinder extends Binder {
        public MediaSessionCompat.Token getMediaSessionToken() {
            return mediaSession.getSessionToken();
        }
    }

    private void refreshNotificationAndForegroundStatus(int playbackState) {
        switch (playbackState) {
            case PlaybackStateCompat.STATE_PLAYING: {
                startForeground(NOTIFICATION_ID, getNotification(playbackState));

                break;
            }
            case PlaybackStateCompat.STATE_PAUSED: {
                NotificationManagerCompat.from(PlayerService.this).notify(NOTIFICATION_ID, getNotification(playbackState));
                stopForeground(false);
                break;
            }
            default: {
                stopForeground(true);
                break;
            }
        }
    }

    private Notification getNotification(int playbackState) {

        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession);

        builder.addAction(new NotificationCompat.Action(R.drawable.exo_controls_previous, getText(R.string.previous), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(new NotificationCompat.Action(R.drawable.exo_controls_pause, getText(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(new NotificationCompat.Action(R.drawable.exo_controls_play, getText(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        builder.addAction(new NotificationCompat.Action(R.drawable.exo_controls_next, getText(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        builder.setStyle(new MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(mediaSession.getSessionToken())); // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.drawable.play); // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setOnlyAlertOnce(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID);
        }

        int red = 0;
        int green = 0;
        int blue = 0;

        try {
            int pixel = PlayerService.albumArt.getPixel(200, 200);

            red = Color.red(pixel) /2;
            green = Color.green(pixel) /2;
            blue = Color.blue(pixel) /2;


            if (pixel == Color.WHITE) {
                red = Color.red(0);
                green = Color.blue(0);
                blue = Color.green(0);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
        {
            builder.setColor(Color.rgb(red, green, blue));
        }


        return builder.build();
    }

    public Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {

            try {
                albumArt = bitmap;
                updateMetadataFromTrack();
                refreshNotificationAndForegroundStatus(currentState);


                MainActivity.art.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, 300, 150));
                MainActivity.setBlur(bitmap);


                MainActivity.interrupt = false;

                mainActivity.showAlphaBlur();


            } catch (Exception e)
            {
                e.printStackTrace();
            }

        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.color.colorTransperent);
            updateMetadataFromTrack();
            refreshNotificationAndForegroundStatus(currentState);
            MainActivity.art.setImageResource(R.color.colorTransperent);
            MainActivity.setBlur(BitmapFactory.decodeResource(getResources(), R.color.colorTransperent));
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };

    public void getArt() {
        Picasso.with(appContext).load("http://retrowave.ru" + arts.get(currentPosition)).resize(300, 300).into(target);
    }

    //Парсинг JSON и получение ссылок на объекты
    public class NextTrackParse extends AsyncTask<Void, Void, String> {



        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJson = "";
        URL url = null;
        InputStream inputStream = null;
        StringBuffer buffer = null;
        int random_number;


        @Override
        protected  void onPreExecute()
        {
            random_number = 0 + (int) (Math.random() * 398);
            loading = true;

            try {
                if (MainActivity.settings.getString("sound", "false").toString().equalsIgnoreCase("true"))
                    MainActivity.mediaPlayer.start();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }


        @Override
        protected String doInBackground(Void... params) {
            // получаем данные с внешнего ресурса
            try {


                url = new URL("http://retrowave.ru/api/v1/tracks?cursor=1&limit=350");
                Log.e("URL", url.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                inputStream = urlConnection.getInputStream();
                buffer = new StringBuffer();

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                resultJson = buffer.toString();


            } catch (Exception e) {
                e.printStackTrace();
            }
            return "{\"tracks\":" + resultJson + "}";
        }

        @Override
        protected void onPostExecute(String strJson) {
            super.onPostExecute(strJson);


            try {

                JSONObject parentObject = new JSONObject(resultJson);
                JSONObject userDetails = parentObject.getJSONObject("body");

                loading = false;

                JSONArray arr = userDetails.getJSONArray("tracks");
                for (int i = 0; i < arr.length(); i++)
                {
                    String title = arr.getJSONObject(i).getString("title");
                    String streamUrl = arr.getJSONObject(i).getString("streamUrl");
                    String artworkUrl = arr.getJSONObject(i).getString("artworkUrl");

                    titles.add(title);
                    urls.add(streamUrl);
                    arts.add(artworkUrl);
                }


                updateMetadataFromTrack();

                refreshNotificationAndForegroundStatus(currentState);

                prepareToPlay(Uri.parse("http://retrowave.ru" + urls.get(currentPosition)));

                getArt();


            }
            catch (JSONException jse)
            {
                Toast.makeText(appContext, getText(R.string.error), Toast.LENGTH_LONG).show();
                error = true;
                jse.printStackTrace();
            }
            catch (IndexOutOfBoundsException ex)
            {

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }


    public static Long getDuration()
    {
        return exoPlayer.getDuration();
    }

    public static Long getCurrentTrackTime()
    {
        return exoPlayer.getCurrentPosition();
    }

}
