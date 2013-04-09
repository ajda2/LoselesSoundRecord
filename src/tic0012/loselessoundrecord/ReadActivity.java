package tic0012.loselessoundrecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import tic0012.loselessoundrecord.classes.AmplitudePrinter;
import tic0012.loselessoundrecord.classes.Category;
import tic0012.loselessoundrecord.classes.RecordReadTask;
import tic0012.loselessoundrecord.classes.Gunshot;
import tic0012.loselessoundrecord.classes.Record;
import tic0012.loselessoundrecord.model.CategoryModel;
import tic0012.loselessoundrecord.model.DBException;
import tic0012.loselessoundrecord.model.RecordModel;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Reading WAV file activity
 * 
 * @author tic0012, Michal Tichý
 */
public class ReadActivity extends BaseActivity {

	/**
	 * All RAW amplitudes from WAF file
	 */
	private ArrayList<Short> amplitudes = null;

	/**
	 * Lists of times when was fired
	 */
	private ArrayList<Float> gunShotsTimes = null;

	/**
	 * List of gunShots indexes(sample number)
	 */
	private ArrayList<Integer> gunshotSamples;

	/**
	 * Asynchronous WAV file reading and Amplitude display
	 */
	private RecordReadTask task = null;

	/**
	 * Path to WAV file
	 */
	private String filePath = null;

	/**
	 * Key to retrieve stored amplitudes
	 */
	private final String AMPLITUDES_STORED_KEY = "amplitudes";

	/**
	 * Key to retrieve stored gunShots times
	 */
	private final String GUNSHOT_TIMES_STORED_KEY = "gunShotsTimes";

	/**
	 * Key to retrieve stored gunShots samples indexes
	 */
	private final String GUNSHOT_SAMPLES_STORED_KEY = "gunshotSamples";

	private final String PICTURE_VISIBLE_KEY = "pviture_visible";

	/**
	 * List for gunShots editing
	 */
	private ListView listView;

	/**
	 * Sample rate in Hz
	 */
	private final int SAMPLE_RATE = 44100;

	/**
	 * Printer to show sound picture
	 */
	private AmplitudePrinter printer;

	/**
	 * Image View where to show amplitude
	 */
	private ImageView imageView;

	private RecordModel recordModel;

	private Button saveButton;

	/**
	 * Visible status of image
	 */
	private boolean pictureVisible = true;

	private CategoryModel categoryModel;

	private ArrayList<Category> categories;

	private AlertDialog noGunShotDialog;

	/**
	 * Dialog for reading progress
	 */
	AlertDialog progressDialog;

	/**
	 * WAF file processing progress bar
	 */
	private ProgressBar progressBar;

	/**
	 * Dialog for adding gunShot
	 */
	private AlertDialog addGunShotDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read);

		// create no gunShot found dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.no_gunshot);
		builder.setPositiveButton(R.string.add_gunshot,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						addGunShotDialog.show();
					}

				});
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						if (!pictureVisible) {
							setActivity(FinalRecordActivity.class, true);
						}
					}

				});
		this.noGunShotDialog = builder.create();
		this.noGunShotDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						if (!pictureVisible) {
							setActivity(FinalRecordActivity.class, true);
						}
					}
				});

		// create progress dialog
		builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sound_processing);
		LayoutInflater inflater = LayoutInflater.from(this);
		View progresDialogView = inflater.inflate(R.layout.dialog_wav_reading,
				null);
		builder.setView(progresDialogView);
		builder.setCancelable(true);
		builder.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		this.progressDialog = builder.create();
		this.progressDialog
				.setOnCancelListener(this.progressDialogDismissListener);

		// create ad gunShot dialog
		this.createAddDialog();

		// set default value for picture visible
		this.pictureVisible = this.preferences.getBoolean(
				this.CONFIG_SHOW_IMAGE_KEY,
				this.getResources().getBoolean(R.bool.show_image));

		// get ListView for results
		this.listView = (ListView) this.findViewById(R.id.shotsList);
		this.listView.setVisibility(View.GONE);

		this.saveButton = (Button) this.findViewById(R.id.saveResultButton);
		this.saveButton.setVisibility(View.GONE);

		// prepare ImageView and progressBar for canvas
		this.progressBar = (ProgressBar) progresDialogView
				.findViewById(R.id.readingProgressBar);
		this.imageView = (ImageView) this.findViewById(R.id.imageView1);
		this.imageView.setVisibility(View.GONE);

		// prepare shot icon for canvas
		Bitmap crosshair = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.crosshair2);

		// setup display height
		Display display = getWindowManager().getDefaultDisplay();

		int printerSampleRate = this.SAMPLE_RATE;
		if (this.getResources().getBoolean(R.bool.compression)) {
			printerSampleRate = printerSampleRate
					/ this.getResources().getInteger(
							R.integer.compression_ratio);
		}
		this.printer = new AmplitudePrinter(printerSampleRate, crosshair,
				(display.getHeight() - 80), this.getResources().getInteger(
						R.integer.max_bitmap_seconds), this.getResources()
						.getInteger(R.integer.bitmap_ampl_stretch));

		// try to get filePath and stored points
		if (savedInstanceState != null) { // activity is already running
			this.amplitudes = ((ArrayList<Short>) savedInstanceState
					.getSerializable(this.AMPLITUDES_STORED_KEY));
			this.gunShotsTimes = (ArrayList<Float>) savedInstanceState
					.getSerializable(this.GUNSHOT_TIMES_STORED_KEY);
			this.gunshotSamples = (ArrayList<Integer>) savedInstanceState
					.getSerializable(this.GUNSHOT_SAMPLES_STORED_KEY);

			this.filePath = savedInstanceState.getString(this.FILEPATH_FLAG);

			this.pictureVisible = savedInstanceState.getBoolean(
					this.PICTURE_VISIBLE_KEY, true);
		} else { // new activity intent
			Bundle extras = this.getIntent().getExtras();

			if (extras == null) { // filePath is not set
				this.filePath = null;

				// start parent activity
				this.setActivity(FinalRecordActivity.class, true);
			} else {
				this.filePath = extras.getString(this.FILEPATH_FLAG);
			}
		}

		// set SaveButton listener
		((Button) findViewById(R.id.saveResultButton))
				.setOnClickListener(saveButtonClickListener);

		this.recordModel = RecordModel
				.getInstance(this.getApplicationContext());

		// Setup asynchronous file reading
		int sensitivy = this.preferences.getInt(this.CONFIG_SENSITIVITY_KEY, this.getResources().getInteger(R.integer.gunshot_sensitivity));
		int compressionRatio = this.getResources().getInteger(R.integer.compression_ratio);
		boolean compression = this.getResources().getBoolean(R.bool.compression);
		this.task = new RecordReadTask(this.SAMPLE_RATE, this.progressBar,
				this.filePath, this, compression, compressionRatio,
				sensitivy);

		// start asynchronous rendering
		if (this.amplitudes == null) {
			// lock screen orientation
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state)
					|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) { // SD card is ready
				
				File file = new File(this.filePath);
				if(!file.exists()){ // file dos not exists
					Toast.makeText(this.getApplicationContext(), R.string.file_not_found, Toast.LENGTH_SHORT).show();
					this.setActivity(FinalRecordActivity.class, true);
				}
				
				// run new WAV file reading
				this.task.execute();				
				this.progressDialog.show();
				
				// prevent screen turn off
				this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else { // SD card is not ready
				Toast.makeText(this.getApplicationContext(),
						R.string.no_sd_card, Toast.LENGTH_LONG).show();
			}

		} else {
			// retrieve stored data and display it
			this.publishResult(false);
		}

		this.registerForContextMenu(this.imageView);

		this.categoryModel = CategoryModel.getInstance(this
				.getApplicationContext());
		this.categories = this.categoryModel.getAll();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_read, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_hide_graph:
			this.hidePicture();

			return true;

		case R.id.menu_add_gunshot:
			this.addGunShotDialog.show();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		if (task != null && this.amplitudes == null) {
			this.amplitudes = task.getResult();
		}

		// make amplitudes persistent
		outState.putSerializable(this.AMPLITUDES_STORED_KEY, this.amplitudes);
		outState.putSerializable(this.GUNSHOT_TIMES_STORED_KEY,
				this.gunShotsTimes);
		outState.putSerializable(this.GUNSHOT_SAMPLES_STORED_KEY,
				this.gunshotSamples);

		// save filePath
		outState.putString(this.FILEPATH_FLAG, this.filePath);

		// save image state
		outState.putBoolean(this.PICTURE_VISIBLE_KEY, this.pictureVisible);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.noGunShotDialog != null)
			this.noGunShotDialog.dismiss();

		if (this.progressDialog != null) {
			this.progressDialog.dismiss();
		}

		if (this.addGunShotDialog != null) {
			this.addGunShotDialog.dismiss();
		}
	}

	public void setResults(ArrayList<Short> ampl, ArrayList<Float> shotsTime,
			ArrayList<Integer> samples) {
		this.amplitudes = ampl;
		this.gunShotsTimes = shotsTime;
		this.gunshotSamples = samples;

		this.publishResult(true);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();

		if (R.id.imageView1 == v.getId()) {
			inflater.inflate(R.menu.context_menu_image, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.context_menu_hide_picture:
			this.hidePicture();
			return true;
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * Publish result obtained from WAV reading
	 * 
	 * @param showMessage
	 *            Show dialog if no gunShots found
	 */
	private void publishResult(boolean showMessage) {
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int screenDensity = metrics.densityDpi;
		
		// screen can turn off now
		this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// remove WAV file if configured
		boolean deleteWav = this.preferences.getBoolean(this.CONFIG_DELETE_WAV,
				this.getResources().getBoolean(R.bool.delete_wav_after_read));
		if (deleteWav) {
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) { // SD card is ready
				File file = new File(this.filePath);						
				file.delete();
				
				boolean saveToMusicDir = this.preferences.getBoolean(this.SAVE_INTO_MUSIC_FOLDER, this.getResources().getBoolean(R.bool.record_into_music_folder));			
				// remove sound from Media index
				if(saveToMusicDir){					
					String filePath = "file:/" + file.getPath();
					this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse(filePath)) ); 	
				}				
			}						
		}

		// user information
		Toast toast = Toast.makeText(this, R.string.creating_image,
				Toast.LENGTH_SHORT);
		toast.show();

		// show picture
		if (this.pictureVisible) {
			Bitmap picture = this.printer.printAmplitudes(this.amplitudes,
					this.gunshotSamples, screenDensity);
			this.imageView.setImageBitmap(picture);
			this.imageView.setVisibility(View.VISIBLE);
		}

		// setup GunShots into list
		this.setListView();
		this.listView.setVisibility(View.VISIBLE);

		// hide progress dialog
		this.progressDialog.hide();
		toast.cancel();

		// unlock screen orientation
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

		if (this.gunShotsTimes.size() == 0) {
			this.showNoGunShotDialog();
		} else {
			this.saveButton.setVisibility(View.VISIBLE);
		}

		/*
		 * if (this.gunShotsTimes.size() == 0 && !this.pictureVisible) { Toast
		 * noGunShotToast = Toast.makeText(getApplicationContext(),
		 * R.string.no_gunshot, Toast.LENGTH_LONG); noGunShotToast.show();
		 * this.setActivity(FinalRecordActivity.class, true); } else if
		 * (this.gunShotsTimes.size() == 0) { if (showMessage) {
		 * this.showNoGunShotDialog(); } } else {
		 * this.saveButton.setVisibility(View.VISIBLE); }
		 */
	}

	private void setListView() {
		// setup GunShots into list
		String[] values = new String[this.gunShotsTimes.size()];
		for (int i = 0; i < this.gunShotsTimes.size(); i++) { // check all
																// gunShots
			values[i] = this.gunShotsTimes.get(i).toString() + " s";
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, values); // simple_list_item_1
		this.listView.setAdapter(adapter);
		this.listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		for (int i = 0; i < this.listView.getCount(); i++) { // check all
																// gunShots
			this.listView.setItemChecked(i, true);
		}
	}

	/**
	 * Save founded gunShots
	 */
	private View.OnClickListener saveButtonClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			// long categoryId = 1;
			// create category picker dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(
					v.getContext());
			builder.setTitle(R.string.choose_category);
			builder.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			String[] items = new String[categories.size()];
			for (int i = 0; i < categories.size(); i++) {
				items[i] = categories.get(i).name;
			}

			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					long categoryId = categories.get(which).getId();
					Log.d("selected category", "" + categoryId);

					SparseBooleanArray sparseBooleanArray = listView
							.getCheckedItemPositions();
					// save record into DB
					Record newRecord = new Record(0, new ArrayList<Gunshot>(),
							new Date(), categoryId);

					// save gunShots into DB
					for (int i = 0; i < gunShotsTimes.size(); i++) {
						if (sparseBooleanArray.get(i) == true) {
							Gunshot gunShot = new Gunshot(0, gunShotsTimes
									.get(i), 0);
							newRecord.gunshots.add(gunShot);
						}
					}

					// save record into DB
					try {
						recordModel.add(newRecord);
						Toast.makeText(getApplicationContext(),
								R.string.gunshots_saved, Toast.LENGTH_SHORT)
								.show();
						setActivity(FinalRecordActivity.class, true);
					} catch (DBException e) {
						Toast.makeText(getApplicationContext(),
								R.string.db_insert_error, Toast.LENGTH_SHORT)
								.show();

						e.printStackTrace();
					}
				}
			});
			AlertDialog CategorySelectDialog = builder.create();
			CategorySelectDialog.show();
		}
	};

	/**
	 * Hide picture
	 */
	private void hidePicture() {
		this.imageView.setVisibility(View.GONE);
		this.pictureVisible = false;

		if (this.gunShotsTimes.size() == 0) {
			this.setActivity(FinalRecordActivity.class, true);
		}
	}

	/**
	 * Dialog information - no gunShots
	 */
	private void showNoGunShotDialog() {
		this.noGunShotDialog.show();
	}

	/**
	 * Handle reading dialog cancel
	 */
	private DialogInterface.OnCancelListener progressDialogDismissListener = new DialogInterface.OnCancelListener() {

		public void onCancel(DialogInterface dialog) {
			// stop reading task
			task.cancel(true);
			setActivity(FinalRecordActivity.class, true);
		}
	};

	/**
	 * Show and handle gunShot adding dialog
	 */
	private void createAddDialog() {
		AlertDialog.Builder addGunShotBuilder = new AlertDialog.Builder(this);
		addGunShotBuilder.setTitle(R.string.add_gunshot);

		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_add_gunshot, null);
		addGunShotBuilder.setView(view);

		final EditText timeEditText = (EditText) view
				.findViewById(R.id.timeEditText);

		addGunShotBuilder.setPositiveButton(R.string.save,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String textValue = timeEditText.getText().toString();
						if (textValue.equals("")) {
							return;
						}
						float time = Float.valueOf(textValue);
						gunShotsTimes.add(time);
						Collections.sort(gunShotsTimes);
						setListView();
						saveButton.setVisibility(View.VISIBLE);
					}
				});

		this.addGunShotDialog = addGunShotBuilder.create();
		this.addGunShotDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {

					public void onCancel(DialogInterface dialog) {
						if (gunShotsTimes.size() == 0 && !pictureVisible) {
							setActivity(FinalRecordActivity.class, true);
						}
					}
				});
	}
}
