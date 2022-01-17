package com.yidian.player.view.video.preview;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.Surface;

import com.yidian.player.YiDianApp;

import java.nio.ByteBuffer;

/**
 * 使用MediaExtractor抽帧，MediaCodec解码，然后渲染到Surface
 * Created by frank on 2019/11/16.
 */
public class HardwareDecode {

    private final static String TAG = HardwareDecode.class.getSimpleName();

    private final static long DEQUEUE_TIME = 10 * 1000;
    private final static int SLEEP_TIME = 10;

    private final static int RATIO_1080 = 1080;
    private final static int RATIO_480 = 480;
    private final static int RATIO_240 = 240;

    private Surface mSurface;

    private Uri mUri;

    private VideoDecodeThread videoDecodeThread;

    private OnDataCallback mCallback;

    public interface OnDataCallback {
        void onData(long duration);
    }

    private Context getContext() {
        return YiDianApp.application.getApplicationContext();
    }

    public HardwareDecode(Surface surface, Uri uri, OnDataCallback onDataCallback) {
        this.mSurface = surface;
        this.mUri = uri;
        this.mCallback = onDataCallback;
    }

    public void decode() {
        videoDecodeThread = new VideoDecodeThread();
        videoDecodeThread.start();
    }

    public void seekTo(long seekPosition) {
        if (videoDecodeThread != null && !videoDecodeThread.isInterrupted()) {
            videoDecodeThread.seekTo(seekPosition);
        }
    }

    public void setPreviewing(boolean previewing) {
        if (videoDecodeThread != null) {
            videoDecodeThread.setPreviewing(previewing);
        }
    }

    public void release() {
        if (videoDecodeThread != null && !videoDecodeThread.isInterrupted()) {
            videoDecodeThread.interrupt();
            videoDecodeThread.release();
            videoDecodeThread = null;
        }
    }

    private class VideoDecodeThread extends Thread {

        private MediaExtractor mediaExtractor;

        private MediaCodec mediaCodec;

        private boolean isPreviewing;

        private boolean isStartPreviewing;

        void setPreviewing(boolean previewing) {
            this.isPreviewing = previewing;
        }

        void seekTo(long seekPosition) {
            isStartPreviewing = true;
            try {
                if (mediaExtractor != null) {
                    mediaExtractor.seekTo(seekPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "seekTo error=" + e.toString());
            }
            isStartPreviewing = false;
        }

        void release() {
            try {
                if (mediaCodec != null) {
                    mediaCodec.stop();
                    mediaCodec.release();
                }
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "release error=" + e.toString());
            }
        }

        @Override
        public void run() {
            super.run();

            mediaExtractor = new MediaExtractor();
            MediaFormat mediaFormat = null;
            String mimeType = "";
            try {
                mediaExtractor.setDataSource(getContext(), mUri, null);
                for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                    mediaFormat = mediaExtractor.getTrackFormat(i);
                    mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                    if (mimeType != null && mimeType.startsWith("video/")) {
                        mediaExtractor.selectTrack(i);
                        break;
                    }
                }
                if (mediaFormat == null || mimeType == null) {
                    return;
                }
                int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                if (mCallback != null) {
                    mCallback.onData(duration);
                }
                Log.i(TAG, "width=" + width + "--height=" + height + "--duration==" + duration);

                //重新设置预览分辨率
                setPreviewRatio(mediaFormat);
                Log.i(TAG, "mediaFormat=" + mediaFormat.toString());

                //配置MediaCodec，并且start
                mediaCodec = MediaCodec.createDecoderByType(mimeType);
                mediaCodec.configure(mediaFormat, mSurface, null, 0);
                mediaCodec.start();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

                while (!isInterrupted()) {
                    if (!isPreviewing || !isStartPreviewing) {
                        SystemClock.sleep(SLEEP_TIME);
                        continue;
                    }

                    //从缓冲区取出一个缓冲块，如果当前无可用缓冲块，返回inputIndex<0
                    int inputIndex = mediaCodec.dequeueInputBuffer(DEQUEUE_TIME);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputIndex);
                        int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                        //入队列
                        if (sampleSize < 0) {
                            mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            mediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                        }
                    }

                    //出队列
                    int outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIME);
                    switch (outputIndex) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            Log.i(TAG, "output format changed...");
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            Log.i(TAG, "try again later...");
                            break;
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            Log.i(TAG, "22222");
                            break;
                        default:
                            //渲染到surface
                            mediaCodec.releaseOutputBuffer(outputIndex, true);
                            break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "setDataSource error=" + e.toString());
            }
        }

        /**
         * 根据原分辨率大小动态设置预览分辨率
         *
         * @param mediaFormat mediaFormat
         */
        private void setPreviewRatio(MediaFormat mediaFormat) {
            if (mediaFormat == null) {
                return;
            }
            int videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            int previewRatio;
            if (videoWidth >= RATIO_1080) {
                previewRatio = 10;
            } else if (videoWidth >= RATIO_480) {
                previewRatio = 6;
            } else if (videoWidth >= RATIO_240) {
                previewRatio = 4;
            } else {
                previewRatio = 1;
            }
            int previewWidth = videoWidth / previewRatio;
            int previewHeight = videoHeight / previewRatio;
            Log.e(TAG, "videoWidth=" + videoWidth + "--videoHeight=" + videoHeight
                    + "--previewWidth=" + previewWidth + "--previewHeight=" + previewHeight);
            mediaFormat.setInteger(MediaFormat.KEY_WIDTH, previewWidth);
            mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, previewHeight);
        }
    }

}