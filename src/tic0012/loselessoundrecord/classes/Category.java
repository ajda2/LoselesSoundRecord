package tic0012.loselessoundrecord.classes;

import java.util.List;


/**
 * Category of GunShots
 * 
 * @author tic0012
 */
public class Category extends BaseObject  {

	/**
	 * Records in Category
	 */
	public List<Record> records;
	
	/**
	 * Group Name
	 */
	public String name;
	
	public Category(long id, String name, List<Record> records){
		this.id = id;
		this.name = name;
		this.records = records;		
	}
	
}
