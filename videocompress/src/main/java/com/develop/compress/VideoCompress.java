package com.develop.compress;

import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import com.coremedia.iso.boxes.Container;
import com.develop.compress.video.MediaController;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Angus
 */
public class VideoCompress {
    static VideoCompressTask mVideoCompressTask;

    /**
     * compress
     *
     * @param videoPath
     * @param compressDir
     * @param listener
     */
    public static void compress(String videoPath, String compressDir, OnVideoCompressListener listener) {
        long size = getFileSize(videoPath);
        // sdk 16 以上启用压缩
        if (Build.VERSION.SDK_INT >= 16 && size > (1024 * 1024 * 3)) {//视频大于3M才压缩
            if (checkMinBitrate(videoPath, size)) {
                if (listener != null) {
                    listener.onComplete(videoPath);
                }
            } else {
                mVideoCompressTask = new VideoCompressTask(videoPath, compressDir, listener);
                mVideoCompressTask.execute();
            }
        } else {
            if (listener != null) {
                listener.onComplete(videoPath);
            }
        }
    }


    /**
     * cancel
     */
    public void cancelCompress() {
        try {
            if (mVideoCompressTask != null && mVideoCompressTask.getStatus() != AsyncTask.Status.FINISHED) {
                mVideoCompressTask.cancel(true);
            }
            MediaController.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 过滤掉码率低的视频不压缩， 否则会有些手机把码率低的视频压出来比较模糊
     * 暂时过滤// TODO
     *
     * @param filePath
     * @return
     */
    private static boolean checkMinBitrate(String filePath, long size) {
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(filePath);
            String bit = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            int bitValue = bit == null ? 0 : Integer.valueOf(bit);
            if (bitValue > 0 && bitValue < 150000 && size < (1024 * 1024 * 20)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public interface OnVideoCompressListener {
        void onComplete(String path);
    }


    /**
     * 视频压缩
     */
    static class VideoCompressTask extends AsyncTask<Void, Void, Boolean> {
        String videoPath, compressDir, compressPath;
        OnVideoCompressListener listener;

        public VideoCompressTask(String videoPath, String compressDir, OnVideoCompressListener listener) {
            super();
            this.videoPath = videoPath;
            this.compressDir = compressDir;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            deleteDirectory(compressDir, false, false);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            File file = new File(compressDir);
            if (!file.exists()) {
                file.mkdirs();
            }
            File cacheFile = new File(compressDir, "VIDEO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4");
            compressPath = cacheFile.getAbsolutePath();
            boolean isConverted = MediaController.getInstance().convertVideo(videoPath, cacheFile);

            if (isCancelled()) {
                if (listener != null) {
                    listener.onComplete(videoPath);
                }
                return false;
            }

            if (isConverted) {
                // 压缩成功将视频再处理以解决不能在线播放的问题
                File tempFile = new File(compressDir, "VD_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4");
                try {
                    Movie movie = MovieCreator.build(cacheFile.getAbsolutePath());
                    Container out = new DefaultMp4Builder().build(movie);

                    FileChannel fc = new FileOutputStream(tempFile).getChannel();
                    out.writeContainer(fc);
                    fc.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    compressPath = null;
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    compressPath = tempFile.getAbsolutePath();
                    deleteFile(cacheFile.getAbsolutePath());
                } else {
                    deleteFile(tempFile.getAbsolutePath());
                }
            } else {
                // 压缩失败上传原视频
                compressPath = null;
            }
            return isConverted;

        }

        @Override
        protected void onPostExecute(Boolean compressed) {
            super.onPostExecute(compressed);
            if (!isCancelled()) {
                if (listener != null) {
                    listener.onComplete(!TextUtils.isEmpty(compressPath) ? compressPath : videoPath);
                }
            }
        }
    }

    private static long getFileSize(String path) {
        if (TextUtils.isEmpty(path)) {
            return -1;
        }

        File file = new File(path);
        return (file.exists() && file.isFile() ? file.length() : -1);
    }

    private static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
            return true;
        }
        return false;
    }

    private static boolean deleteDirectory(String filePath, boolean delDir, boolean onlyFile) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        // 如果sPath不以文件分隔符结尾，自动添加文件分隔符

        if (!filePath.endsWith(File.separator)) {
            filePath = filePath + File.separator;
        }
        File dirFile = new File(filePath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        // 删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } // 删除子目录
            else {
                if (!onlyFile) {
                    flag = deleteDirectory(files[i].getAbsolutePath(), delDir, false);
                    if (!flag) {
                        break;
                    }
                }
            }
        }
        if (!flag) {
            return false;
        }

        if (delDir) {
            // 删除当前目录
            if (dirFile.delete()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
