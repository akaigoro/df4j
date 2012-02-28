/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.swing;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;

/**
 * Processes messages in asynchronous way using its own thread.
 * @param <M> the type of accepted messages
 */
public class SwingActorTest {
    public void smokeTest() {
        System.out.println(" thr="+Thread.currentThread().getName());
        final HelloWorld hw= new HelloWorld();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
 //               hw = new HelloWorld();
                hw.setVisible(true);
            }
        });
        hw.sa.send("M1");
        hw.sa.send("M2");
    }
    
    @SuppressWarnings("serial")
    class HelloWorld extends JFrame {
        private JTextField jTextField = new javax.swing.JTextField();
        private JLabel jLabel = new javax.swing.JLabel();
        JButton jButton = new javax.swing.JButton();
        JScrollPane jScrollPane=new JScrollPane(); 
        JTextArea jlist = new javax.swing.JTextArea();
        SimpleActor sa=new SimpleActor(jlist);

        public HelloWorld() {
           this.setTitle("HelloWorld");
           this.setSize(360, 300);
           this.getContentPane().setLayout(null);
           jLabel.setBounds(34, 40, 60, 18);
           jLabel.setText("Enter text:");
           this.add(jLabel, null);
           jTextField.setBounds(100, 40, 160, 20);
           this.add(jTextField, null);
           jButton.setBounds(270, 40, 60, 33);
           jButton.setText("OK");
           this.add(jButton, null);
           jButton.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                sa.send("entered:"+jTextField.getText());
           }});
           
           this.add(jScrollPane, null);
           jScrollPane.createVerticalScrollBar();
           jScrollPane.setBounds(34, 90, 280, 120);
           jScrollPane.add(jlist, null);
           jlist.setBounds(0, 0, 280, 120);
           jlist.setBorder(new LineBorder(Color.BLACK));
           
           addWindowListener(new java.awt.event.WindowAdapter() {
               public void windowClosing(WindowEvent winEvt) {
                   System.exit(0);
               }
           });

        }

    }
    
    class SimpleActor extends SwingActor<StringMessage> {
        JTextArea jlist;
        {start();}

        public SimpleActor(JTextArea jlist) {
            this.jlist=jlist;
        }

        public void send(String m) {
            super.send(new StringMessage(m));
        }

        @Override
        protected void act(StringMessage m) throws Exception {
            jlist.append(m.str);
            jlist.append("\n");
        }

        @Override
        protected void complete() throws Exception {
            // TODO Auto-generated method stub
        }
        
    }
    
    static class StringMessage extends Link {
        String str;

        public StringMessage(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
    
    public static void main(String[] args) throws Exception {
        SwingActorTest t=new SwingActorTest();
        t.smokeTest();
    }
    
}
