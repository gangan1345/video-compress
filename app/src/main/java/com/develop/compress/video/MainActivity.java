package com.develop.compress.video;


import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.develop.compress.VideoCompress;


public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static final int REQUEST_VIDEO_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_VIDEO_CODE);
                break;
        }
    }

    private void compress(final String videopath) {
        VideoCompress.compress(videopath, Environment.getExternalStorageDirectory().getPath() + "/compress.mp4", new VideoCompress.OnVideoCompressListener() {
            @Override
            public void onComplete(String path) {
                TextView textView = findViewById(R.id.textView2);
                textView.setText(videopath);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VIDEO_CODE) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                /** 数据库查询操作。
                 * 第一个参数 uri：为要查询的数据库+表的名称。
                 * 第二个参数 projection ： 要查询的列。
                 * 第三个参数 selection ： 查询的条件，相当于SQL where。
                 * 第三个参数 selectionArgs ： 查询条件的参数，相当于 ？。
                 * 第四个参数 sortOrder ： 结果排序。
                 */
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        // 视频ID:MediaStore.Audio.Media._ID
                        int videoId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                        // 视频名称：MediaStore.Audio.Media.TITLE
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                        // 视频路径：MediaStore.Audio.Media.DATA
                        String videoPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                        // 视频时长：MediaStore.Audio.Media.DURATION
                        int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                        // 视频大小：MediaStore.Audio.Media.SIZE
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

                        String text = String.format("videoPath = %s, duration = %s, size = %s, title = %s", videoPath, String.valueOf(duration), String.valueOf(size), title);
                        TextView textView = findViewById(R.id.textView);
                        textView.setText(text);

                        compress(videoPath);
                    }
                    cursor.close();
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
