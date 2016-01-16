package hamed_gh.ir.hameddonwloadmanager;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    private String urlString;
    private DownloadService ds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;
        final TextView tv = (TextView)findViewById(R.id.textview);
        final ProgressBar pb = (ProgressBar)findViewById(R.id.progressbar);
        urlString = "http://api.irangrammy.com/api/v4/en/music/11238-5e570e44ae82f0f77133aad4f7789078";
        HashMap<String,String> headers = new HashMap<>();
        headers.put("token","12-c5c856982961f5ec59cad075015409f17c2a1b99");
        headers.put("uuid","1");
        headers.put("agent", "postman");

        ds = new DownloadService(urlString,headers, ctx, pb);

    }
}