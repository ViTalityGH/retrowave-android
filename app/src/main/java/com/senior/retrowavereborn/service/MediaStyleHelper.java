package com.senior.retrowavereborn.service;



import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import com.senior.retrowavereborn.MainActivity;
import com.senior.retrowavereborn.R;

/**
 * Helper APIs for constructing MediaStyle notifications
 */
class MediaStyleHelper {
    /**
     * Build a notification using the information from the given media session. Makes heavy use
     * of {@link MediaMetadataCompat#getDescription()} to extract the appropriate information.
     *
     * @param context      Context used to construct the notification.
     * @param mediaSession Media session to get information.
     * @return A pre-built notification with information from the given media session.
     */
    static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        //MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        try {

            String[] ta = PlayerService.titles.get(PlayerService.currentPosition).split("–");
            builder.setContentTitle(ta[1].trim());
            builder.setContentText(ta[0].trim());
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        builder.setLargeIcon(PlayerService.albumArt);
        builder.setContentIntent(controller.getSessionActivity());
        builder.setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP));
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        return builder;

    }

}