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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadService extends Service {
    private final HashMap<String, String> headers;
    String downloadUrl, fileName;
    LocalBroadcastManager mLocalBroadcastManager;
    ProgressBar progressBar;
    TextView textView;
    File sdCard = Environment.getExternalStorageDirectory();
    File dir;
    public static final int DOWNLOAD_CHUNK_SIZE = 2048; //Same as Okio Segment.SIZE
    DownloadAsyncTask dat;

    private String TAG = "DownloadService";

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public DownloadService(String url,
                           String path,
                           HashMap<String, String> headers,
                           Context c,
                           ProgressBar pBar,
                           TextView tv) {

        dir = new File(path);
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
        from = new File(dir,fileName);
        if (from.exists()){
            return -1;
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
                        .url(downloadUrl);

                for (String key : headers.keySet()) {
                    builder.addHeader(key, headers.get(key));
                }

                long downloaded = isIncomplete();
                if (downloaded > 0) {
                    builder.addHeader("Range", "bytes=" + (downloaded) + "-");
                } else if (downloaded == -1) {
                    return "Download compeleted.";
                }

                Log.d(TAG, "doInBackground: downloaded= " + downloaded);

                Request request = builder.build();
                Response response = client.newCall(request).execute();
                Log.i(TAG, "doInBackground: response.body=" + response.body().contentLength());

                ResponseBody body = response.body();
                long contentLength = body.contentLength() + downloaded;
                Log.d(TAG, "doInBackground: contentLength= " + contentLength);

                long bytesRead = downloaded;
                Log.d(TAG, "doInBackground: bytesRead= " + bytesRead);
                int bufferLength = 0;

                byte[] buffer = new byte[DOWNLOAD_CHUNK_SIZE];
                FileOutputStream fos = new FileOutputStream(dir.getAbsolutePath() + "/" + fileName + "-incomplete", true);
                InputStream inputStream = response.body().byteStream();

//                        response.getInputStream();
                while ((bufferLength = inputStream.read(buffer)) != -1) {
                    if (isCancelled()) {
                        break;
                    }
                    fos.write(buffer, 0, bufferLength);

                    bytesRead += bufferLength;
                    int progress = (int) ((bytesRead * 100) / contentLength);
                    Log.d(TAG, "doInBackground: bytesRead= " + bytesRead);
                    Log.d(TAG, "doInBackground: contentLength= " + contentLength);
                    Log.d(TAG, "doInBackground: progress= " + progress);

                    publishProgress(progress);

//                    downloadedSize += bufferLength;
//                    percentage = (int) ((downloadedSize / fileSize) * 100);
//                    publishProgress(percentage);
                    //Log.d("status","downloading: " + downloadedSize+"/"+fileSize+" ("+percentage+"%)");
                }
                fos.close();

//                BufferedSink sink = Okio.buffer(Okio.sink(f));
//                sink.writeAll(response.body().source());
//                sink.close();

//                bytesRead = 0;
//                while (source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE) != -1) {
//                    if(isCancelled()){
//                        break;
//                    }
//                    bytesRead += DOWNLOAD_CHUNK_SIZE;
//                    int progress = (int) ((bytesRead * 100) / contentLength);
//                    publishProgress(progress);
//                }
//                sink.writeAll(source);
//                sink.close();


//                File f = new File(dir, fileName + "-incomplete");
//                BufferedSource source = body.source();
//                BufferedSink sink;
//                if (f.exists()) {
//                    sink = Okio.buffer(Okio.appendingSink(f));
//                }else {
//                    sink = Okio.buffer(Okio.sink(f));
//                }
//                byte[] buf=new byte[1024];
//                while ((bufferLength = source.read(buf)) != -1) {
//                    if(isCancelled()){
//                        break;
//                    }
//                    bytesRead += bufferLength;
//                    sink.write(buf,0, bufferLength);
//
//                    sink.flush();
//                    int progress = (int) ((bytesRead * 100) / contentLength);
//                    Log.d(TAG, "doInBackground: bytesRead= " + bytesRead);
//                    Log.d(TAG, "doInBackground: contentLength= "+ contentLength);
//                    Log.d(TAG, "doInBackground: progress= "+ progress);
//
//                    publishProgress(progress);
//                }
//                sink.close();

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
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
                Log.w(TAG, "ProgressBar is null, please supply one!");
            }

            if (textView != null){
                textView.setText("%" + values[0]);
            } else {
                Log.d(TAG, "Textview is null, please supply one!");
            }
        }

        @Override
        protected void onPreExecute() {
            mLocalBroadcastManager.sendBroadcast(new Intent("ir.hamed_gh.download.DOWNLOAD_STARTED"));
        }

        @Override
        protected void onPostExecute(String str) {
            File from = new File(dir, fileName + "-incomplete");
            File to = new File(dir, fileName);
            from.renameTo(to);
            mLocalBroadcastManager.sendBroadcast(new Intent("ir.hamed_gh.download.DOWNLOAD_FINISHED"));
        }

        @Override
        protected void onCancelled() {
            mLocalBroadcastManager.sendBroadcast(new Intent("ir.hamed_gh.download.DOWNLOAD_CANCELLED"));
        }
    }
}