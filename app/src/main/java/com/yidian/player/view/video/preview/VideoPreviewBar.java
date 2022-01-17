package com.yidian.player.view.video.preview;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ScreenUtils;
import com.yidian.player.R;
import com.yidian.player.utils.VideoUtils;

/**
 * 视频拖动实时预览的控件
 * Created by frank on 2019/11/16.
 */
public class VideoPreviewBar extends RelativeLayout implements HardwareDecode.OnDataCallback {

    private final static String TAG = VideoPreviewBar.class.getSimpleName();

    private TextureView texturePreView;

    private SeekBar previewBar;

    private TextView txtVideoProgress;

    private TextView txtVideoDuration;

    private HardwareDecode hardwareDecode;

    private PreviewBarCallback mPreviewBarCallback;

    private int duration;

    private int screenWidth;

    private int moveEndPos = 0;

    private int previewHalfWidth;

    public VideoPreviewBar(Context context) {
        super(context);
        initView(context);
    }

    public VideoPreviewBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.preview_video, this);
        previewBar = view.findViewById(R.id.preview_bar);
        texturePreView = view.findViewById(R.id.texture_preview);
        txtVideoProgress = view.findViewById(R.id.txt_video_progress);
        txtVideoDuration = view.findViewById(R.id.txt_video_duration);
        setListener();
        screenWidth = ScreenUtils.getScreenWidth();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (moveEndPos == 0) {
            int previewWidth = texturePreView.getWidth();
            previewHalfWidth = previewWidth / 2;
            int marginEnd = 0;
            MarginLayoutParams layoutParams = (MarginLayoutParams) texturePreView.getLayoutParams();
            if (layoutParams != null) {
                marginEnd = layoutParams.getMarginEnd();
            }
            moveEndPos = screenWidth - previewWidth - marginEnd;
            Log.i(TAG, "previewWidth=" + previewWidth);
        }
    }

    private void setPreviewCallback(final Uri uri, TextureView texturePreView) {
        texturePreView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                doPreview(uri, new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void doPreview(Uri uri, Surface surface) {
        if (uri == null || surface == null) {
            return;
        }
        release();
        hardwareDecode = new HardwareDecode(surface, uri, this);
        hardwareDecode.decode();
    }

    private void setListener() {
        previewBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                //previewBar.setProgress(progress);
                if (hardwareDecode != null && progress < duration) {
                    // us to ms
                    hardwareDecode.seekTo(progress * 1000);
                }
                int percent = progress * screenWidth / duration;
                if (percent > previewHalfWidth && percent < moveEndPos && texturePreView != null) {
                    texturePreView.setTranslationX(percent - previewHalfWidth);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (texturePreView != null) {
                    texturePreView.setVisibility(VISIBLE);
                }
                if (hardwareDecode != null) {
                    hardwareDecode.setPreviewing(true);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (texturePreView != null) {
                    texturePreView.setVisibility(GONE);
                }
                if (mPreviewBarCallback != null) {
                    mPreviewBarCallback.onStopTracking(seekBar.getProgress());
                }
                if (hardwareDecode != null) {
                    hardwareDecode.setPreviewing(false);
                }
            }
        });
    }

    @Override
    public void onData(long duration) {
        //us to ms
        final int durationMs = (int) (duration / 1000);
        Log.i(TAG, "duration=" + duration);
        this.duration = durationMs;
        post(new Runnable() {
            @Override
            public void run() {
                previewBar.setMax(durationMs);
                txtVideoDuration.setText(getVideoTime(durationMs));
                texturePreView.setVisibility(GONE);
            }
        });
    }

    private String getVideoTime(long duration) {
        return VideoUtils.INSTANCE.formatTimeMillis(duration);
    }

    public void init(Uri uri, PreviewBarCallback callback) {
        this.mPreviewBarCallback = callback;
        //doPreview(uri, new Surface(texturePreView.getSurfaceTexture()));
        setPreviewCallback(uri, texturePreView);
    }

    public void initDefault(Uri uri, PreviewBarCallback previewBarCallback) {
        this.mPreviewBarCallback = previewBarCallback;
        setPreviewCallback(uri, texturePreView);
    }

    public void updateProgress(int progress) {
        if (progress >= 0 && progress <= duration) {
            txtVideoProgress.setText(getVideoTime(progress));
            previewBar.setProgress(progress);
        }
    }

    public void release() {
        if (hardwareDecode != null) {
            hardwareDecode.release();
            hardwareDecode = null;
        }
    }

    public interface PreviewBarCallback {
        void onStopTracking(long progress);
    }

}
