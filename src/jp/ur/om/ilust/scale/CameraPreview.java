package jp.ur.om.ilust.scale;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.Toast;

/**
 * カメラのプレビューを描画するビュー。サーフェイスビューの拡張でありサーフェイスホルダーコールバックの実装
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

	private SurfaceHolder holder;
	protected Camera camera;
	private Context thisContext;
	private AtomicBoolean nowAutoFocusFlag = new AtomicBoolean(false);


	/**
	 * コンストラクター
	 * 
	 * @param context
	 *            コンテキスト
	 */
	public CameraPreview(Context context) {

		super(context);
		/* コンテクストの保持 */
		this.thisContext = context;

		this.holder = getHolder();
		this.holder.addCallback(this);

		/* リファレンス見ると必要なさそうだけど削除すると動かなくなる */
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		try {
			/* カメラデバイスをオープン */
			camera = Camera.open();
			/* プレビューディスプレイを自身のSerfaceHolderに設定 */
			camera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
			/* プレビューを止める */
			camera.stopPreview();
			/* カメラデバイスの解放 */
			camera.release();
		}
	}


	@Override
	public void surfaceChanged(SurfaceHolder horder, int format, int width, int height) {

		/* プレビューを止める */
		camera.stopPreview();
		/* カメラのパラメータを取得 */
		Parameters params = camera.getParameters();

		/* 端末がサポートしているプレビューサイズを取得 */
		List<Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
		if ((supportedPreviewSizes != null) && (supportedPreviewSizes.size() > 0)) {
			/* 画面サイズに近いプレビューサイズを探す */
			Size selectPreviewSize = null;
			long x;
			long y;
			long scxy = Long.MAX_VALUE;
			long tempScxy;
			for (Size previewSize : supportedPreviewSizes) {
				x = previewSize.width - width;
				y = previewSize.height - height;
				tempScxy = (x * x) + (y * y);
				if (tempScxy < scxy) {
					scxy = tempScxy;
					selectPreviewSize = previewSize;
				}
			}
			params.setPreviewSize(selectPreviewSize.width, selectPreviewSize.height);
			params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		} else {
			params.setPreviewSize(width, height);
		}

		SharedPreferences preference = thisContext.getSharedPreferences("scale_config", Context.MODE_PRIVATE);

		/* 広角レンズ補正(1段階ズーム)のパラメータを設定 */
		zoomConfig(params, preference);

		try {
			/* パラメータセット */
			camera.setParameters(params);
			/* プレビューを再開 */
			camera.startPreview();
		} catch (Exception e) {
			Editor editor = preference.edit();
			editor.putBoolean("zoom_flag", false);
			editor.commit();
			surfaceDestroyed(null);
			return;
		}

		/* 初回起動であるか確認 */

		if (preference.getInt("first_launch", 0) == 0) {
			/* 初回起動時メッセージを表示する */
			Toast.makeText(thisContext, R.string.first_launch_af, Toast.LENGTH_LONG).show();
			Toast.makeText(thisContext, R.string.first_launch_cfg, Toast.LENGTH_LONG).show();

			/* 初回起動済みに設定 */
			Editor editor = preference.edit();
			editor.putInt("first_launch", 1);
			editor.commit();
		}
	}


	/** 広角レンズ補正(1～2段階ズーム)のパラメータを設定 */
	private void zoomConfig(Parameters params, SharedPreferences preference) {
		if (!params.isZoomSupported()) {
			Log.d("zoom scale", "ズーム非サポート機種");
			return;
		}
		if (preference.getBoolean("zoom_flag", false)) {
			List<Integer> zoomList = params.getZoomRatios();
			if ((zoomList != null) && (zoomList.size() > 1)) {
				int targetParsentage;
				if (preference.getBoolean("zoom_plus_flag", false)) {
					targetParsentage = 128;
				} else {
					targetParsentage = 115;
				}
				int abs = 10000;
				int zoom = 1;
				Log.d("zoom scale", "listsize" + zoomList.size());
				for (int i = 0; i < zoomList.size(); i++) {
					Log.d("zoom scale", "" + zoomList.get(i));
					int tempAbs = Math.abs(targetParsentage - zoomList.get(i));
					if (abs > tempAbs) {
						abs = tempAbs;
						zoom = i;
					}
				}
				params.setZoom(zoom);
				Log.d("zoom scale", "zoomList" + zoom + "番目に設定");
			}
		} else {
			params.setZoom(0);
		}
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

		/* プレビューを止める */
		camera.setPreviewCallback(null);
		camera.stopPreview();
		/* カメラデバイスの解放 */
		camera.release();
		camera = null;
	}


	/** オートフォーカス実行 */
	public void autoFocus() {
		if (camera != null) {
			/* オートフォーカスコールバック */
			camera.autoFocus(new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					nowAutoFocusFlag.set(false);
				}
			});
		}
	}


	/** カメラのプレビューを止める */
	public void previewHold() {
		camera.takePicture(null, null, null);
	}


	/** カメラのプレビューを再開 */
	public void previewRestart() {
		camera.startPreview();
	}


	/** オートフォーカス中である旨のメッセージをトースト表示する */
	public void showAutoFocusMessage() {
		if (nowAutoFocusFlag.get() == false) {
			nowAutoFocusFlag.set(true);
			Toast toast = Toast.makeText(thisContext, R.string.focus_set, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP | Gravity.LEFT, 10, 10);
			toast.show();
		}
	}
}
