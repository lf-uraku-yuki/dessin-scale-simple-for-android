package jp.ur.om.ilust.scale;

import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * デスケルアプリのメイン・起動アクティビティ
 * 
 * @author uraku
 * 
 */
public class DessinScaleActivity extends Activity {

	private CameraPreview cameraPreview;
	private ScaleOverlayView scaleOverlayView;

	/** 画面ホールドフラグ */
	private AtomicBoolean nowScreenHoldFlag = new AtomicBoolean(false);
	/** インテント管理用 */
	private static final int MAIN_REQUEST = 200;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/* UIをフルスクリーンにする */
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		/* アプリのタイトルバーを削除する */
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		/* カメラプレビューをビューにセットする */
		try {
			cameraPreview = new CameraPreview(this);
			setContentView(cameraPreview);
		} catch (Exception e) {
			Toast toast = Toast.makeText(this, R.string.start_failed, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			SharedPreferences preference = getSharedPreferences("scale_config", Context.MODE_PRIVATE);
			Editor editor = preference.edit();
			editor.putBoolean("zoom_flag", false);
			editor.commit();

			finish();
		}

		/* ホールド状態：false */
		nowScreenHoldFlag.set(false);

		/* スケールの描画 */
		scaleOverlayView = new ScaleOverlayView(this);
		scaleOverlayView.setKeepScreenOn(true); // スクリーンの自動消灯を行わない
		this.addContentView(scaleOverlayView, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
	}


	/* Viewに対するタッチイベント */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		/* 画面ホールド状態でない時のみ */
		if (nowScreenHoldFlag.get() == false) {
			cameraPreview.showAutoFocusMessage(); // オートフォーカスメッセージを表示
			try {
				cameraPreview.autoFocus(); // オートフォーカスを実行する
			} catch (Exception e) {
				/* 何もしない */
			}
		}
		return true;
	}


	/* オプションメニューの作成 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return (super.onCreateOptionsMenu(menu));
	}


	/* メニューを開いた際に項目の再セットアップを行う */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		populateMenu(menu);
		return (super.onPrepareOptionsMenu(menu));
	}


	/**
	 * メニュー項目のセットアップ
	 * 
	 * @param menu
	 *            メニューオブジェクト
	 */
	private void populateMenu(Menu menu) {

		menu.removeGroup(1);
		menu.add(1, 1, Menu.NONE, R.string.config);

		/* ホールド状態によって二つ目のメニューを切り替える */
		if (nowScreenHoldFlag.get() == false) {
			menu.add(1, 2, Menu.NONE, R.string.display_hold);
		} else {
			menu.add(1, 3, Menu.NONE, R.string.hold_off);
		}
	}


	/* メニュー選択処理 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return (applyMenuChoice(item) || super.onOptionsItemSelected(item));
	}


	/**
	 * メニュー内の処理
	 * 
	 * @param item
	 *            メニューアイテム
	 * @return 処理結果
	 */
	private boolean applyMenuChoice(MenuItem item) {
		switch (item.getItemId()) {
		case (1):
			/* ホールド状態:false */
			nowScreenHoldFlag.set(false);

			/* インテントの作成と発行 ConfigActivityに遷移する */
			Intent intent = new Intent();
			intent.setClass(this, ConfigActivity.class);
			startActivityForResult(intent, MAIN_REQUEST);
			return true;

		case (2):
			/* 画面をホールド（保存はしない。プレビュー一時停止のために使う） */
			cameraPreview.previewHold();
			nowScreenHoldFlag.set(true);
			return true;

		case (3):
			/* 画面のホールドを解除しプレビュー再開 */
			cameraPreview.previewRestart();
			nowScreenHoldFlag.set(false);
			return true;

		default:
		}
		return false;
	}


	/** スケールをオーバーレイ描画するビュー */
	class ScaleOverlayView extends View {

		/* Paintオブジェクトを作成する */
		Paint paint = new Paint();

		/* ディスプレイのスケール、解像度 */
		private float dipScale;
		private int displayWidthSize;
		private int displayHeightSize;

		/* デスケル描画エリア */
		private int dessinScaleWidth;
		private int dessinScaleHeight;


		/**
		 * コンストラクター
		 * 
		 * @param context
		 *            コンテキスト
		 */
		public ScaleOverlayView(Context context) {
			super(context);
			setFocusable(true);

			/* ディスプレイのスケール、解像度を取得 */
			DisplayMetrics metrics = new DisplayMetrics();
			WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
			wm.getDefaultDisplay().getMetrics(metrics);

			dipScale = metrics.scaledDensity;
			displayWidthSize = metrics.widthPixels;
			displayHeightSize = metrics.heightPixels;

			/* デスケル描画エリアは画面サイズよりも少し小さくする */
			dessinScaleWidth = (int) (displayWidthSize - (8 * dipScale));
			dessinScaleHeight = (int) (displayHeightSize - (8 * dipScale));

			this.setWillNotDraw(false);
		}


		/* onDrawの実装 */
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);

			/* SharedPreferencesからアスペクト比設定値の読出し */
			float scaleAspect;
			SharedPreferences pref = getSharedPreferences("scale_config", MODE_PRIVATE);
			scaleAspect = pref.getFloat("scale_aspect", 1.1666f);

			/* SharedPreferencesから色設定値の読出し */
			int lineColorNumber = pref.getInt("scale_color_select", 1);

			/* SharedPreferencesから中間線表示設定の読出し */
			boolean middleLineDrawFlag = pref.getBoolean("scale_middle_line", true);

			/* ライン描画の一次セットアップ */
			canvas.drawColor(Color.TRANSPARENT); // 多分必要

			/* カラーの設定 */
			switch (lineColorNumber) {
			case (0):
				paint.setColor(Color.WHITE);
				break;

			case (1):
				paint.setColor(Color.BLACK);
				break;

			case (2):
				paint.setColor(Color.RED);
				break;

			case (3):
				paint.setColor(0xff228B22);
				break;

			case (4):
				paint.setColor(0xff4169E1);
				break;

			case (5):
				paint.setColor(0x88000000);
				break;

			default:
				paint.setColor(Color.BLACK);
				break;
			}

			/* ディスプレイアスペクト比の確認 */
			if (((dessinScaleWidth + (1.5 * dipScale)) / (dessinScaleHeight + (1.5 * dipScale))) >= scaleAspect) {
				widthBasedDrawScaleLine(canvas, paint, scaleAspect, middleLineDrawFlag); /* ライン描画メソッドコール */
			} else {
				heightBasedDrawScaleLine(canvas, paint, scaleAspect, middleLineDrawFlag); /* ライン描画メソッドコール */
			}
		}


		/**
		 * width長を元にデスケルの描画を行う。heightは最大幅となる。
		 * 
		 * @param canvas
		 *            キャンバスオブジェクト
		 * @param paint
		 *            ペイントオブジェクト
		 * @param scaleAspect
		 *            描画するスケールのアスペクト比
		 * @param middleLineDrawFlag
		 *            スケールの中間線を描画する場合true
		 */
		private void widthBasedDrawScaleLine(Canvas canvas, Paint paint, double scaleAspect,
				boolean middleLineDrawFlag) {

			int scale_width_line_size = (int) (dessinScaleHeight * scaleAspect);

			/* ライン描画の二次セットアップ */
			/* 主線の太さを設定 */
			paint.setStrokeWidth(3 * dipScale);

			/* 上の横線 */
			canvas.drawLine(((displayWidthSize - scale_width_line_size) / 2.0f),
					((displayHeightSize - dessinScaleHeight) / 2.0f),
					(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
					((displayHeightSize - dessinScaleHeight) / 2.0f), paint);

			/* 下の横線 */
			canvas.drawLine(((displayWidthSize - scale_width_line_size) / 2.0f),
					(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight),
					(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
					(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);

			/* 中央の横線 */
			canvas.drawLine(((displayWidthSize - scale_width_line_size) / 2.0f), (displayHeightSize / 2.0f),
					(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
					(displayHeightSize / 2.0f), paint);

			/* 左の縦線 */
			canvas.drawLine(((displayWidthSize - scale_width_line_size) / 2.0f),
					((displayHeightSize - dessinScaleHeight) / 2.0f),
					((displayWidthSize - scale_width_line_size) / 2.0f),
					(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);

			/* 右の縦線 */
			canvas.drawLine((((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
					((displayHeightSize - dessinScaleHeight) / 2.0f),
					(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
					(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);

			/* 中央の縦線 */
			canvas.drawLine((displayWidthSize / 2.0f), ((displayHeightSize - dessinScaleHeight) / 2.0f),
					(displayWidthSize / 2.0f),
					(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);

			/* 補助線用にラインの太さを変更 */
			paint.setStrokeWidth(1 * dipScale);

			if (middleLineDrawFlag) {
				/* 上の補助横線 */
				canvas.drawLine(
						((displayWidthSize - scale_width_line_size) / 2.0f),
						((((displayHeightSize - dessinScaleHeight) / 2.0f) + (displayHeightSize / 2.0f)) / 2.0f),
						(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
						((((displayHeightSize - dessinScaleHeight) / 2.0f) + (displayHeightSize / 2.0f)) / 2.0f),
						paint);

				/* 下の補助横線 */
				canvas.drawLine(
						((displayWidthSize - scale_width_line_size) / 2.0f),
						(((((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight) + (displayHeightSize / 2.0f)) / 2.0f),
						(((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size),
						(((((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight) + (displayHeightSize / 2.0f)) / 2.0f),
						paint);

				/* 左の補助縦線 */
				canvas.drawLine(
						((((displayWidthSize - scale_width_line_size) / 2.0f) + (displayWidthSize / 2.0f)) / 2.0f),
						((displayHeightSize - dessinScaleHeight) / 2.0f),
						((((displayWidthSize - scale_width_line_size) / 2.0f) + (displayWidthSize / 2.0f)) / 2.0f),
						(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);

				/* 右の補助縦線 */
				canvas.drawLine(
						(((((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size) + (displayWidthSize / 2.0f)) / 2.0f),
						((displayHeightSize - dessinScaleHeight) / 2.0f),
						(((((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size) + (displayWidthSize / 2.0f)) / 2.0f),
						(((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight), paint);
			}

			/* 枠外の塗りつぶし：左 */
			canvas.drawRect(0, 0, ((displayWidthSize - scale_width_line_size) / 2.0f), displayHeightSize,
					paint);

			/* 枠外の塗りつぶし：上 */
			canvas.drawRect(0, 0, displayWidthSize, ((displayHeightSize - dessinScaleHeight) / 2.0f), paint);

			/* 枠外の塗りつぶし：右 */
			canvas.drawRect((((displayWidthSize - scale_width_line_size) / 2.0f) + scale_width_line_size), 0,
					displayWidthSize, displayHeightSize, paint);

			/* 枠外の塗りつぶし：下 */
			canvas.drawRect(0, (((displayHeightSize - dessinScaleHeight) / 2.0f) + dessinScaleHeight),
					displayWidthSize, displayHeightSize, paint);

			return;
		}


		/**
		 * width長を元にデスケルの描画を行う。widthtは最大幅となる。
		 * 
		 * @param canvas
		 *            キャンバスオブジェクト
		 * @param paint
		 *            ペイントオブジェクト
		 * @param scaleAspect
		 *            描画するスケールのアスペクト比
		 * @param middleLineDrawFlag
		 *            スケールの中間線を描画する場合true
		 */
		private void heightBasedDrawScaleLine(Canvas canvas, Paint paint, double scaleAspect,
				boolean middleLineDrawFlag) {

			int scale_height_line_size = (int) ((dessinScaleWidth * (1 / scaleAspect)));

			/* ライン描画の二次セットアップ */
			/* 主線の太さを設定 */
			paint.setStrokeWidth(3 * dipScale);

			/* 上の横線 */
			canvas.drawLine(((displayWidthSize - dessinScaleWidth) / 2.0f),
					((displayHeightSize - scale_height_line_size) / 2.0f),
					(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
					((displayHeightSize - scale_height_line_size) / 2.0f), paint);

			/* 下の横線 */
			canvas.drawLine(((displayWidthSize - dessinScaleWidth) / 2.0f),
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size),
					(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size), paint);

			/* 中央の横線 */
			canvas.drawLine(((displayWidthSize - dessinScaleWidth) / 2.0f), (displayHeightSize / 2.0f),
					(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
					(displayHeightSize / 2.0f), paint);

			/* 左の縦線 */
			canvas.drawLine(((displayWidthSize - dessinScaleWidth) / 2.0f),
					((displayHeightSize - scale_height_line_size) / 2.0f),
					((displayWidthSize - dessinScaleWidth) / 2.0f),
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size), paint);

			/* 右の縦線 */
			canvas.drawLine((((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
					((displayHeightSize - scale_height_line_size) / 2.0f),
					(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size), paint);

			/* 中央の縦線 */
			canvas.drawLine((displayWidthSize / 2.0f), ((displayHeightSize - scale_height_line_size) / 2.0f),
					(displayWidthSize / 2.0f),
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size), paint);

			/* 補助線用にラインの太さを変更 */
			paint.setStrokeWidth(1 * dipScale);

			if (middleLineDrawFlag) {

				/* 上の補助横線 */
				canvas.drawLine(
						((displayWidthSize - dessinScaleWidth) / 2.0f),
						(((displayHeightSize - scale_height_line_size) / 2.0f) + (scale_height_line_size / 4.0f)),
						(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
						(((displayHeightSize - scale_height_line_size) / 2.0f) + (scale_height_line_size / 4.0f)),
						paint);

				/* 下の補助横線 */
				canvas.drawLine(
						((displayWidthSize - dessinScaleWidth) / 2.0f),
						((((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size) - (scale_height_line_size / 4.0f)),
						(((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth),
						((((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size) - (scale_height_line_size / 4.0f)),
						paint);

				/* 左の補助縦線 */
				canvas.drawLine((((displayWidthSize - dessinScaleWidth) / 2.0f) + (dessinScaleWidth / 4.0f)),
						((displayHeightSize - scale_height_line_size) / 2.0f),
						(((displayWidthSize - dessinScaleWidth) / 2.0f) + (dessinScaleWidth / 4.0f)),
						(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size),
						paint);

				/* 右の補助縦線 */
				canvas.drawLine(
						((((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth) - (dessinScaleWidth / 4.0f)),
						((displayHeightSize - scale_height_line_size) / 2.0f),
						((((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth) - (dessinScaleWidth / 4.0f)),
						(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size),
						paint);

			}

			/* 枠外の塗りつぶし：左 */
			canvas.drawRect(0, 0, ((displayHeightSize - scale_height_line_size) / 2.0f), displayHeightSize,
					paint);

			/* 枠外の塗りつぶし：上 */
			canvas.drawRect(0, 0, displayWidthSize, ((displayHeightSize - scale_height_line_size) / 2.0f),
					paint);

			/* 枠外の塗りつぶし：右 */
			canvas.drawRect((((displayWidthSize - dessinScaleWidth) / 2.0f) + dessinScaleWidth), 0,
					displayWidthSize, displayHeightSize, paint);

			/* 枠外の塗りつぶし：下 */
			canvas.drawRect(0,
					(((displayHeightSize - scale_height_line_size) / 2.0f) + scale_height_line_size),
					displayWidthSize, displayHeightSize, paint);
			return;
		}


		/** Activityのレジュームが解除されたらスケールの再描画を行わせる */
		public void onResume() {
			scaleReDraw();
		}


		/** スケールの再描画を行う */
		private void scaleReDraw() {
			invalidate();
		}
	}
}