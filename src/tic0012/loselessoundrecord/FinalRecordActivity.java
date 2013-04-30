package tic0012.loselessoundrecord;

import java.io.File;

import tic0012.loselessoundrecord.classes.UncompressedAudioRecorder;
import tic0012.loselessoundrecord.model.SDCardException;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Starting activity
 * 
 * @author tic0012, Michal Tichý
 */
public class FinalRecordActivity extends BaseActivity {
	/**
	 * Recording Thread
	 */
	private Thread recordingThread = null;

	/**
	 * WAV recorder
	 */
	private UncompressedAudioRecorder myRecorder = null;

	/**
	 * START/STOP recording button
	 */
	private ToggleButton toggleButton;

	/**
	 * Last recorded FilePath
	 */
	private String recordedFilePath = null;

	/**
	 * TimePicker dialog
	 */
	private TimePickerDialog timePickerDialog;

	/**
	 * TimePicker dialog for random recording
	 */
	private TimePickerDialog randomTimePickerDialogDown;

	/**
	 * TimePicker dialog for random recording
	 */
	private TimePickerDialog randomTimePickerDialogUp;

	/**
	 * Recording starts after this time in ms
	 */
	private long startAfter;

	/**
	 * Recording starts randomly after this time in ms
	 */
	private long randomRecordStartFrom;

	/**
	 * Recording starts randomly before this time in ms
	 */
	private long randomRecordEndTo;

	/**
	 * Timer for recording after time
	 */
	private CountDown recordTimer;
	
	/**
	 * Timer for recording after time
	 */
	private RandomCountDown randomTimer;

	/**
	 * Dialog for showing remaining time
	 */
	private AlertDialog countDownDialog;

	/**
	 * TextView inside countDownDialog
	 */
	private TextView countDownTextView;
	
	/**
	 * Dialog for information about random recording
	 */
	private AlertDialog randomRecordDialog;			
	
	/**
	 * If true, recorded sound will be stored in public Music folder
	 */
	private boolean saveToMusicDir;
	
	/**
	 * Auto stop recording after time
	 */
	private int autoStopTime;
	
	/**
	 * Timer for auto stop recording
	 */
	private StopTimer stopTimer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_final_record);							
				
		// work with my recording class
		this.myRecorder = new UncompressedAudioRecorder();	
		
		// set toggle button OnClick listener
		this.toggleButton = ((ToggleButton) findViewById(R.id.recordControllButton));
		this.toggleButton.setOnClickListener(this.recordButtonClickListener);		

		// create Timer dialog
		this.timePickerDialog = new TimePickerDialog(this,
				this.timePickerListener, 0, 0, true);
		this.timePickerDialog.setCancelable(true);
		this.timePickerDialog.setTitle(R.string.dialog_title_time_setup);
				
		// create countDown dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title_time_setup);
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_count_down, null);
		builder.setView(view);
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				});
		countDownTextView = (TextView) view
				.findViewById(R.id.dialog_countdown_text);
		this.countDownDialog = builder.create();
		this.countDownDialog
				.setOnDismissListener(this.timerDialogDismissListener);

		// create Random Timer dialog
		this.randomTimePickerDialogDown = new TimePickerDialog(this,
				this.downTimePickerListener, 0, 0, true);
		this.randomTimePickerDialogDown.setCancelable(true);
		this.randomTimePickerDialogDown
				.setTitle(R.string.dialog_title_time_select);
		this.randomTimePickerDialogDown
				.setMessage(this.getResources().getString(
						R.string.dialog_title_random_time_down_select_descr));

		this.randomTimePickerDialogUp = new TimePickerDialog(this,
				this.upTimePickerListener, 0, 0, true);
		this.randomTimePickerDialogUp.setCancelable(true);
		this.randomTimePickerDialogUp
				.setTitle(R.string.dialog_title_time_select);
		this.randomTimePickerDialogUp.setMessage(this.getResources().getString(
				R.string.dialog_title_random_time_up_select_descr));		
	
		builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.dialog_shoot_after_sound);		
		this.randomRecordDialog = builder.create();
		this.randomRecordDialog.setOnCancelListener(this.randomRecordCancelListener);
		this.randomRecordDialog.setOnDismissListener(this.randomRecordDismissListener);	
	}

	@Override
	protected void onPause() {
		super.onPause();

		this.countDownDialog.dismiss();
		this.randomRecordDialog.dismiss();
		
		// screen can turn off now
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if(!hasFocus){
			this.countDownDialog.dismiss();
			this.randomRecordDialog.dismiss();				
			
			// screen can turn off now
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}		
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_final_record, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_set_timer: // timer selection
			this.timePickerDialog.show();
			return true;
		case R.id.menu_set_random_timer:
			this.randomTimePickerDialogDown.show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * TimePicker listener
	 */
	private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int selectedHour,
				int selectedMinute) {

			if (selectedHour == 0 && selectedMinute == 0) { // start record
															// right now
				countDownDialog.dismiss();

				try {
					toggleButton.setChecked(true);
					startRecording();
				} catch (SDCardException e) {
					toggleButton.setChecked(false);
					// show info
					Toast.makeText(getApplicationContext(),
							R.string.no_sd_card, Toast.LENGTH_LONG).show();

					e.printStackTrace();
				}

				return;
			}

			// get time in ms
			startAfter = (selectedHour * 60 * 60 * 1000)
					+ (selectedMinute * 60 * 1000);

			recordTimer = new CountDown(startAfter, 1000);
			recordTimer.start();

			// clear time
			view.setCurrentHour(0);
			view.setCurrentMinute(0);

			// show countDown dialog
			countDownTextView.setText(selectedHour + ":" + selectedMinute);
			countDownDialog.show();
		}
	};

	/**
	 * Random Down time picker listener
	 */
	private TimePickerDialog.OnTimeSetListener downTimePickerListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int selectedHour,
				int selectedMinute) {
			randomRecordStartFrom = (selectedHour * 60 * 60 * 1000)
					+ (selectedMinute * 60 * 1000);

			// clear time
			view.setCurrentHour(0);
			view.setCurrentMinute(0);

			randomTimePickerDialogUp.updateTime(selectedHour, selectedMinute + 1);
			randomTimePickerDialogUp.show();
		}
	};

	/**
	 * Random Up time picker listener
	 */
	private TimePickerDialog.OnTimeSetListener upTimePickerListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int selectedHour,
				int selectedMinute) {
			randomRecordEndTo = (selectedHour * 60 * 60 * 1000)
					+ (selectedMinute * 60 * 1000);

			// clear time
			view.setCurrentHour(0);
			view.setCurrentMinute(0);
						
			if (randomRecordEndTo <= randomRecordStartFrom) { // start record
				// right now
				try {
					toggleButton.setChecked(true);
					startRecording();
				} catch (SDCardException e) {
					toggleButton.setChecked(false);
					// show info
					Toast.makeText(getApplicationContext(),
							R.string.no_sd_card, Toast.LENGTH_LONG).show();

					e.printStackTrace();
				}

				return;
			}						
			
			startAfter = randomRecordStartFrom + (long)(Math.random() * ((randomRecordEndTo - randomRecordStartFrom) + 1));

			Log.d("down limit: ", "" + randomRecordStartFrom);
			Log.d("rup limit: ", "" + randomRecordEndTo);
			Log.d("records start after: ", "" + startAfter);
			
			randomTimer = new RandomCountDown(startAfter, 1000);
			randomTimer.start();

			// show dialog	
			randomRecordDialog.show();	
		}
	};

	/**
	 * Timer Dialog dismiss handling
	 */
	private DialogInterface.OnDismissListener timerDialogDismissListener = new DialogInterface.OnDismissListener() {
		
		public void onDismiss(DialogInterface dialog) {
			if (recordTimer != null) {
				recordTimer.cancel();
			}
			
			// screen can turn off now
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	};

	/**
	 * ToggleButton OnClick Listener
	 */
	private View.OnClickListener recordButtonClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			ToggleButton button = (ToggleButton) v;
			boolean on = button.isChecked();

			if (on) { // start recording
				try {
					startRecording();
				} catch (SDCardException e) { // SD card error
					// show info
					Toast.makeText(getApplicationContext(),
							R.string.no_sd_card, Toast.LENGTH_LONG).show();

					button.setChecked(false);
					e.printStackTrace();
				}

			} else { // stop recording
				if(stopTimer != null){
					stopTimer.cancel();
				}
				
				recordedFilePath = myRecorder.stopRecording();
										
				// Add record to media scanner
				if(saveToMusicDir){
					File file = new File(recordedFilePath);			
					String[] filesToScan = new String[]{ file.getPath() };
					String[] mimeTypes = new String[]{ "audio/wav", "audio/x-wav" };
					MediaScannerConnection.scanFile(getApplicationContext(), filesToScan, mimeTypes, null);	
				}				
				
				// TODO: remove after debug
				//recordedFilePath = getSavePath(true) + "/four_realshot.wav";
				//recordedFilePath = getSavePath(true) + "/one_real_shot.wav";
				//recordedFilePath = getSavePath(true) + "/real_short.wav";
				//recordedFilePath = getSavePath(true) + "/real_long.wav";
				//recordedFilePath = getSavePath(true) + "/sound_7.wav";
				
				// change activity
				setReadActivity(recordedFilePath);
			}
		}
	};
	
	/**
	 * Random record dialog dismiss listener
	 */
	private DialogInterface.OnDismissListener randomRecordDismissListener = new DialogInterface.OnDismissListener() {

		public void onDismiss(DialogInterface dialog) {
			if (randomTimer != null) {
				randomTimer.cancel();
			}
			
			// screen can turn off now
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	};
	
	private DialogInterface.OnCancelListener randomRecordCancelListener = new DialogInterface.OnCancelListener() {

		public void onCancel(DialogInterface dialog) {
			dialog.dismiss();			
		}
	};
	

	/**
	 * Start recording
	 * 
	 * @throws SDCardException
	 */
	private void startRecording() throws SDCardException {
		this.saveToMusicDir = this.preferences.getBoolean(this.SAVE_INTO_MUSIC_FOLDER, this.getResources().getBoolean(R.bool.record_into_music_folder));
		final String savePath = this.getSavePath(saveToMusicDir);		
		Log.d("savePath", savePath);	
		
		// get auto stop time from preff.
		this.autoStopTime = this.preferences.getInt(this.CONFIG_AUTO_STOP_TIME_KEY, this.getResources().getInteger(R.integer.auto_stop_time));

		if(this.autoStopTime > 0){
			this.stopTimer = new StopTimer((this.autoStopTime * 1000), 1000);	
			this.stopTimer.start();
		}
		
		Log.i("auto stop", "" + this.autoStopTime);
		
		// screen can turn off now
		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) { // SD card is
														// not ready
			throw new SDCardException("SD card is not mounted");
		}

		this.recordingThread = new Thread(new Runnable() {

			public void run() {
				Log.d("recorder", "Start Recording");
				myRecorder.startRecording(savePath);
			}
		}, "AudioRecorder Thread");

		this.recordingThread.start();
	}

	/**
	 * Change activity to Read
	 * 
	 * @param filePath
	 *            absolute path to recorder file
	 */
	private void setReadActivity(String filePath) {
		Intent intent = new Intent(this.getApplicationContext(),
				ReadActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

		intent.putExtra(this.FILEPATH_FLAG, filePath);

		this.startActivity(intent);
	}

	/**
	 * Start timed recording
	 */
	protected class CountDown extends CountDownTimer {

		public CountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			
			// prevent screen turn off
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		@Override
		public void onFinish() {
			countDownDialog.dismiss();

			try {
				ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM,
						100);
				tg.startTone(ToneGenerator.TONE_DTMF_6, 700);

				toggleButton.setChecked(true);
				startRecording();
			} catch (SDCardException e) {
				toggleButton.setChecked(false);
				// show info
				Toast.makeText(getApplicationContext(), R.string.no_sd_card,
						Toast.LENGTH_LONG).show();

				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onTick(long millisUntilFinished) {
			long hours, minutes, seconds;
			hours = millisUntilFinished / 60 / 60 / 1000;
			minutes = (millisUntilFinished / 1000 / 60) - (hours * 60);
			seconds = (millisUntilFinished / 1000) - (minutes * 60)
					- (hours * 60 * 60);

			if (hours == 0 && minutes == 0 && seconds < 10) {
				countDownTextView.setTextColor(Color.RED);
			}

			countDownTextView.setText(hours + ":" + minutes + ":" + seconds);
		}
	}
	
	/**
	 * Start random timed recording
	 */
	protected class RandomCountDown extends CountDownTimer {

		public RandomCountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			
			// prevent screen turn off
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		@Override
		public void onFinish() {
			randomRecordDialog.dismiss();

			try {
				ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM,
						100);
				tg.startTone(ToneGenerator.TONE_DTMF_6, 700);

				toggleButton.setChecked(true);
				startRecording();
			} catch (SDCardException e) {
				toggleButton.setChecked(false);
				// show info
				Toast.makeText(getApplicationContext(), R.string.no_sd_card,
						Toast.LENGTH_LONG).show();

				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public void onTick(long arg0) {			
		}
	}
		
	/**
	 * Get save path for gunShots
	 * @return
	 */
	private String getSavePath(boolean saveToMusicDir){		
		String filePath = null;
		String folderName = this.getResources().getString(R.string.save_folder);
		File folder;			
		
		if(saveToMusicDir){ // prepare folder in shared music folder	
			folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC) + "/" + folderName);									
			if(!folder.exists()){
				folder.mkdirs();
			}	
			filePath = folder.getPath();
		}
		else{ // prepare folder in application external directory
			filePath =  this.getExternalFilesDir(null).getPath();			
		}
				
		return filePath;
	}
	
	/**
	 * Stop recording
	 */
	protected class StopTimer extends CountDownTimer {

		public StopTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			
			// prevent screen turn off
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		@Override
		public void onFinish() {						
			toggleButton.performClick();
		}

		@Override
		public void onTick(long arg0) {	
			Log.i("timer tick", "ticked");
		}
	}
}
