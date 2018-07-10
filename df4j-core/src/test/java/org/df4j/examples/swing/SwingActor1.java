package org.df4j.examples.swing;

import org.df4j.core.node.messagestream.Actor1;

import java.awt.EventQueue;

public abstract class SwingActor1<T> extends Actor1<T> {
   {
      setExecutor(EventQueue::invokeLater);
      start();
   }
}
