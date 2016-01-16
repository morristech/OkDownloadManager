package hamed_gh.ir.okdonwloadmanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class DownloadService extends Service {
    private final HashMap<String, String> headers;
    String downloadUrl, fileName;
    LocalBroadcastManager mLocalBroadcastManager;
    ProgressBar progressBar;
    TextView textView;
    File sdCard = Environment.getExternalStorageDirectory();
    File dir = new File(sdCard.getAbsolutePath() + "/ir.hamed_gh.download/");
    double fileSize = 0;
    public static final int DOWNLOAD_CHUNK_SIZE = 2048; //Same as Okio Segment.SIZE

    DownloadAsyncTask dat;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public DownloadService(String url,
                           HashMap<String, String> headers,
                           Context c,
                           ProgressBar pBar,
                           TextView tv) {

        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.headers = headers;
        downloadUrl = url;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(c);
        progressBar = pBar;
        textView = tv;
        dat = new DownloadAsyncTask();
        dat.execute(new String[]{downloadUrl});
    }

    private boolean checkDirs() {
        if (!dir.exists()) {
            return dir.mkdirs();
        }
        return true;
    }

    private long isIncomplete() {
        File from = new File(dir, fileName + "-incomplete");
        if (from.exists()) {
            Log.d("status", "download is incomplete, filesize:" + from.length());
            return from.length();
        }
        return 0;
    }

    public void cancel() {
        dat.cancel(true);
    }

    public class DownloadAsyncTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);
            if (!checkDirs()) {
                return "Making directories failed!";
            }
            try {
                OkHttpClient client = new OkHttpClient();
                Request.Builder builder = new Request.Builder()
                        .url(downloadUrl)
                        .addHeader("token", "12-c5c856982961f5ec59cad075015409f17c2a1b99")
                        .addHeader("uuid", "1")
                        .addHeader("agent", "postman");

                for (String key : headers.keySet()) {
                    builder.addHeader(key, headers.get(key));
                }
                Request request = builder.build();
                Response response = client.newCall(request).execute();

                File f = new File(dir, "dummy-incomplete");

//                BufferedSink sink = Okio.buffer(Okio.sink(f));
//                sink.writeAll(response.body().source());
//                sink.close();

                ResponseBody body = response.body();
                long contentLength = body.contentLength();
                BufferedSource source = body.source();

                BufferedSink sink = Okio.buffer(Okio.sink(f));

                long bytesRead = 0;
                while (source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE) != -1) {
                    if(isCancelled()){
                        break;
                    }
                    bytesRead += DOWNLOAD_CHUNK_SIZE;
                    int progress = (int) ((bytesRead * 100) / contentLength);
                    publishProgress(progress);
                }
                sink.writeAll(source);
                sink.close();

            } catch (Exception e) {
                Log.e("Download Failed", "Error: " + e.getMessage());
            }
            if (isCancelled()) {
                return "Download cancelled!";
            }
            return "Download complete";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values[0]);
            if (progressBar != null) {
                progressBar.setProgress(values[0]);
            } else {
                Log.w("status", "ProgressBar is null, please supply one!");
            }

            if (textView != null){
                textView.setText("%" + values[0]);
            } else {
                Log.d("status", "Textview is null, please supply one!");
            }

        }

        @Override
        protected void onPreExecute() {
            mLocalBroadcastManager.sendBroadcast(new Intent("org.test.download.DOWNLOAD_STARTED"));
        }

        @Override
        protected void onPostExecute(String str) {
            File from = new File(dir, fileName + "-incomplete");
            File to = new File(dir, fileName);
            from.renameTo(to);
            mLocalBroadcastManager.sendBroadcast(new Intent("org.test.download.DOWNLOAD_FINISHED"));
        }

        @Override
        protected void onCancelled() {
            mLocalBroadcastManager.sendBroadcast(new Intent("org.test.download.DOWNLOAD_CANCELLED"));
        }
    }
}