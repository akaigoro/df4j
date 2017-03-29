package org.df4j.examples.swing;

import java.awt.EventQueue;
import java.util.concurrent.Executor;

public class SwingExecutor implements Executor {
    public static final Executor swingExecutor = new SwingExecutor();

    // runs on Event Dispatch Thread and allows the act() method to modify GUI
	@Override
	public void execute(Runnable command) {
        EventQueue.invokeLater(command);
	}
}
