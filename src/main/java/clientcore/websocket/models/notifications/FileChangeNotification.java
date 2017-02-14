package clientcore.websocket.models.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import clientcore.websocket.models.INotificationData;

import java.util.Arrays;

public class FileChangeNotification implements INotificationData {
	@JsonProperty("Changes")
	public final String[] changes;

	@JsonProperty("FileVersion")
	public final long fileVersion;

	@JsonProperty("BaseFileVersion")
	public final long baseFileVersion;

	public FileChangeNotification(@JsonProperty("Changes") String[] changes,
								  @JsonProperty("FileVersion") long fileVersion,
								  @JsonProperty("BaseFileVersion") long baseFileVersion) {
		this.changes = changes;
		this.fileVersion = fileVersion;
		this.baseFileVersion = baseFileVersion;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		FileChangeNotification that = (FileChangeNotification) o;

		if (fileVersion != that.fileVersion) return false;
		if (baseFileVersion != that.baseFileVersion) return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		return Arrays.equals(changes, that.changes);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(changes);
		result = 31 * result + (int) (fileVersion ^ (fileVersion >>> 32));
		result = 31 * result + (int) (baseFileVersion ^ (baseFileVersion >>> 32));
		return result;
	}
}
