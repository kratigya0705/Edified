package com.example.hppc.edified;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class DownloadService extends MyService {
    /**
     * Actions
     **/
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String DOWNLOAD_COMPLETED = "download_completed";
    public static final String DOWNLOAD_ERROR = "download_error";
    /**
     * Extras
     **/
    public static final String EXTRA_DOWNLOAD_PATH = "extra_download_path";
    public static final String EXTRA_BYTES_DOWNLOADED = "extra_bytes_downloaded";
    private static final String TAG = "Storage#DownloadService";
    private StorageReference mStorageRef;

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(DOWNLOAD_COMPLETED);
        filter.addAction(DOWNLOAD_ERROR);

        return filter;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Storage
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            // Get the path to download from the intent
            String downloadPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH);
            downloadFromPath(downloadPath);
        }

        return START_REDELIVER_INTENT;
    }

    private void downloadFromPath(final String downloadPath) {
        Log.d(TAG, "downloadFromPath:" + downloadPath);
        mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl(downloadPath);

        File rootPath = new File(Environment.getExternalStorageDirectory(), "edified");
        if (!rootPath.exists()) {
            rootPath.mkdirs();
        }

        final File localFile = new File(rootPath, "book.pdf");

        // Mark task started
        taskStarted();
        showProgressNotification(getString(R.string.progress_downloading), 0, 0);

        mStorageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @SuppressWarnings("VisibleForTests")
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "download:SUCCESS");

                // Send success broadcast with number of bytes downloaded
                broadcastDownloadFinished(downloadPath, taskSnapshot.getTotalByteCount());
                showDownloadFinishedNotification(downloadPath, (int) taskSnapshot.getTotalByteCount());

                // Mark task completed
                taskCompleted();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "download:FAILURE", e);

                // Send failure broadcast
                broadcastDownloadFinished(downloadPath, -1);
                showDownloadFinishedNotification(downloadPath, -1);

                // Mark task completed
                taskCompleted();
            }
        });

//        // Download and get total bytes
//        mStorageRef.child(downloadPath).getStream(
//                new StreamDownloadTask.StreamProcessor() {
//                    @SuppressWarnings("VisibleForTests")
//                    @Override
//                    public void doInBackground(StreamDownloadTask.TaskSnapshot taskSnapshot,
//                                               InputStream inputStream) throws IOException {
//                        long totalBytes = taskSnapshot.getTotalByteCount();
//                        long bytesDownloaded = 0;
//
//                        byte[] buffer = new byte[1024];
//                        int size;
//
//                        while ((size = inputStream.read(buffer)) != -1) {
//                            bytesDownloaded += size;
//                            showProgressNotification(getString(R.string.progress_downloading),
//                                    bytesDownloaded, totalBytes);
//                        }
//
//                        // Close the stream at the end of the Task
//                        inputStream.close();
//                    }
//                })
//                .addOnSuccessListener(new OnSuccessListener<StreamDownloadTask.TaskSnapshot>() {
//                    @SuppressWarnings("VisibleForTests")
//                    @Override
//                    public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
//                        Log.d(TAG, "download:SUCCESS");
//
//                        // Send success broadcast with number of bytes downloaded
//                        broadcastDownloadFinished(downloadPath, taskSnapshot.getTotalByteCount());
//                        showDownloadFinishedNotification(downloadPath, (int) taskSnapshot.getTotalByteCount());
//
//                        // Mark task completed
//                        taskCompleted();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        Log.w(TAG, "download:FAILURE", exception);
//
//                        // Send failure broadcast
//                        broadcastDownloadFinished(downloadPath, -1);
//                        showDownloadFinishedNotification(downloadPath, -1);
//
//                        // Mark task completed
//                        taskCompleted();
//                    }
//                });
    }

    /**
     * Broadcast finished download (success or failure).
     *
     * @return true if a running receiver received the broadcast.
     */
    private boolean broadcastDownloadFinished(String downloadPath, long bytesDownloaded) {
        boolean success = bytesDownloaded != -1;
        String action = success ? DOWNLOAD_COMPLETED : DOWNLOAD_ERROR;

        Intent broadcast = new Intent(action)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded);
        return LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(broadcast);
    }

    /**
     * Show a notification for a finished download.
     */
    private void showDownloadFinishedNotification(String downloadPath, int bytesDownloaded) {
        // Hide the progress notification
        dismissProgressNotification();

        // Make Intent to MainActivity
        Intent intent = new Intent(this, MainActivity.class)
                .putExtra(EXTRA_DOWNLOAD_PATH, downloadPath)
                .putExtra(EXTRA_BYTES_DOWNLOADED, bytesDownloaded)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        boolean success = bytesDownloaded != -1;
        String caption = success ? getString(R.string.download_success) : getString(R.string.download_failure);
        showFinishedNotification(caption, intent, true);
    }
}
