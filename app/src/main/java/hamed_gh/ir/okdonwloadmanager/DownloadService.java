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
    File sdCard = Environment.getExternalStorageDirectory();
    File dir = new File(sdCard.getAbsolutePath() + "/ir.hamed_gh.download/");
    double fileSize = 0;
    public static final int DOWNLOAD_CHUNK_SIZE = 2048; //Same as Okio Segment.SIZE

    DownloadAsyncTask dat;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public DownloadService(String url, HashMap<String, String> headers, Context c, ProgressBar pBar) {
        if (!dir.exists()) {
            dir.mkdirs();
        }

        this.headers = headers;

        downloadUrl = url;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(c);
        progressBar = pBar;
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
                    builder.addHeader(key,headers.get(key));
                }
                Request request = builder.build();
                Response response = client.newCall(request).execute();


//                    InputStream is = response.body().byteStream();
//
//                    BufferedInputStream input = new BufferedInputStream(is);
//            FileOutputStream fos = new FileOutputStream(new File(dir,"dummy-incomplete"),true);

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
                    bytesRead += DOWNLOAD_CHUNK_SIZE;
                    int progress = (int) ((bytesRead * 100) / contentLength);
                    publishProgress(progress);
                }
                sink.writeAll(source);
                sink.close();


//					byte[] buffer = new byte[5000];
//			        int bufferLength = 0;
//			        int percentage = 0;
//			        double downloadedSize = 0;
//					URL url = new URL(downloadUrl);
//					HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
//			        urlConnection.setRequestMethod("GET");
//			        urlConnection.setConnectTimeout(10000);
//			        urlConnection.setReadTimeout(10000);
//			        Log.d("status","ReadTimeOut: "+urlConnection.getReadTimeout() + "ConnectTimeOut: "+urlConnection.getConnectTimeout());
//			        long downloaded = isIncomplete();
//			        if(downloaded > 0){
//			        	urlConnection.setRequestProperty("Range", "bytes="+(downloaded)+"-");
//			        	downloadedSize = downloaded;
//			        	fileSize = downloaded;
//			        }
//			        urlConnection.setDoOutput(true);
//			        urlConnection.connect();
//			        fileSize += urlConnection.getContentLength();
//			        FileOutputStream fos = new FileOutputStream(new File(dir,fileName+"-incomplete"),true);
//			        InputStream inputStream = urlConnection.getInputStream();
//			        while ( (bufferLength = inputStream.read(buffer)) > 0 )
//		            {
//			        	if(isCancelled()){
//			        		break;
//			        	}
//			            fos.write(buffer, 0, bufferLength);
//			            downloadedSize += bufferLength;
//			            percentage = (int) ((downloadedSize / fileSize) * 100);
//			            publishProgress(percentage);
//			            //Log.d("status","downloading: " + downloadedSize+"/"+fileSize+" ("+percentage+"%)");
//		            }
//			        fos.close();
//			        urlConnection.disconnect();
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
