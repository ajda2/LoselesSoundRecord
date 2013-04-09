package tic0012.loselessoundrecord.classes;


/**
 * GunShot representation
 * 
 * @author tic0012, Michal Tichý
 */
public class Gunshot extends BaseObject {
	
	/**
	 * Time when was fired in seconds
	 */
	public float time;
	
	/**
	 * Category ID which belong
	 */
	public long recordId;
	
	public Gunshot(long id, float time, long recordId){
		this.id = id;
		this.time = time;
		this.recordId = recordId;
	}	
	
	public long getRecordId(){
		return this.recordId;
	}
}