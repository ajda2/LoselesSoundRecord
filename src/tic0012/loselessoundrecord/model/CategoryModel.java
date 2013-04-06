package tic0012.loselessoundrecord.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;
import tic0012.loselessoundrecord.classes.Category;
import tic0012.loselessoundrecord.classes.Gunshot;
import tic0012.loselessoundrecord.classes.Record;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.util.Xml;

/**
 * Category of GunShots repository model, singleton
 * 
 * @author tic0012
 */
public class CategoryModel implements IModel<Category> {

	/**
	 * Static instance of self, singleton pattern implementation
	 */
	private static CategoryModel self;

	/**
	 * Default Category ID, which cannot be deleted
	 */
	public final long DEFAULT_CATEGORY_ID = 1;

	/**
	 * Record model
	 */
	private RecordModel recordModel;

	/**
	 * Work with DB
	 */
	private DatabaseHandler dbHandler;

	/**
	 * Private constructor, singleton implementation
	 */
	private CategoryModel(Context context) {
		//this.dbHandler = new DatabaseHandler(context);
		this.dbHandler = DatabaseHandler.getInstance(context);
		this.recordModel = RecordModel.getInstance(context);
	}

	/**
	 * Get Model singleton instance
	 * 
	 * @return
	 */
	public static synchronized CategoryModel getInstance(Context context) {
		if (self == null) {
			self = new CategoryModel(context);
		}
		return self;
	}

	/**
	 * Save new Category into DB
	 * 
	 * @throws DBException
	 */
	public Category add(Category cat) throws DBException {
		long result;
		ArrayList<Record> records = new ArrayList<Record>();
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.COLUMN_CATEGORY_NAME, cat.name);

		result = this.dbHandler.insertRow(values,
				DatabaseHandler.TB_CATEGORY_NAME);

		if (result < 0) {
			throw new DBException("Insert new Category failure");
		}

		// setup records
		if(cat.records != null){
			for (Record record : cat.records) {
				record = this.recordModel.add(record);
				records.add(record);
			}
		}
		
		return this.categoryFactory(result, cat.name, records);
	}

	/**
	 * Update Category
	 * 
	 * @param cat
	 * @return
	 */
	public boolean update(Category cat) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.COLUMN_CATEGORY_NAME, cat.name);

		int result = this.dbHandler.update(DatabaseHandler.TB_CATEGORY_NAME,
				values, cat.getId());

		if (result > 0) {
			return true;
		}

		return false;
	}

	/**
	 * Delete Category from DB
	 * 
	 * @throws CannotDeleteException
	 */
	public boolean remove(Category cat) throws CannotDeleteException {
		String whereClause = DatabaseHandler.COLUMN_CATEGORY_ID + " = ?";
		String[] whereArgs = { String.valueOf(cat.getId()) };
		int result;

		if (cat.getId() == this.DEFAULT_CATEGORY_ID) {
			throw new CannotDeleteException(
					"Default category cannot be deleted");
		}

		// delete category
		result = this.dbHandler.delete(DatabaseHandler.TB_CATEGORY_NAME,
				whereClause, whereArgs);

		// delete records in it
		if (result > 0) {
			// boolean recordResult = false;
			for (Record record : cat.records) {
				this.recordModel.remove(record);
			}
			// result = this.dbHandler.delete(DatabaseHandler.TB_RECORD_NAME,
			// whereClause, whereArgs);

			return true;
		}

		return false;
	}

	/**
	 * Get single Category from DB by ID
	 */
	public Category get(long id) {
		String query = "SELECT * FROM " + DatabaseHandler.TB_CATEGORY_NAME
				+ " WHERE " + DatabaseHandler.COLUMN_CATEGORY_ID
				+ " = ? LIMIT 1";
		String[] selectionArgs = { String.valueOf(id) };
		ArrayList<Record> records = new ArrayList<Record>();
		Category category;

		// get Category
		Cursor cursor = this.dbHandler.select(query, selectionArgs);

		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();

			category = this
					.categoryFactory(
							Long.parseLong(cursor.getString(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_CATEGORY_ID))),
							cursor.getString(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_CATEGORY_NAME)),
							records);
			cursor.close();

			// get all records in category
			query = "SELECT * FROM " + DatabaseHandler.TB_RECORD_NAME
					+ " WHERE " + DatabaseHandler.COLUMN_RECORD_CATEGORY_ID
					+ " = ?";
			cursor = this.dbHandler.select(query, selectionArgs);
			if (cursor.moveToFirst() && cursor.getCount() > 0) {
				do {

					Record record = this.recordModel.get(cursor.getLong(cursor
							.getColumnIndex(DatabaseHandler.COLUMN_RECORD_ID)));

					// Adding record to list
					records.add(record);
				} while (cursor.moveToNext());

				category.records = records;
			}
			cursor.close();

			return category;
		}

		return null;
	}

	/**
	 * Get all categories from DB
	 * 
	 * @return
	 */
	public ArrayList<Category> getAll() {
		ArrayList<Category> categories = new ArrayList<Category>();
		String query = "SELECT * FROM " + DatabaseHandler.TB_CATEGORY_NAME
				+ " ORDER BY " + DatabaseHandler.COLUMN_CATEGORY_ID +" ASC";

		// get Category
		Cursor cursor = this.dbHandler.select(query, null);

		// no category found
		if (!cursor.moveToFirst() && cursor.getCount() < 1) {
			return null;
		}

		// get all categories
		do {
			long catId = Long.parseLong(cursor.getString(cursor
					.getColumnIndex(DatabaseHandler.COLUMN_CATEGORY_ID)));

			Category category = this
					.categoryFactory(
							catId,
							cursor.getString(cursor
									.getColumnIndex(DatabaseHandler.COLUMN_CATEGORY_NAME)),
							new ArrayList<Record>());

			// get all records in category			
			String query2 = "SELECT " + DatabaseHandler.COLUMN_RECORD_ID + " FROM "
					+ DatabaseHandler.TB_RECORD_NAME + " WHERE "
					+ DatabaseHandler.COLUMN_RECORD_CATEGORY_ID + " = ? "
					+ "ORDER BY " + DatabaseHandler.COLUMN_RECORD_ID +" DESC";
			String[] selectionArgs = { String.valueOf(catId) };
			Cursor recordCursor = this.dbHandler.select(query2, selectionArgs);

			// some records found
			
			if (recordCursor.moveToFirst() && recordCursor.getCount() > 0) {
				do {					
					Record record = this.recordModel
							.get(recordCursor.getLong(recordCursor
									.getColumnIndex(DatabaseHandler.COLUMN_RECORD_ID)));

					// Adding record to list
					category.records.add(record);						
				} while (recordCursor.moveToNext());				
			}	
			recordCursor.close();
			
			categories.add(category);

		} while (cursor.moveToNext());
		cursor.close();

		return categories;
	}

	/**
	 * 
	 * @param id
	 * @param name
	 * @param gunshots
	 * @return
	 */
	public Category categoryFactory(long id, String name, List<Record> records) {
		return new Category(id, name, records);
	}
	
	public void exportToXML(String filePath) throws IOException{
		ArrayList<Category> categories = this.getAll();
		
		File newxmlfile = new File(filePath);
		newxmlfile.createNewFile();
		FileOutputStream fileos = null;
		fileos = new FileOutputStream(newxmlfile);		
		XmlSerializer serializer = Xml.newSerializer();
		
		serializer.setOutput(fileos, "UTF-8");
        serializer.startDocument("UTF-8", true);        
        
        serializer.startTag(null, "categories");
        
        for(Category category : categories){
        	serializer.startTag(null, "category");
        	serializer.attribute(null, "id", String.valueOf(category.getId()) );
        	
        	serializer.startTag(null, "name");
        	serializer.text(category.name);
        	serializer.endTag(null, "name");
        	
        	serializer.startTag(null, "records");
        	
        	for(Record record : category.records){
        		serializer.startTag(null, "record");
        		serializer.attribute(null, "id", String.valueOf(record.getId()) );
        		serializer.attribute(null, "date", (String)DateFormat.format("yyyy-MM-dd hh:mm:ss", record.dateRecorderd));
        		
        		serializer.startTag(null, "gunshots");        		
        		for(Gunshot gunshot : record.gunshots){
        			serializer.startTag(null, "gunshot");  
        			serializer.attribute(null, "id", String.valueOf(gunshot.getId()) );
        			serializer.attribute(null, "time", String.valueOf(gunshot.time) );
        			serializer.endTag(null, "gunshot");
        		}
        		serializer.endTag(null, "gunshots");
        		
        		serializer.endTag(null, "record");
        	}
        	
        	serializer.endTag(null, "records");
        	
        	serializer.endTag(null, "category");
        }
        
        serializer.endTag(null, "categories");
        
        serializer.endDocument();
        serializer.flush();
        fileos.close();
	}	

}
