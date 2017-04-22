import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LineFormatter extends Formatter {
	public String format(LogRecord record) {
		StringBuilder resultStr = new StringBuilder();
		long mSeconds = record.getMillis();
		Date date = new Date(mSeconds);
		String  dateStr  = new SimpleDateFormat("MMM dd,yy HH:mm:ss").format(date);
		resultStr.append(dateStr).append("\t").append(record.getMessage()).append("\n");
		return resultStr.toString();
	}

}
