package hamed_gh.ir.okdonwloadmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    private String urlString;
    private DownloadService ds;
    private Button btnStart;
    private Button btnPause;
    private LocalBroadcastManager mLocalBroadcastManager;

    //In an Activity
    private String[] mFileList;
    private File mPath = new File(Environment.getExternalStorageDirectory() + "");// + "//yourdir//");
    private String mChosenFile;
    private static final String FTYPE = ".txt";
    private static final int DIALOG_LOAD_FILE = 1000;
    private String TAG = "MainActivity";
    private Button btnBrowse;
    private TextView precentTextview;
    private ProgressBar progressBar;
    private TextView downloadLinkTextView;
    private TextView pathTextview;
    String path;
    private int REQUEST_DIRECTORY;

    private void findViews(){
        btnStart = (Button) findViewById(R.id.btnStart);
        btnPause = (Button) findViewById(R.id.btnPause);
        btnBrowse = (Button) findViewById(R.id.browse_btn);

        precentTextview = (TextView) findViewById(R.id.precent_textview);
        pathTextview = (TextView) findViewById(R.id.path_textview);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        downloadLinkTextView = (TextView) findViewById(R.id.downloadLink_TextView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;
        findViews();
        path = Environment.getExternalStorageDirectory()+"";
        pathTextview.setText(path);

        btnBrowse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent chooserIntent = new Intent(MainActivity.this, DirectoryChooserActivity.class);

                final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                        .newDirectoryName("DirChooserSample")
                        .allowReadOnlyDirectory(true)
                        .allowNewDirectoryNameModification(true)
                        .build();

                chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);

                // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
                REQUEST_DIRECTORY = 0;
                startActivityForResult(chooserIntent, REQUEST_DIRECTORY);

//                loadFileList();
//                onCreateDialog();

//                File mPath = new File(path);// + "//DIR//");
//                FileDialog fileDialog = new FileDialog(MainActivity.this, mPath);
////                fileDialog.setFileEndsWith(".txt");
//                fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
//                    public void fileSelected(File file) {
//                        Log.d(getClass().getName(), "selected file " + file.toString());
//                    }
//                });

                //fileDialog.addDirectoryListener(new FileDialog.DirectorySelectedListener() {
                //  public void directorySelected(File directory) {
                //      Log.d(getClass().getName(), "selected dir " + directory.toString());
                //  }
                //});
                //fileDialog.setSelectDirectoryOption(false);
//                fileDialog.showDialog();
            }
        });

//        urlString = "http://api.irangrammy.com/api/v4/en/music/11238-5e570e44ae82f0f77133aad4f7789078";
        urlString = "http://bmxmuseum.com/image/11212789_10152796929931876_1411937103111212490_n557d66a73f_blowup.jpg";
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("token", "12-c5c856982961f5ec59cad075015409f17c2a1b99");
        headers.put("uuid", "1");
        headers.put("agent", "postman");

        downloadLinkTextView.setText(urlString);

        btnStart.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                ds = new DownloadService(urlString, path, headers, ctx, progressBar, precentTextview);
                btnPause.setEnabled(true);
            }
        });
        btnPause.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                ds.cancel();
                btnPause.setEnabled(false);
                btnStart.setEnabled(true);
            }
        });

        /*******************************************/
        /*				Broadcast Receiver		   */
        /*******************************************/
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction("ir.hamed_gh.download.DOWNLOAD_STARTED");
        filter.addAction("ir.hamed_gh.download.DOWNLOAD_FINISHED");
        filter.addAction("ir.hamed_gh.download.DOWNLOAD_CANCELLED");
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("ir.hamed_gh.download.DOWNLOAD_STARTED")) {
                    Toast.makeText(ctx, "Download started!", Toast.LENGTH_SHORT).show();
//                    progressBar.setVisibility(View.VISIBLE);
                    btnStart.setEnabled(false);
                }
                if (intent.getAction().equals("ir.hamed_gh.download.DOWNLOAD_FINISHED")) {
                    Toast.makeText(ctx, "Download finished!", Toast.LENGTH_SHORT).show();
                    Log.d("status", "Download finished");
//                    progressBar.setVisibility(View.INVISIBLE);
                }
                if (intent.getAction().equals("ir.hamed_gh.download.DOWNLOAD_CANCELLED")) {
                    Toast.makeText(ctx, "Download cancelled!", Toast.LENGTH_SHORT).show();
                    Log.d("status", "Download cancelled");
//                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        };
        mLocalBroadcastManager.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                path = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                pathTextview.setText(path);
            } else {
                // Nothing selected
            }
        }
    }
}