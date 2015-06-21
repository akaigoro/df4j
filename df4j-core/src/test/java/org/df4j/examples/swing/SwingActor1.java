package org.df4j.examples.swing;

import java.awt.EventQueue;
import org.df4j.core.Actor1;

public abstract class SwingActor1<T> extends Actor1<T> {

    // runs on Event Dispatch Thread and allows the act() method to modify GUI
    @Override
    protected void fire() {
        EventQueue.invokeLater(this);
    }
}
