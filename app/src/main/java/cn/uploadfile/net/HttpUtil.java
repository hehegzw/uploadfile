package cn.uploadfile.net;

import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gzw on 2016/4/15.
 */
public class HttpUtil {
    private static ExecutorService executorService;
    private static HttpUtil httpUtil;

    private HttpUtil() {
    }

    ;

    public static HttpUtil getInstence() {
        if (httpUtil == null) {
            return new HttpUtil();
        } else {
            return httpUtil;
        }
    }

    public static void post(String path, Map<String, String> params, Map<String, File> files, Success success, Failure failure) {
        DataOutputStream outStream = null;
        HttpURLConnection conn = null;
        try {
            String BOUNDARY = "---------" + UUID.randomUUID().toString();//数据分隔线
            String PREFIX = "--";
            String LINEND = "\r\n";
            String MULTIPART_FORM_DATA = "multipart/from_data";
            String CHARSET = "utf-8";

            URL url = new URL(path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5 * 1000);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("Charset", CHARSET);
            conn.setRequestProperty("Content-type", MULTIPART_FORM_DATA + ";boundary=" + BOUNDARY);

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(PREFIX);
                sb.append(BOUNDARY);
                sb.append(LINEND);
                sb.append("Content-Disposition: form-data; name=\""
                        + entry.getKey() + "\"" + LINEND);
                sb.append("Content-Type: text/plain; charset=" + CHARSET + LINEND);
                sb.append("Content-Transfer-Encoding: 8bit" + LINEND);
                sb.append(LINEND);
                sb.append(entry.getValue());
                sb.append(LINEND);
            }
            outStream = new DataOutputStream(
                    conn.getOutputStream());
            outStream.write(sb.toString().getBytes());
            // 发送文件数据
            if (files != null)
                for (Map.Entry<String, File> file : files.entrySet()) {
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(PREFIX);
                    sb1.append(BOUNDARY);
                    sb1.append(LINEND);
                    sb1.append("Content-Disposition: form-data; name=\"file\"; filename=\""
                            + file.getKey() + "\"" + LINEND);
                    sb1.append("Content-Type: multipart/form-data; charset="
                            + CHARSET + LINEND);
                    sb1.append(LINEND);
                    outStream.write(sb1.toString().getBytes());
                    InputStream is = new FileInputStream(file.getValue());
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = is.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                    }
                    is.close();
                    outStream.write(LINEND.getBytes());
                }
            // 请求结束标志
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
            outStream.write(end_data);
            outStream.flush();
            // 得到响应码
            int resCode = conn.getResponseCode();
            if (resCode != 200) {
                if (failure != null) {
                    failure.failure(resCode + "");
                }
            } else {
                InputStream in = conn.getInputStream();
                InputStreamReader isReader = new InputStreamReader(in);
                BufferedReader bufReader = new BufferedReader(isReader);
                String line = null;
                String data = "getResult=";
                while ((line = bufReader.readLine()) != null)
                    data += line;
                if (success != null) {
                    success.success(data);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void uploadFile(String path, Map<String, String> params, Map<String, File> files, Success success, Failure failure) {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        executorService.execute(new FileThread(path, params, files, success, failure));

    }
    public void download(final String path,final String fileName, final Success success, final Failure failure){
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                downloadFile(path,fileName,success,failure);
            }
        });
    }

    private void downloadFile(String path,String fileName, Success success, Failure failure) {
        String dirName = "";
        if (Environment.getExternalStorageState().endsWith(android.os.Environment.MEDIA_MOUNTED)) {
            dirName = Environment.getExternalStorageDirectory() + "/MyDownload";
        } else {
            //sd不存在
            return;
        }
        File dir;
        File file;
        URL url;
        FileOutputStream out = null;
        InputStream in = null;
        try {
            url = new URL(path+"/"+fileName);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            int resCode = conn.getResponseCode();
            if (resCode == 200) {
                dir = new File(dirName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file = new File(dirName +"/"+fileName);
                if (!file.exists())
                    file.createNewFile();
                out = new FileOutputStream(file);
                int len = conn.getContentLength();
                in = conn.getInputStream();
                int lengs;
                byte[] bytes = new byte[1024];
                while ((lengs=in.read(bytes)) > 0)
                    out.write(bytes, 0, lengs);
                out.flush();
                if(success!=null){
                    success.success(file.getAbsolutePath());
                }
            }else{
                if(failure!=null){
                    failure.failure(resCode+"");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null)
                    out.close();
                if(in!=null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface Success {
        void success(String response);
    }

    public interface Failure {
        void failure(String error);
    }

    private class FileThread implements Runnable {
        private String path;
        private Map<String, String> params;
        private Map<String, File> files;
        private Success success;
        private Failure failure;

        public FileThread(String path, Map<String, String> params, Map<String, File> files, Success success, Failure failure) {
            this.path = path;
            this.params = params;
            this.files = files;
            this.success = success;
            this.failure = failure;
        }

        @Override
        public void run() {
            post(path, params, files, success, failure);
        }
    }
}
