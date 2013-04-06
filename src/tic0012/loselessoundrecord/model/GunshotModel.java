package tic0012.loselessoundrecord.model;

import tic0012.loselessoundrecord.classes.Gunshot;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

/**
 * GunShot repository model, singleton
 * 
 * @author tic0012
 */
public class GunshotModel implements IModel<Gunshot> {

	/**
	 * Static instance of self, singleton pattern implementation
	 */
	private static GunshotModel self;

	/**
	 * Work with DB
	 */
	private DatabaseHandler dbHandler;

	/**
	 * Private constructor, singleton implementation
	 */
	private GunshotModel(Context context) {
		//this.dbHandler = new DatabaseHandler(context);
		this.dbHandler = DatabaseHandler.getInstance(context);
	}

	/**
	 * Get Model singleton instance
	 * 
	 * @return
	 */
	public static synchronized GunshotModel getInstance(Context context) {
		if (self == null) {
			self = new GunshotModel(context);
		}
		return self;
	}

	/**
	 * Save new GunShot into DB
	 * 
	 * @throws DBException
	 */
	public Gunshot add(Gunshot gunShot) throws DBException {
		long result;
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.COLUMN_GUNSHOT_TIME, gunShot.time);
		values.put(DatabaseHandler.COLUMN_GUNSHOT_RECORD_ID,
				gunShot.getRecordId());

		result = this.dbHandler.insertRow(values,
				DatabaseHandler.TB_GUNSHOT_NAME);

		if (result < 0) {
			throw new DBException("Insert new gunshot failure");
		}

		return this.gunShotFactory(result, gunShot.time, gunShot.getRecordId());
	}

	/**
	 * Delete GunShot from DB
	 */
	public boolean remove(Gunshot gunShot) {
		String whereClause = DatabaseHandler.COLUMN_GUNSHOT_ID + " = ?";
		String[] whereArgs = { String.valueOf(gunShot.getId()) };

		int result = this.dbHandler.delete(DatabaseHandler.TB_GUNSHOT_NAME,
				whereClause, whereArgs);

		if (result > 0) {
			return true;
		}

		return false;
	}

	/**
	 * Get GunShot from DB by ID
	 */
	public Gunshot get(long id) {
		String query = "SELECT * FROM " + DatabaseHandler.TB_GUNSHOT_NAME
				+ " WHERE " + DatabaseHandler.COLUMN_GUNSHOT_ID
				+ " = ? LIMIT 1";
		String[] selectionArgs = { String.valueOf(id) };
		Gunshot gunshot = null;

		Cursor cursor = this.dbHandler.select(query, selectionArgs);

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			gunshot = this
					.gunShotFactory(
							cursor.getLong(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_ID)),
							cursor.getFloat(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_TIME)),
							cursor.getLong(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_GUNSHOT_RECORD_ID)));
		}
		cursor.close();

		return gunshot;
	}

	/**
	 * GunShot Factory
	 * 
	 * @param id
	 *            Unique gunShot ID
	 * @param time
	 *            Time when was fired in seconds, from record started
	 * @param recordSetId
	 *            Record set ID
	 * @return
	 */
	public Gunshot gunShotFactory(long id, float time, long recordSetId) {
		return new Gunshot(id, time, recordSetId);
	}
}
