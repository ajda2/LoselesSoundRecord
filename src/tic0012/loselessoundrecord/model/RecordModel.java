package tic0012.loselessoundrecord.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import tic0012.loselessoundrecord.classes.Gunshot;
import tic0012.loselessoundrecord.classes.Record;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;

/**
 * Record set of GunShots repository model, singleton
 * 
 * @author tic0012, Michal Tichý
 */
public class RecordModel implements IModel<Record> {

	/**
	 * Static instance of self, singleton pattern implementation
	 */
	private static RecordModel self;

	/**
	 * GunShot model
	 */
	private GunshotModel gunShotModel;

	/**
	 * Work with DB
	 */
	private DatabaseHandler dbHandler;

	/**
	 * Private constructor, singleton implementation
	 */
	private RecordModel(Context context) {
		//this.dbHandler = new DatabaseHandler(context);
		this.dbHandler = DatabaseHandler.getInstance(context);
		this.gunShotModel = GunshotModel.getInstance(context);
	}

	/**
	 * Get Model singleton instance
	 * 
	 * @return
	 */
	public static synchronized RecordModel getInstance(Context context) {
		if (self == null) {
			self = new RecordModel(context);
		}
		return self;
	}

	/**
	 * Save new Record into DB
	 */
	public Record add(Record record) throws DBException {
		long result;
		ArrayList<Gunshot> gunshots = new ArrayList<Gunshot>();
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.COLUMN_RECORD_DATE,
				record.dateRecorderd.getTime());
		values.put(DatabaseHandler.COLUMN_RECORD_CATEGORY_ID,
				record.getCategoryId());

		result = this.dbHandler.insertRow(values,
				DatabaseHandler.TB_RECORD_NAME);

		if (result < 0) {
			throw new DBException("Insert new Record set failure");
		}

		// setup gunShots
		for (Gunshot shot : record.gunshots) {
			Gunshot gunShot;
			shot.recordId = result;

			gunShot = this.gunShotModel.add(shot);
			gunshots.add(gunShot);
		}

		return this.recordFactory(result, gunshots, record.dateRecorderd,
				record.getCategoryId());
	}

	/**
	 * Delete Record set from DB
	 */
	public boolean remove(Record record) {
		String whereClause = "_id = ?";
		String[] whereArgs = { String.valueOf(record.getId()) };
		int result;

		// delete record set
		result = this.dbHandler.delete(DatabaseHandler.TB_RECORD_NAME,
				whereClause, whereArgs);

		// delete gunShots in it
		if (result > 0) {
			whereClause = DatabaseHandler.COLUMN_GUNSHOT_RECORD_ID + " = ?";
			result = this.dbHandler.delete(DatabaseHandler.TB_GUNSHOT_NAME,
					whereClause, whereArgs);

			if (result > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get Record from DB
	 */
	public Record get(long id) {
		String query = "SELECT * FROM " + DatabaseHandler.TB_RECORD_NAME
				+ " WHERE " + DatabaseHandler.COLUMN_RECORD_ID + " = ? LIMIT 1";
		String[] selectionArgs = { String.valueOf(id) };
		ArrayList<Gunshot> gunShots = new ArrayList<Gunshot>();
		Record record;

		// get Category
		Cursor cursor = this.dbHandler.select(query, selectionArgs);

		// some gunShots was found
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			long dateTime = cursor.getLong(cursor
					.getColumnIndex(DatabaseHandler.COLUMN_RECORD_DATE));
			Date dateRecorded = new Date(dateTime);

			record = this
					.recordFactory(
							cursor.getLong(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_RECORD_ID)),
							gunShots,
							dateRecorded,
							cursor.getLong(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_RECORD_CATEGORY_ID)));
			cursor.close();

			// get all gunShots in record set
			query = "SELECT * FROM " + DatabaseHandler.TB_GUNSHOT_NAME
					+ " WHERE " + DatabaseHandler.COLUMN_GUNSHOT_RECORD_ID
					+ " = ? " + "ORDER BY " + DatabaseHandler.COLUMN_GUNSHOT_TIME
					+ " ASC";
			cursor = this.dbHandler.select(query, selectionArgs);
			if (cursor.moveToFirst() && cursor.getCount() > 0) {
				do {
					Gunshot gunShot = this.gunShotModel
							.gunShotFactory(
									cursor.getLong(cursor
											.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_ID)),
									cursor.getFloat(cursor
											.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_TIME)),
									cursor.getLong(cursor
											.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_RECORD_ID)));

					// Adding gunShots to list
					gunShots.add(gunShot);
				} while (cursor.moveToNext());

				record.gunshots = gunShots;
			}
			cursor.close();

			return record;
		}

		return null;
	}

	/**
	 * Record set factory
	 * 
	 * @param id
	 * @param gunshots
	 *            List of gunShot
	 * @param dateRecorded
	 *            Date when was record set setup
	 * @return
	 */
	public Record recordFactory(long id, List<Gunshot> gunshots,
			Date dateRecorded, long categoryId) {
		return new Record(id, gunshots, dateRecorded, categoryId);
	}

	/**
	 * Prepare text to share
	 * @param record
	 * @return
	 */
	public String getShareText(Record record) {
		String shareText = "";
		shareText += (String) DateFormat.format("d. M. yyyy k:mm",
				record.dateRecorderd) + "\n";
		for(int i = 0; i < record.gunshots.size(); i++){
			shareText += record.gunshots.get(i).time + "s";
			
			if((i + 1) < record.gunshots.size()){
				shareText += ", ";
			}
		}
		
		return shareText;
	}

}
