/*
 * Copyright 2020 Akash Magnadia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.armcomptech.smartanimaldetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.armcomptech.smartanimaldetector.env.ImageUtils;
import com.armcomptech.smartanimaldetector.env.Logger;
import com.armcomptech.smartanimaldetector.tflite.Classifier;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
  private static final int REQUEST_INVITE = 100;
  private static final String TAG = "CameraActivity";
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;
  protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private SwitchCompat apiSwitchCompat;
  private TextView threadsTextView;

  private TextView mCaptureCount;

  private boolean generalSwitchTakePhoto;
  private boolean birdSwitchTakePhoto;
  private int birdSeekBar;
  private boolean squirrelSwitchTakePhoto;
  private int squirrelSeekBar;
  private FrameLayout frameLayout;

  private boolean greenLightToTakePhoto = true;
  private FirebaseAnalytics mFirebaseAnalytics;

  Button mBtnCapture;
  CameraConnectionFragment camera2Fragment;
  Fragment fragment;

  @SuppressLint("StaticFieldLeak")
  private static CameraActivity instance;

  public CameraActivity() {
    instance = this;
  }

  public static Context getContext() {
    return instance;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  protected void onCreate(final Bundle savedInstanceState) {

    // Obtain the FirebaseAnalytics instance.
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

    //for logging event created bundles
    Bundle openedApp = new Bundle();
    openedApp.putString(FirebaseAnalytics.Param.ITEM_ID, "openApp");
    openedApp.putString(FirebaseAnalytics.Param.ITEM_NAME, "Opened Application");
    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, openedApp);

    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    setContentView(R.layout.tfe_od_activity_camera);
    Toolbar toolbar = findViewById(R.id.toolbar);
    Objects.requireNonNull(toolbar.getOverflowIcon()).setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP);
    setSupportActionBar(toolbar);
    Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

    mBtnCapture = findViewById(R.id.btnCapture);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    threadsTextView = findViewById(R.id.threads);
    int numThreads = 4; //default threads to be used by the device
    threadsTextView.setText(String.valueOf(numThreads));
    setNumThreads(numThreads);

    ImageView plusImageView = findViewById(R.id.plus);
    ImageView minusImageView = findViewById(R.id.minus);
    apiSwitchCompat = findViewById(R.id.api_info_switch);
    LinearLayout bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
    mCaptureCount = findViewById(R.id.captureCount);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                //                int width = bottomSheetLayout.getMeasuredWidth();
                int height = gestureLayout.getMeasuredHeight();

                sheetBehavior.setPeekHeight(height);
              }
            });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
            new BottomSheetBehavior.BottomSheetCallback() {
              @Override
              public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                  case BottomSheetBehavior.STATE_HIDDEN:
                    break;
                  case BottomSheetBehavior.STATE_EXPANDED: {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                  }
                  break;
                  case BottomSheetBehavior.STATE_COLLAPSED: {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                  }
                  break;
                  case BottomSheetBehavior.STATE_DRAGGING:
                    break;
                  case BottomSheetBehavior.STATE_SETTLING:
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                    break;
                  case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    break;
                }
              }

              @Override
              public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });

    frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);
    frameLayout = findViewById(R.id.container);

    apiSwitchCompat.setOnCheckedChangeListener(this);

    plusImageView.setOnClickListener(this);
    minusImageView.setOnClickListener(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

    if (requestCode == REQUEST_INVITE) {
      if (resultCode == RESULT_OK) {
        // Get the invitation IDs of all sent messages
        String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
        for (String id : ids) {
          Log.d(TAG, "onActivityResult: sent invitation " + id);
        }
      } else {
        // Sending failed or it was canceled, show failure message to the user
        // ...
      }
    }
  }

  public void checkForSettings() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    boolean generalBoxSwitch = sharedPreferences.getBoolean("generalBoxSwitch", true);
    boolean defaultGeneralBoxCheckBox = sharedPreferences.getBoolean("defaultGeneralBoxCheckBox", true);
    int generalBoxSeekBar;
    if (defaultGeneralBoxCheckBox) {
      generalBoxSeekBar = SettingsActivity.getDefaultConfidenceLevel(); //default confidence level
    } else {
      generalBoxSeekBar = sharedPreferences.getInt("generalBoxSeekBar", 50);
    }

    generalSwitchTakePhoto = sharedPreferences.getBoolean("generalSwitchTakePhoto", false);

    birdSwitchTakePhoto = sharedPreferences.getBoolean("birdSwitchTakePhoto", false);
    boolean defaultBirdTakePhotoCheckBox = sharedPreferences.getBoolean("defaultBirdTakePhotoCheckBox", true);
    if (defaultBirdTakePhotoCheckBox) {
      birdSeekBar = SettingsActivity.getDefaultConfidenceLevel(); //default confidence level
    } else {
      birdSeekBar = sharedPreferences.getInt("birdSeekBar", 50);
    }

    squirrelSwitchTakePhoto = sharedPreferences.getBoolean("squirrelSwitchTakePhoto", false);
    boolean defaultSquirrelTakePhotoCheckBox = sharedPreferences.getBoolean("defaultSquirrelTakePhotoCheckBox", true);
    if (defaultSquirrelTakePhotoCheckBox) {
      squirrelSeekBar = SettingsActivity.getDefaultConfidenceLevel(); //default confidence level
    } else {
      squirrelSeekBar = sharedPreferences.getInt("squirrelSeekBar", 50);
    }

    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

    if (getScreenOrientation() == 0 || getScreenOrientation() == 180) {
      params.setMargins(0, 0, 0,0);
    } else if (getScreenOrientation() == 90 || getScreenOrientation() == 270) {

      WindowManager wm = (WindowManager) CameraActivity.getContext().getSystemService(Context.WINDOW_SERVICE);
      getScreenOrientation();
      assert wm != null;
      Display display = wm.getDefaultDisplay();
      int width = display.getWidth();
      int height = display.getHeight();

      if (width > 1000) {
        params.setMargins(0, 0, 0,0);
      } else //noinspection IntegerDivisionInFloatingPointContext
        if (height/width < 0.70) {
        params.setMargins(150, 0, 150,0);
      } else {
        params.setMargins(0, 0, 0,0);
      }
    }
    frameLayout.setLayoutParams(params);
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    //noinspection SuspiciousNameCombination
    yRowStride = previewWidth;

    imageConverter =
            () -> ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);

    postInferenceCallback =
            () -> {
              camera.addCallbackBuffer(bytes);
              isProcessingFrame = false;
            };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
              () -> ImageUtils.convertYUV420ToARGB8888(
                      yuvBytes[0],
                      yuvBytes[1],
                      yuvBytes[2],
                      previewWidth,
                      previewHeight,
                      yRowStride,
                      uvRowStride,
                      uvPixelStride,
                      rgbBytes);

      postInferenceCallback =
              () -> {
                image.close();
                isProcessingFrame = false;
              };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    greenLightToTakePhoto = true;
    LOGGER.d("onStart " + this);
    checkForSettings();
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    checkForSettings();
    greenLightToTakePhoto = true;
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    greenLightToTakePhoto = false;
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    greenLightToTakePhoto = false;
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public void onRequestPermissionsResult(
          final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      return (checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED) &&
              (checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required to use this app",
                Toast.LENGTH_LONG)
                .show();
      }
      if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
        Toast.makeText(
                CameraActivity.this,
                "Storage permission required to save photos",
                Toast.LENGTH_LONG)
                .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA, WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
          CameraCharacteristics characteristics) {
    @SuppressWarnings("ConstantConditions") int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return false;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL <= deviceLevel;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      assert manager != null;
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        //noinspection ConstantConditions
        useCamera2API =
                (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  protected void setFragment() {
    String cameraId = chooseCamera();

    if (useCamera2API) {
      camera2Fragment =
              CameraConnectionFragment.newInstance(
                      (size, rotation) -> {
                        previewHeight = size.getHeight();
                        previewWidth = size.getWidth();
                        CameraActivity.this.onPreviewSizeChosen(size, rotation);
                      },
                      this,
                      getLayoutId(),
                      getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;

      mBtnCapture.setOnClickListener(e ->
      {
        //when button capture is clicked
        logPictureTaken();
        camera2Fragment.takePicture();
        refreshCaptureCount();
      });

    } else {
      fragment =
              new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
      mBtnCapture.setOnClickListener(e ->
      {
        //when button capture is clicked
        logPictureTaken();
        ((LegacyCameraConnectionFragment) fragment).takePicture();
        refreshCaptureCount();
      });
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  private void logPictureTaken() {
    Bundle pictureTaken = new Bundle();
    pictureTaken.putString(FirebaseAnalytics.Param.ITEM_ID, "takePic");
    pictureTaken.putString(FirebaseAnalytics.Param.ITEM_NAME, "Picture Taken");
    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, pictureTaken);
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return false;
  }

  @SuppressLint("SetTextI18n")
  public void refreshCaptureCount() {
    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Smart Wildlife Capture");

    File[] list = file.listFiles();
    int count = 0;
    if (list != null) {
      for (File f: list){
        String name = f.getName();
        if (name.endsWith(".jpg"))
          count++;
      }
    }

    int finalCount = count;
    runOnUiThread(() -> mCaptureCount.setText("Captures: " + finalCount));
  }

  //TODO: change the objects and models
  void checkforObject(List<Classifier.Recognition> results) {
    if (generalSwitchTakePhoto) { //if green light to take photos
      for (final Classifier.Recognition result : results) {

        if(result.getTitle().equals("Squirrel")
                && (result.getConfidence() >= (float)(squirrelSeekBar/100))
                && squirrelSwitchTakePhoto && generalSwitchTakePhoto && greenLightToTakePhoto) {

          //show capture count to user
          refreshCaptureCount();

          // if green light for squirrel and confidence level is surpassed
          if (camera2Fragment != null) {
            logPictureTaken();
            camera2Fragment.takePicture();
          } else {
            logPictureTaken();
            ((LegacyCameraConnectionFragment) fragment).takePicture();
          }

        } else if(result.getTitle().equals("Bird")
                && (result.getConfidence() >= (float)(birdSeekBar/100))
                && birdSwitchTakePhoto && generalSwitchTakePhoto && greenLightToTakePhoto) {

          //show capture count to user
          refreshCaptureCount();

          // if green light for squirrel and confidence level is surpassed
          if (camera2Fragment != null) {
            logPictureTaken();
            camera2Fragment.takePicture();
          } else {
            logPictureTaken();
            ((LegacyCameraConnectionFragment) fragment).takePicture();
          }
        }
      }
    }
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
      case Surface.ROTATION_0:
        break;
    }
    return 0;
  }

  @SuppressLint("SetTextI18n")
  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }
  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }

  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);
  protected abstract void setUseNNAPI(boolean isChecked);

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.option_menu, menu);

    menu.add(0, R.id.settings, 0, menuIconWithText(getResources().getDrawable(R.drawable.ic_baseline_settings_24_black), "Settings"));
    menu.add(0, R.id.privacy_policy, 1, menuIconWithText(getResources().getDrawable(R.drawable.ic_baseline_lock_24_black), "Privacy Policy"));
    menu.add(0, R.id.share, 2, menuIconWithText(getResources().getDrawable(R.drawable.ic_baseline_share_24_black), "Share"));

    return true;
  }

  private CharSequence menuIconWithText(Drawable r, String title) {

    r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
    SpannableString sb = new SpannableString("    " + title);
    ImageSpan imageSpan = new ImageSpan(r, ImageSpan.ALIGN_BOTTOM);
    sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    return sb;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (item.isChecked()) {
      item.setChecked(false);
    } else {
      item.setChecked(true);
    }

    switch (id) {

      case R.id.settings:
        greenLightToTakePhoto = false;
        startActivity(new Intent(this, SettingsActivity.class));
        break;

      case R.id.privacy_policy:
        Intent myWebLink = new Intent(android.content.Intent.ACTION_VIEW);
        myWebLink.setData(Uri.parse("https://smartanimaldetector.blogspot.com/2020/06/smart-animal-detector-privacy-policy.html"));
        startActivity(myWebLink);
        break;

      case R.id.share:
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);

        Bundle shareApp = new Bundle();
        shareApp.putString(FirebaseAnalytics.Param.ITEM_ID, "share");
        shareApp.putString(FirebaseAnalytics.Param.ITEM_NAME, "Share Application");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, shareApp);
        break;

      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }
}