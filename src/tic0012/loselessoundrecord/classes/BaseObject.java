package tic0012.loselessoundrecord.classes;

import tic0012.loselessoundrecord.model.IDbEntity;

/**
 * Base object representation, Models interact with it
 * 
 * @author tic0012, Michal Tichý
 */
public abstract class BaseObject implements IDbEntity{

	/**
	 * Unique DB identification
	 */
	protected long id;
	
	public long getId(){
		return this.id;
	}
	
}
