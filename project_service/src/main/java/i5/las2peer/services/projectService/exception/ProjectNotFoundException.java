package i5.las2peer.services.projectService.exception;

import java.sql.SQLException;

/**
 * Exception class to differentiate "correct" not found cases from real database
 * errors.
 */
public class ProjectNotFoundException extends SQLException {
	private static final long serialVersionUID = 3005029978036391725L;
}
