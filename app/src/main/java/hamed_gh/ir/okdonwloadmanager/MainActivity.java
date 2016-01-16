package hamed_gh.ir.okdonwloadmanager;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    private String urlString;
    private DownloadService ds;
    private Button btnStart;
    private Button btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;
        final TextView tv = (TextView)findViewById(R.id.textview);
        final ProgressBar pb = (ProgressBar)findViewById(R.id.progressbar);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnPause = (Button) findViewById(R.id.btnPause);

        urlString = "http://api.irangrammy.com/api/v4/en/music/11238-5e570e44ae82f0f77133aad4f7789078";
        final HashMap<String,String> headers = new HashMap<>();
        headers.put("token","12-c5c856982961f5ec59cad075015409f17c2a1b99");
        headers.put("uuid","1");
        headers.put("agent", "postman");

        btnStart.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                ds = new DownloadService(urlString,headers, ctx, pb,tv);
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

    }
}