package autoandshare.headvr.activity;

import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.tencent.mmkv.MMKV;

import org.videolan.medialibrary.media.MediaWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;

import autoandshare.headvr.R;
import autoandshare.headvr.lib.BasicUI;
import autoandshare.headvr.lib.NoDistortionProvider;
import autoandshare.headvr.lib.Setting;
import autoandshare.headvr.lib.VideoRenderer;
import autoandshare.headvr.lib.browse.PlayList;
import autoandshare.headvr.lib.headcontrol.HeadControl;
import autoandshare.headvr.lib.headcontrol.HeadMotion.Motion;
import autoandshare.headvr.lib.rendering.Mesh;
import autoandshare.headvr.lib.rendering.VRTexture2D;

public class VideoActivity extends GvrActivity implements
        GvrView.StereoRenderer {


    private static final String TAG = "VideoActivity";

    private BasicUI basicUI;

    private Setting setting;

    private GvrView cardboardView;
    private PlayList playList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //NoDistortionProvider.setupProvider(this);

        setting = new Setting(this);

        setContentView(R.layout.video_ui);

        MMKV.initialize(this);

        cardboardView = findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        this.setGvrView(cardboardView);

        Log.i("intent", "start");
        Uri uri = this.getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        Log.i("intent", uri.toString());
        playList = PlayList.getPlayList(uri, this);
    }

    private HeadControl headControl = new HeadControl();
    private static final List<Motion> PlayPause = Arrays.asList(Motion.DOWN, Motion.UP);
    private static final List<Motion> Left = Arrays.asList(Motion.LEFT, Motion.IDLE);
    private static final List<Motion> Right = Arrays.asList(Motion.RIGHT, Motion.IDLE);
    private static final List<Motion> Idle = Collections.singletonList(Motion.IDLE);
    private static final List<Motion> Idles = Arrays.asList(Motion.IDLE, Motion.IDLE, Motion.IDLE);
    private static final List<Motion> Any = Collections.singletonList(Motion.ANY);
    private static final List<Motion> Return = Arrays.asList(Motion.UP, Motion.LEFT, Motion.RIGHT, Motion.DOWN);
    private static final List<Motion> Next = Arrays.asList(Motion.UP, Motion.DOWN, Motion.RIGHT, Motion.LEFT);
    private static final List<Motion> Prev = Arrays.asList(Motion.UP, Motion.DOWN, Motion.LEFT, Motion.RIGHT);
    private static final List<Motion> Round = Arrays.asList(Motion.RIGHT, Motion.DOWN, Motion.LEFT, Motion.UP);
    private static final List<Motion> ReverseRound = Arrays.asList(Motion.LEFT, Motion.DOWN, Motion.RIGHT, Motion.UP);
    private static final List<Motion> Force2D = Arrays.asList(Motion.RIGHT, Motion.LEFT, Motion.RIGHT, Motion.LEFT);
    private static final List<Motion> Recenter = Arrays.asList(Motion.LEFT, Motion.RIGHT, Motion.LEFT, Motion.RIGHT);

    private void setupMotionActionTable() {
        headControl.addMotionAction(Any, () -> {
            updateUIVisibility(Motion.ANY);
            return false;
        });
        headControl.addMotionAction(PlayPause, () -> videoRenderer.pauseOrPlay());
        headControl.addMotionAction(Left, () -> videoRenderer.handleSeeking(Motion.LEFT, headControl));
        headControl.addMotionAction(Right, () -> videoRenderer.handleSeeking(Motion.RIGHT, headControl));
        headControl.addMotionAction(Idle, () -> videoRenderer.handleSeeking(Motion.IDLE, headControl));
        headControl.addMotionAction(Any, () -> videoRenderer.handleSeeking(Motion.ANY, headControl));
        headControl.addMotionAction(Idles, () -> {
            updateUIVisibility(Motion.IDLE);
            return false;
        });
        headControl.addMotionAction(Return, this::returnHome);
        headControl.addMotionAction(Next, this::nextFile);
        headControl.addMotionAction(Prev, this::prevFile);
        headControl.addMotionAction(Round, () -> updateEyeDistance(3));
        headControl.addMotionAction(ReverseRound, () -> updateEyeDistance(-3));
        headControl.addMotionAction(Force2D, () -> videoRenderer.toggleForce2D());
        headControl.addMotionAction(Recenter, () -> recenter());
    }

    private boolean resetRotationMatrix = false;

    private Boolean recenter() {
        if (!videoRenderer.getState().isVR()) {
            return false;
        }

        hideAll = true;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            resetRotationMatrix = true;
            hideAll = false;
        }, 1000);
        return true;
    }

    private Boolean updateEyeDistance(int i) {
        Setting.id id = videoRenderer.getState().drawAs2D() ?
                Setting.id.EyeDistance : Setting.id.EyeDistance3D;

        updateEyeDistanceWithId(i, id);
        return true;
    }

    private void updateEyeDistanceWithId(int i, Setting.id id) {
        int eyeDistance = setting.get(id) + i;
        if (eyeDistance > setting.getMax(id)) {
            eyeDistance = setting.getMax(id);
        }
        if (eyeDistance < setting.getMin(id)) {
            eyeDistance = setting.getMin(id);
        }

        setting.set(id, eyeDistance);

        videoRenderer.getState().message = "setting " + id + " to " + eyeDistance;
    }

    private Boolean playMediaFromList(int offset) {
        if (!playList.isReady()) {
            videoRenderer.getState().message = "Loading play list";
        } else {
            loaded = true;

            MediaWrapper mw = playList.next(offset);
            if (mw == null) {
                videoRenderer.getState().errorMessage = "Invalid play list";
            } else {
                hideAll = true;
                videoRenderer.playUri(mw);
                new Handler(Looper.getMainLooper()).postDelayed(() -> hideAll = false, 1500);
            }
        }
        return true;
    }

    private boolean hideAll = false;

    private Boolean prevFile() {
        return playMediaFromList(-1);
    }

    private Boolean nextFile() {
        return playMediaFromList(1);
    }


    private boolean loaded = false;

    private void loadFirstVideo() {
        if (loaded) {
            return;
        }

        playMediaFromList(0);

    }

    private Boolean returnHome() {
        finish();
        return true;
    }

    private void setBrightness() {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = Setting.Brightness;
        getWindow().setAttributes(layout);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()");

        if (videoRenderer != null) {
            videoRenderer.pause();
            videoRenderer.savePosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");

        setBrightness();
        if (videoRenderer != null) {
            videoRenderer.updateVideoPosition();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }


    @Override
    public void onNewFrame(HeadTransform headTransform) {
        loadFirstVideo();

        float[] upVector = new float[3];
        headTransform.getUpVector(upVector, 0);

        float[] forwardVector = new float[3];
        headTransform.getForwardVector(forwardVector, 0);

        headControl.handleMotion(upVector, forwardVector);

        if (resetRotationMatrix) {
            if (Mesh.recenterMatrix == null) {
                Mesh.recenterMatrix = new float[16];
            }
            Matrix.transposeM(Mesh.recenterMatrix, 0, headTransform.getHeadView(), 0);
            Mesh.recenterMatrix[3] = Mesh.recenterMatrix[7] = Mesh.recenterMatrix[11] = 0;
            resetRotationMatrix = false;
        }
    }

    @Override
    public void onDrawEye(Eye eye) {

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (hideAll) {
            return;
        }

        videoRenderer.glDraw(eye);

        if (uiVisible) {
            basicUI.glDraw(eye, videoRenderer.getState(), headControl,
                    (playList != null) ? playList.currentIndex() : "");
        }

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    VideoRenderer videoRenderer;

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        VRTexture2D.glInit();

        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0f, 0f, 0f, 0.5f); // Dark background so text shows up well.

        basicUI = new BasicUI();

        videoRenderer = new VideoRenderer(this);

        setupMotionActionTable();
    }

    private boolean uiVisible = true;

    private void updateUIVisibility(Motion motion) {
        if (!videoRenderer.normalPlaying()) {
            uiVisible = true;
            return;
        }
        if (motion == Motion.ANY) {
            if (headControl.notIdle()) {
                uiVisible = true;
            }
        } else if (motion == Motion.IDLE) {
            if (videoRenderer.normalPlaying()) {
                uiVisible = false;
                videoRenderer.getState().message = null;
            }
        }
    }
}