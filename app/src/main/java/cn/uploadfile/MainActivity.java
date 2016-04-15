package cn.uploadfile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cn.uploadfile.net.HttpUtil;
import cn.uploadfile.util.FilePathUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG= "MainActivity";
    private static final String UPLOAD_PATH= "http://192.168.0.107:8080/UploadFile/UploadFile";
    private static final String DOWN_PATH= "http://192.168.0.107:8080/UploadFile/upload";
    private static final int SELECT_PIC_KITKAT = 1;
    private static final int SELECT_PIC = 2;
    private Button selectFile;
    private Button submit;
    private Button download;
    private ImageView image;
    private TextView showFileName;
    private String filePath;
    private HttpUtil httpUtil;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 1){
                Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
            }else if(msg.what == 2){
                Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
            }else if(msg.what == 3){
                String filePath = (String) msg.obj;
                Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                image.setImageBitmap(bitmap);
            }else{
                Toast.makeText(MainActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        httpUtil = HttpUtil.getInstence();
        initView();
    }

    private void initView() {
        selectFile = (Button) findViewById(R.id.selectFile);
        submit = (Button) findViewById(R.id.submit);
        showFileName = (TextView) findViewById(R.id.showFileName);
        download = (Button) findViewById(R.id.download);
        image = (ImageView) findViewById(R.id.image);
        initEvent();
    }

    private void initEvent() {
        selectFile.setOnClickListener(this);
        submit.setOnClickListener(this);
        download.setOnClickListener(this);
    }
    //打开文件管理器
    private void showFileChooser() {
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);//ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/jpeg");
        if(android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.KITKAT){
            startActivityForResult(intent, SELECT_PIC_KITKAT);
        }else{
            startActivityForResult(intent, SELECT_PIC);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.selectFile:
                showFileChooser();
                break;
            case R.id.submit:
                String str = showFileName.getText().toString();
                if(TextUtils.isEmpty(str)){
                    Toast.makeText(MainActivity.this, "请选择一个图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                initData();
                break;
            case R.id.download:
                String str1 = showFileName.getText().toString();
                if(TextUtils.isEmpty(str1)){
                    Toast.makeText(MainActivity.this, "请先上传一个图片", Toast.LENGTH_SHORT).show();
                    return;
                }
                download(str1);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (data == null) return;
        if(requestCode == SELECT_PIC_KITKAT){
            Uri uri = data.getData();
            filePath = FilePathUtil.getPath(this, uri);
            Log.d(TAG,filePath);
        }else{
            Uri uri = data.getData();
            filePath = Uri.decode(data.getDataString());
            if(!filePath.toString().startsWith("file")){
                filePath = FilePathUtil.getDataColumn(this,uri,null,null);
            }else{
                filePath = filePath.toString().substring(7);
            }
        }
        //截取文件名
        String[] filenames = filePath.split("/");
        String filename = filenames[filenames.length - 1];
        showFileName.setText(filename);
    }

    private void initData() {
        Map<String,String> params = new HashMap<>();
        params.put("String","文字信息");
        Map<String,File> files = new HashMap<>();
        File file = new File(filePath);
        files.put(showFileName.getText().toString(),file);
        uploadFile(UPLOAD_PATH,params,files);
    }

    private void uploadFile(String path, Map<String,String> params, Map<String,File> files){
        httpUtil.uploadFile(path, params, files, new HttpUtil.Success() {
            @Override
            public void success(String response) {
                handler.sendEmptyMessage(1);
            }

        }, new HttpUtil.Failure() {
            @Override
            public void failure(String error) {
                handler.sendEmptyMessage(2);
            }
        });
    }
    private void download(String fileName){
        httpUtil.download(DOWN_PATH, fileName, new HttpUtil.Success() {
            @Override
            public void success(String response) {
                Message msg = new Message();
                msg.what = 3;
                msg.obj = response;
                handler.sendMessage(msg);
            }
        }, new HttpUtil.Failure() {
            @Override
            public void failure(String error) {
                handler.sendEmptyMessage(4);
            }
        });
    }
}
