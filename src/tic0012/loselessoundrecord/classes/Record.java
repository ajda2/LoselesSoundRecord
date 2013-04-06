package tic0012.loselessoundrecord.classes;

import java.util.Date;
import java.util.List;


/**
 * Recorded gunShot group representation
 * 
 * @author tic0012
 */
public class Record extends BaseObject  {

	/**
	 * Category ID, where record belong
	 */
	private long categoryId;
	
	/**
	 * Date when was record get
	 */
	public Date dateRecorderd; 
	
	/**
	 * GunShots in record set
	 */
	public List<Gunshot> gunshots;
	
	public Record(long id, List<Gunshot> gunshots, Date date, long categoryId){
		this.id = id;
		this.gunshots = gunshots;
		this.dateRecorderd = date;
		this.categoryId = categoryId;
	}
	
	public long getCategoryId(){
		return this.categoryId;
	}
}
