package command;

import java.util.List;

@FunctionalInterface
public interface CommandHandler {
	Object handle(List<String> args);
}
