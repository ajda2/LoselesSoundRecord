package tic0012.loselessoundrecord.model;

/**
 * @author tic0012, Michal Tichý
 */
public class DBException extends Exception {
	private static final long serialVersionUID = 1L;

	public DBException() {
	}

	public DBException(String msg) {
		super(msg);
	}
}
