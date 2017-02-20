package org.ancode.simplezxing;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.ancode.libzxing.QrCodeActivity;


public class MainActivity extends AppCompatActivity {
    TextView result;

    Button btn;
    final int scanRequest = 0x101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                QrCodeActivity.launcher(MainActivity.this, "来扫一扫", getResources().getColor(R.color.colorPrimary), scanRequest);

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == scanRequest && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString("result");
            result.setText(scanResult);
            Log.v("MainActivity", "SCAN RESULT=" + scanResult);
        }
    }
}
