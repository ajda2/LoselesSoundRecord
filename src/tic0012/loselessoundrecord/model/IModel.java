package tic0012.loselessoundrecord.model;


/**
 * Model structure
 * 
 * @author tic0012
 */
public interface IModel<T>{

	/**
	 * Save object into DB
	 * @param o
	 * @return Saved object with unique ID
	 * @throws DBException 
	 */
	public T add(T o) throws DBException;
	
	/**
	 * Delete Object from DB
	 * @param o
	 * @return true if object was deleted, false otherwise
	 * @throws CannotDeleteException 
	 */
	public boolean remove(T o) throws CannotDeleteException;
	
	/**
	 * Get object from DB
	 * @param id
	 * @return
	 */
	public T get(long id);	
	
}
