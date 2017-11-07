package jp.ur.om.ilust.scale;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

/**
 * 各種設定を行うためのアクティビティ
 */
public class ConfigActivity extends Activity {

	private Spinner spinnerAspect;
	private Spinner spinnerLineColor;
	private CheckBox checkMiddleLineDrawFlag;
	private CheckBox checkZoomFlag;
	private CheckBox checkZoomPlusFlag;


	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		/* タイトルバーを削除 */
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		/* レイアウトの適用 */
		setContentView(R.layout.config);
		/* スケール設定プリファレンスの読み込み */
		SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);

		/* Aspect-Sppinerのセットアップ。スケールのアスペクト比の設定項目 */
		ArrayAdapter<CharSequence> aspectArrayAdapter = ArrayAdapter.createFromResource(this,
				R.array.aspect_list, android.R.layout.simple_spinner_item);
		aspectArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerAspect = (Spinner) findViewById(R.id.aspect_spinner);
		spinnerAspect.setAdapter(aspectArrayAdapter);
		spinnerAspect.setSelection(preference.getInt("scale_aspect_select", 0)); // アスペクト比の前回設定値を取得
		/* アスペクト比項目選択時のコールバックリスナーを登録 */
		spinnerAspect.setOnItemSelectedListener(new ScaleAspectSelectedListener());

		/* Color-Sppinerのセットアップ。スケールの色の設定項目 */
		ArrayAdapter<CharSequence> colorArrayAdapter = ArrayAdapter.createFromResource(this,
				R.array.color_list, android.R.layout.simple_spinner_item);
		colorArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerLineColor = (Spinner) findViewById(R.id.color_spinner);
		spinnerLineColor.setAdapter(colorArrayAdapter);
		spinnerLineColor.setSelection(preference.getInt("scale_color_select", 1));
		/* スケール色項目選択時のコールバックリスナーを登録 */
		spinnerLineColor.setOnItemSelectedListener(new ScaleColorSelectedListener());

		/* 中間線表示選択チェックボックスのセットアップ */
		checkMiddleLineDrawFlag = (CheckBox) findViewById(R.id.middle_line_check);
		checkMiddleLineDrawFlag.setChecked((preference.getBoolean("scale_middle_line", true)));
		/* 中間線表示選択チェック操作時のリスナーを登録 */
		checkMiddleLineDrawFlag.setOnCheckedChangeListener(new CheckMiddleLineDrawFlagListener());

		/* 広角レンズ補正チェックボックスのセットアップ */
		checkZoomFlag = (CheckBox) findViewById(R.id.zoom_check);
		checkZoomFlag.setChecked((preference.getBoolean("zoom_flag", false)));
		checkZoomFlag.setOnCheckedChangeListener(new CheckZoomFlagListener());

		/* 広角レンズ補正プラスチェックボックスのセットアップ */
		checkZoomPlusFlag = (CheckBox) findViewById(R.id.zoom_plus);
		checkZoomPlusFlag.setChecked((preference.getBoolean("zoom_plus_flag", false)));
		checkZoomPlusFlag.setEnabled(checkZoomFlag.isChecked());
		checkZoomPlusFlag.setOnCheckedChangeListener(new CheckZoomPlusFlagListener());
	}


	private final class CheckMiddleLineDrawFlagListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				/* 中間線を表示設定にする */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("scale_middle_line", true);
				editor.commit();
			} else {
				/* 中間線を非表示設定にする */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("scale_middle_line", false);
				editor.commit();
			}
		}
	}


	private final class CheckZoomFlagListener implements OnCheckedChangeListener {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				/* 広角レンズ補正を行う */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("zoom_flag", true);
				editor.commit();
				checkZoomPlusFlag.setEnabled(true);
			} else {
				/* 広角レンズ補正を行わない */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("zoom_flag", false);
				editor.commit();
				checkZoomPlusFlag.setEnabled(false);
			}
		}
	}


	private final class CheckZoomPlusFlagListener implements OnCheckedChangeListener {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				/* 広角レンズ補正：プラス */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("zoom_plus_flag", true);
				editor.commit();
			} else {
				/* 広角レンズ補正:標準 */
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putBoolean("zoom_plus_flag", false);
				editor.commit();
			}
		}

	}


	private final class ScaleAspectSelectedListener implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			Spinner spinner = (Spinner) parent;

			switch (spinner.getSelectedItemPosition()) {
			case (0): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putFloat("scale_aspect", 1.1666f); // 10F
				editor.putInt("scale_aspect_select", 0);
				editor.commit();
				Log.d("Dessin-Scale", "1.1666fでスケールをセット");
			}
				break;

			case (1): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putFloat("scale_aspect", 1.3611f); // 10P
				editor.putInt("scale_aspect_select", 1);
				editor.commit();
				Log.d("Dessin-Scale", "1.3611fでスケールをセット");
			}
				break;

			case (2): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putFloat("scale_aspect", 1.5555f); // 10M
				editor.putInt("scale_aspect_select", 2);
				editor.commit();
				Log.d("Dessin-Scale", "1.5555fでスケールをセット");
			}
				break;

			case (3): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putFloat("scale_aspect", 1.4142f); // ISO216
				editor.putInt("scale_aspect_select", 3);
				editor.commit();
				Log.d("Dessin-Scale", "1.4142fでスケールをセット");
			}
				break;

			case (4): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putFloat("scale_aspect", 1.3333f); // 木炭紙
				editor.putInt("scale_aspect_select", 4);
				editor.commit();
				Log.d("Dessin-Scale", "1.3333fでスケールをセット");
			}
				break;

			default:
				Log.d("Dessin-Scale", "該当項目が有りませんでした");
				break;
			}
		}


		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	}


	private final class ScaleColorSelectedListener implements AdapterView.OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			Spinner spinner = (Spinner) parent;

			switch (spinner.getSelectedItemPosition()) {
			case (0): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 0);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Whiteでセット");
			}
				break;

			case (1): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 1);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Blackでセット");
			}
				break;

			case (2): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 2);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Redでセット");
			}
				break;

			case (3): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 3);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Greenでセット");
			}
				break;

			case (4): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 4);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Blueでセット");
			}
				break;

			case (5): {
				SharedPreferences preference = getSharedPreferences("scale_config", MODE_PRIVATE);
				Editor editor = preference.edit();
				editor.putInt("scale_color_select", 5);
				editor.commit();
				Log.d("Dessin-Scale", "Color:Shadowでセット");
			}
				break;

			default:
				Log.d("Dessin-Scale", "該当項目が有りませんでした");
				break;
			}
		}


		@Override
		public void onNothingSelected(AdapterView<?> parent) {
		}
	}
}
