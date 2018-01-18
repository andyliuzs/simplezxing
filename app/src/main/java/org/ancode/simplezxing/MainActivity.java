package org.ancode.simplezxing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.qrcode.encoder.QRCode;

import org.ancode.libzxing.QrCodeActivity;
import org.ancode.libzxing.utils.BitMapUtils;
import org.ancode.libzxing.utils.QrUtils;


public class MainActivity extends AppCompatActivity {
    TextView result;

    Button btn;
    final int scanRequest = 0x101;
    EditText et_code;
    Button btnCreate;
    ImageView ivCreateResult;
    CheckBox cb_havelogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.result);
        btn = (Button) findViewById(R.id.btn);
        et_code = (EditText) findViewById(R.id.et_code);
        btnCreate = (Button) findViewById(R.id.btn_create);
        ivCreateResult = (ImageView) findViewById(R.id.iv_create_result);
        cb_havelogo = (CheckBox) findViewById(R.id.cb_havelogo);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                QrCodeActivity.launcher(MainActivity.this, "来扫一扫", getResources().getColor(R.color.colorPrimary), scanRequest);

            }
        });
        btnCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = et_code.getText().toString();
                if (TextUtils.isEmpty(str)) {
                    Toast.makeText(MainActivity.this, "需要生成的数据为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Bitmap bitResult = null;
                    if (cb_havelogo.isChecked()) {
                        bitResult = QrUtils.createQRCodeWithLogo(str, BitMapUtils.drawableToBitmap(MainActivity.this, R.mipmap.ic_launcher));

                    } else {
                        bitResult = QrUtils.createQRCode(str);
                    }
                    ivCreateResult.setImageBitmap(bitResult);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "生成失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();

                }
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
