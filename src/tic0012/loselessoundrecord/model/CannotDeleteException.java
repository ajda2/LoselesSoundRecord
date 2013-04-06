package tic0012.loselessoundrecord.model;

/**
 * @author tic0012
 */
public class CannotDeleteException extends DBException {
	private static final long serialVersionUID = 2L;

	public CannotDeleteException() {
	}

	public CannotDeleteException(String msg) {
		super(msg);
	}
}
