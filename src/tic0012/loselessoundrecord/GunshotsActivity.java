package tic0012.loselessoundrecord;

import tic0012.loselessoundrecord.classes.Gunshot;
import tic0012.loselessoundrecord.classes.Record;
import tic0012.loselessoundrecord.model.DBException;
import tic0012.loselessoundrecord.model.GunshotModel;
import tic0012.loselessoundrecord.model.RecordModel;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.DateFormat;

/**
 * Stored gunShots list activity
 * 
 * @author tic0012, Michal Tichý
 */
public class GunshotsActivity extends BaseActivity {

	/**
	 * Record to show
	 */
	private Record record = null;

	private RecordModel recordModel;

	private GunshotModel gunShotModel;

	private ListView gunShotsListView;

	private ArrayAdapter<String> adapter;

	/**
	 * Dialog for adding gunShot
	 */
	private AlertDialog adGunShotDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gunshots);

		long recordId = 0;

		this.createAddDialog();

		// try to get filePath and stored points
		if (savedInstanceState != null) { // activity is already running
			recordId = savedInstanceState.getLong(BaseActivity.RECORD_ID_FLAG);

		} else { // new activity intent
			Bundle extras = getIntent().getExtras();

			if (extras == null) { // Record ID is not set
				// start parent activity
				this.setActivity(FinalRecordActivity.class, true);
			} else {
				recordId = extras.getLong(BaseActivity.RECORD_ID_FLAG);
			}
		}

		this.recordModel = RecordModel
				.getInstance(this.getApplicationContext());
		this.gunShotModel = GunshotModel.getInstance(this
				.getApplicationContext());
		this.record = this.recordModel.get(recordId);
		this.gunShotsListView = (ListView) this
				.findViewById(R.id.gunsotsListView);
		TextView textView = (TextView) this.findViewById(R.id.recordNameText);

		// set name of Record
		textView.setText(DateFormat.format("d. M. yyyy k:mm",
				this.record.dateRecorderd));

		// setup GunShots into list
		this.setUpListView();
	}

	@Override
	public void onPause() {
		super.onPause();

		if (this.adGunShotDialog != null) {
			this.adGunShotDialog.dismiss();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_gunshots, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_delete_gunshots:
			this.deleteSelected();
			this.refreshListView();

			return true;

		case R.id.menu_sellect_all_gunshots:
			for (int i = 0; i < gunShotsListView.getCount(); i++) { // check all
																	// gunShots
				this.gunShotsListView.setItemChecked(i, true);
			}

			return true;

		case R.id.menu_record_share:
			this.shareGunshots();
			return true;
		case R.id.menu_add_gunshot:
			this.adGunShotDialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		// make record persistent
		outState.putLong(BaseActivity.RECORD_ID_FLAG, this.record.getId());
	}

	/**
	 * Setup Values into List View
	 */
	private void setUpListView() {
		String[] values = new String[this.record.gunshots.size()];
		for (int i = 0; i < this.record.gunshots.size(); i++) { // check all
																// gunShots
			values[i] = this.record.gunshots.get(i).time + " s";
		}

		this.adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, values); // simple_list_item_1
		this.gunShotsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		this.gunShotsListView.setAdapter(this.adapter);

		for (int i = 0; i < gunShotsListView.getCount(); i++) {
			this.gunShotsListView.setItemChecked(i, false);
		}
	}

	/**
	 * Refresh values in List View
	 */
	private void refreshListView() {
		if (this.record == null) {
			return;
		}

		this.record = this.recordModel.get(this.record.getId());
		this.setUpListView();
	}

	/**
	 * Delete selected gunShots
	 */
	private void deleteSelected() {
		SparseBooleanArray sparseBooleanArray = this.gunShotsListView
				.getCheckedItemPositions();

		// remove gunShots
		for (int i = 0; i < sparseBooleanArray.size(); i++) {
			if (sparseBooleanArray.get(i) == true) {
				this.gunShotModel.remove(this.record.gunshots.get(i));
			}
		}
	}

	/**
	 * Share gunShots
	 */
	private void shareGunshots() {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		String shareBody = this.getResources().getString(R.string.my_gunshots)
				+ " " + this.getResources().getString(R.string.from) + " "
				+ this.recordModel.getShareText(this.record);
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this
				.getResources().getString(R.string.my_gunshots));

		Log.d("Share body", shareBody);

		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		startActivity(Intent.createChooser(sharingIntent, this.getResources()
				.getString(R.string.share)));
	}

	/**
	 * Show and handle gunShot adding dialog
	 */
	private void createAddDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.add_gunshot);

		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_add_gunshot, null);
		builder.setView(view);

		final EditText timeEditText = (EditText) view
				.findViewById(R.id.timeEditText);

		builder.setPositiveButton(R.string.save,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String textValue = timeEditText.getText().toString();
						if (textValue.equals("")) {
							return;
						}
						float time = Float.valueOf(textValue);
						Gunshot newGunShot = gunShotModel.gunShotFactory(0,
								time, record.getId());
						try {
							gunShotModel.add(newGunShot);
							refreshListView();
							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.gunshot_added, Toast.LENGTH_SHORT);
							toast.show();
						} catch (DBException e) {
							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.db_insert_error,
									Toast.LENGTH_SHORT);
							toast.show();

							e.printStackTrace();
						}
					}
				});

		this.adGunShotDialog = builder.create();
		// addGunshotDialog.show();
	}

}
