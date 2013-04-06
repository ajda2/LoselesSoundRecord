package tic0012.loselessoundrecord.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database layer, singleton
 * 
 * @author tic0012
 */
public class DatabaseHandler extends SQLiteOpenHelper {
	
	/**
	 * Static instance of self, singleton pattern implementation
	 */
	private static DatabaseHandler self;

	protected static final String DB_NAME = "gunshots_db";

	protected static final int DATABASE_VERSION = 10;

	public static final String TB_GUNSHOT_NAME = "gunshot";
	public static final String COLUMN_GUNSHOT_ID = "_id";
	public static final String COLUMN_GUNSHOT_TIME = "time";
	public static final String COLUMN_GUNSHOT_RECORD_ID = "record_id";

	public static final String TB_RECORD_NAME = "record";
	public static final String COLUMN_RECORD_ID = "_id";
	public static final String COLUMN_RECORD_DATE = "dateRecorded";
	public static final String COLUMN_RECORD_CATEGORY_ID = "category_id";

	public static final String TB_CATEGORY_NAME = "category";
	public static final String COLUMN_CATEGORY_ID = "_id";
	public static final String COLUMN_CATEGORY_NAME = "name";

	/*
	public DatabaseHandler(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
	}
	*/
	
	private DatabaseHandler(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
	} 

	/**
	 * Get Database singleton instance
	 * 
	 * @return
	 */
	public static synchronized DatabaseHandler getInstance(Context context) {
		if (self == null) {
			self = new DatabaseHandler(context);
		}
		return self;
	}
	
	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		// create Category table
		String CREATE_CATEGORY_TABLE = "CREATE TABLE " + TB_CATEGORY_NAME + "("
				+ COLUMN_CATEGORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_CATEGORY_NAME + " VARCHAR(40) NOT NULL" 				
				+ ")";
		db.execSQL(CREATE_CATEGORY_TABLE);

		// create Record table
		String CREATE_RECORD_TABLE = "CREATE TABLE " + TB_RECORD_NAME + "("
				+ COLUMN_RECORD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_RECORD_DATE + " LONG NOT NULL, " 
				+ COLUMN_RECORD_CATEGORY_ID + " INTEGER NOT NULL, " 
				+ "CONSTRAINT record_in_category FOREIGN KEY (" + COLUMN_RECORD_CATEGORY_ID + ") REFERENCES category (" + COLUMN_CATEGORY_ID + ")"
				+ ")";
		db.execSQL(CREATE_RECORD_TABLE);

		// create gunShots table
		String CREATE_GUNSHOTS_TABLE = "CREATE TABLE " + TB_GUNSHOT_NAME + "("
				+ COLUMN_GUNSHOT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ COLUMN_GUNSHOT_TIME + " FLOAT NOT NULL," 
				+ COLUMN_GUNSHOT_RECORD_ID + " INTEGER NOT NULL, " 
				+ "CONSTRAINT gunshot_in_record FOREIGN KEY (" + COLUMN_GUNSHOT_RECORD_ID + ") REFERENCES record (" + COLUMN_RECORD_ID + ")"
				+ ")";
		db.execSQL(CREATE_GUNSHOTS_TABLE);

		// insert default category
		String INSERT_DEFAULT_CAT = "INSERT INTO " + TB_CATEGORY_NAME + "("
				+ COLUMN_CATEGORY_NAME + ")" + " VALUES ('Výchozí')";
		db.execSQL(INSERT_DEFAULT_CAT);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TB_GUNSHOT_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TB_RECORD_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TB_CATEGORY_NAME);

		// Create tables again
		this.onCreate(db);
	}

	/**
	 * Insert new row into DB
	 * 
	 * @param values
	 * @param tableName
	 * @return
	 */
	public long insertRow(ContentValues values, String tableName) {
		SQLiteDatabase db = this.getWritableDatabase();
		long result;

		// Inserting Row
		result = db.insert(tableName, null, values);
		db.close(); // Closing database connection

		return result;
	}

	/**
	 * Select rows from DB
	 * 
	 * @param query
	 * @param selectionArgs
	 * @return
	 */
	public Cursor select(String query, String[] selectionArgs) {
		// SQLiteDatabase db = this.getWritableDatabase();
		SQLiteDatabase db = this.getReadableDatabase();

		if (selectionArgs == null) {
			return db.rawQuery(query, null);
		}

		return db.rawQuery(query, selectionArgs);
	}

	/**
	 * Update single row in DB
	 * 
	 * @param tableName
	 * @param values
	 * @param id
	 * @return
	 */
	public int update(String tableName, ContentValues values, long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		String whereClause = "_id = ?";
		String[] whereArgs = { String.valueOf(id) };

		// updating row
		return db.update(tableName, values, whereClause, whereArgs);
	}

	/**
	 * Delete single row from DB
	 * 
	 * @param tableName
	 * @param id
	 * @return
	 */
	public int delete(String tableName, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = this.getWritableDatabase();
		// String whereClause = "_id = ?";
		// String[] whereArgs = { String.valueOf(id) };

		int result = db.delete(tableName, whereClause, whereArgs);
		db.close();

		return result;
	}
}
