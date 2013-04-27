package tic0012.loselessoundrecord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import tic0012.loselessoundrecord.classes.Category;
import tic0012.loselessoundrecord.classes.CategoryListAdapter;
import tic0012.loselessoundrecord.classes.Record;
import tic0012.loselessoundrecord.model.CannotDeleteException;
import tic0012.loselessoundrecord.model.CategoryModel;
import tic0012.loselessoundrecord.model.DBException;
import tic0012.loselessoundrecord.model.RecordModel;
import tic0012.loselessoundrecord.model.SDCardException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.support.v4.app.NavUtils;
import android.widget.ExpandableListView.OnChildClickListener;

/**
 * List of all stored categories and records in DB
 * 
 * @author tic0012, Michal Tichý
 */
//public class StoredActivity extends ExpandableListActivity {
public class StoredActivity extends BaseActivity {

	/**
	 * Category adapter for expandable list
	 */
	private ExpandableListAdapter mAdapter;

	private CategoryModel categoryModel;

	private RecordModel recordModel;

	/**
	 * All categories
	 */
	private ArrayList<Category> categories;

	/**
	 * Categories ListView
	 */
	private ExpandableListView listView;

	/**
	 * Category to edit
	 */
	private Category categoryEdit;

	/**
	 * Folder where save XML export
	 */
	private String EXPORT_DIR;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stored);
		
		this.EXPORT_DIR = this.getResources().getString(R.string.save_folder);		
				

		this.categoryModel = CategoryModel.getInstance(this
				.getApplicationContext());
		this.recordModel = RecordModel
				.getInstance(this.getApplicationContext());
		this.categories = this.categoryModel.getAll();

		// set data into list
		this.setUpView();

		this.registerForContextMenu(this.listView);
	}

	@Override
	/**
	 * Create context menus
	 */
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);

		MenuInflater inflater = getMenuInflater();
		ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
		int itemType = ExpandableListView
				.getPackedPositionType(info.packedPosition);

		if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			inflater.inflate(R.menu.context_menu_category, menu);
		} else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			inflater.inflate(R.menu.context_menu_record, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item
				.getMenuInfo();

		int groupPos, childPos;
		int itemType = ExpandableListView
				.getPackedPositionType(info.packedPosition);

		groupPos = ExpandableListView
				.getPackedPositionGroup(info.packedPosition);
		Log.d("selected position: ", groupPos + "");

		// Category Selected
		if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			switch (item.getItemId()) {
			case R.id.context_menu_delete:
				try {
					this.categoryModel.remove(this.categories.get(groupPos));
					this.refreshView();
				} catch (CannotDeleteException e) {
					Toast toast = Toast.makeText(this.getApplicationContext(),
							R.string.db_default_cat_cant_delete,
							Toast.LENGTH_LONG);
					toast.show();
				}
				return true;
			case R.id.context_menu_edit:
				this.dialogCategoryEdit(groupPos);

				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}
		// Record selected
		else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			childPos = ExpandableListView
					.getPackedPositionChild(info.packedPosition);

			switch (item.getItemId()) {
			case R.id.context_menu_delete:
				this.recordModel.remove(this.categories.get(groupPos).records
						.get(childPos));
				this.refreshView();

				return true;

			case R.id.context_menu_record_share:
				this.shareGunshots(this.categories.get(groupPos).records
						.get(childPos));
				return true;
			default:
				return super.onContextItemSelected(item);
			}
		}

		return super.onContextItemSelected(item);
	}

	/**
	 * Handle child click event
	 */
	private OnChildClickListener childClickListener = new OnChildClickListener(){
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			// start GunShots Activity
			Intent intent = new Intent(getApplicationContext(),
					GunshotsActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

			intent.putExtra(RECORD_ID_FLAG, id);
			startActivity(intent);

			return true;
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_stored, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;

		case R.id.menu_category_add:
			this.dialogCategoryAdd();
			return true;

		case R.id.menu_category_xml_export:

			try {
				String state = Environment.getExternalStorageState();
				if (!Environment.MEDIA_MOUNTED.equals(state)) {
					throw new SDCardException();
				}

				String filePath = Environment.getExternalStorageDirectory()
						.getAbsolutePath()
						+ "/"
						+ this.EXPORT_DIR
						+ "/"
						+ DateFormat.format("dd_MM_yyyy_export", new Date())
						+ ".xml";

				// check for folder
				File folder = new File(Environment
						.getExternalStorageDirectory().getAbsolutePath()
						+ "/"
						+ this.EXPORT_DIR);
				if (!folder.exists()) {
					folder.mkdirs();
				}

				this.categoryModel.exportToXML(filePath);

				Intent intent = new Intent();
				File file = new File(filePath);
				intent.setAction(android.content.Intent.ACTION_SEND);
				intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
				intent.setType("text/xml");
				startActivity(intent);

			} catch (SDCardException e) {
				Toast.makeText(this.getApplicationContext(),
						R.string.no_sd_card, Toast.LENGTH_LONG).show();

				e.printStackTrace();
			} catch (IOException e) {
				Toast toast = Toast.makeText(getApplicationContext(),
						R.string.xml_export_error, Toast.LENGTH_SHORT);
				toast.show();

				e.printStackTrace();
			}

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Fill ListView
	 */
	private void setUpView() {
		mAdapter = new CategoryListAdapter(this.categories,
				this.getApplicationContext());

		this.listView = (ExpandableListView) findViewById(R.id.CategoryList);
		this.listView.setOnChildClickListener(this.childClickListener);		
		
		this.listView.setAdapter(mAdapter);
	}

	/**
	 * Refresh List View
	 */
	private void refreshView() {
		this.categories = this.categoryModel.getAll();
		this.setUpView();
	}

	/**
	 * Share gunShots
	 */
	private void shareGunshots(Record record) {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		String shareBody = this.getResources().getString(R.string.my_gunshots)
				+ " " + this.getResources().getString(R.string.from) + " "
				+ this.recordModel.getShareText(record);
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this
				.getResources().getString(R.string.my_gunshots));

		Log.d("Share body", shareBody);

		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		startActivity(Intent.createChooser(sharingIntent, this.getResources()
				.getString(R.string.share)));
	}

	/**
	 * Create and handle category dialog editing
	 * 
	 * @param index
	 */
	private void dialogCategoryEdit(int index) {
		categoryEdit = this.categories.get(index);
		AlertDialog.Builder categoryEditDialog = new AlertDialog.Builder(this);

		categoryEditDialog.setTitle(R.string.title_activity_category_edit);

		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_category_edit, null);
		categoryEditDialog.setView(view);

		final EditText dialogText = (EditText) view
				.findViewById(R.id.dialog_editText);
		dialogText.setText(categoryEdit.name);

		// positive button pressed
		categoryEditDialog.setPositiveButton(R.string.save,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String categoryName = dialogText.getText().toString()
								.trim();

						if (categoryName.length() == 0) { // category name is
															// empty
							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.category_name_cant_empty,
									Toast.LENGTH_SHORT);
							toast.show();

							return;
						}

						categoryEdit.name = categoryName;
						if (categoryModel.update(categoryEdit)) {
							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.change_saved, Toast.LENGTH_SHORT);
							toast.show();
							refreshView();
						}
					}
				});
		categoryEditDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		categoryEditDialog.show();
	}

	/**
	 * Dialog category Adding
	 */
	private void dialogCategoryAdd() {
		AlertDialog.Builder categoryAddDialog = new AlertDialog.Builder(this);

		categoryAddDialog.setTitle(R.string.title_activity_category_add);

		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_category_edit, null);
		categoryAddDialog.setView(view);

		final EditText dialogText = (EditText) view
				.findViewById(R.id.dialog_editText);

		// positive button pressed
		categoryAddDialog.setPositiveButton(R.string.save,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String categoryName = dialogText.getText().toString()
								.trim();

						if (categoryName.length() == 0) { // category name is
															// empty
							Toast toast = Toast.makeText(
									getApplicationContext(),
									R.string.category_name_cant_empty,
									Toast.LENGTH_SHORT);
							toast.show();

							return;
						}

						Category newCategory = categoryModel.categoryFactory(0,
								categoryName, new ArrayList<Record>());

						try {
							if (categoryModel.add(newCategory) != null) {
								Toast toast = Toast.makeText(
										getApplicationContext(),
										R.string.change_saved,
										Toast.LENGTH_SHORT);
								toast.show();
								refreshView();
							}
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
		categoryAddDialog.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

		categoryAddDialog.show();
	}
	
}
