/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.ext;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.df4j.core.actor.ProxyActor;
import org.df4j.core.ext.SwingExecutor;

/**
 * Interaction between GUI and Actors.
 * GUI (JTextField) -> Executor (computing actor) -> GUI (printing actor)
 */
@SuppressWarnings("serial")
public class SwingProxyActorTest extends JFrame {
    JTextField jTextField = new javax.swing.JTextField();
    JTextArea jlist = new javax.swing.JTextArea();
    JLabel jLabel2 = new javax.swing.JLabel();
    int workCount;

    IPrint ca=ProxyActor.makeProxy(IPrint.class, new ComputingActor());
    IPrint pa=ProxyActor.makeProxy(IPrint.class, new PrintingActor());

    public SwingProxyActorTest() {
        this.setTitle("SwingActor Test");
        this.setSize(360, 300);
        this.getContentPane().setLayout(null);

        JLabel jLabel = new javax.swing.JLabel();
        jLabel.setBounds(24, 40, 120, 18);
        jLabel.setText("Enter number:");
        this.add(jLabel, null);

        jTextField.setBounds(162, 40, 120, 20);
        jTextField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                // GUI (JTextField) -> computing actor 
            	workCount++;
                jLabel2.setText("working...");
                ca.print(jTextField.getText());
            }
        	
        });
        this.add(jTextField, null);

        jLabel2.setBounds(34, 80, 80, 20);
        this.add(jLabel2, null);

        jlist.setBounds(34, 120, 200, 120);
        jlist.setBorder(new LineBorder(Color.BLACK));
        this.add(jlist, null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                System.exit(0);
            }
        });
    }

    public interface IPrint {
    	public void print(String msg);
    }

    class ComputingActor implements IPrint {

		@Override
		public void print(String str) {
	    	System.out.println("ComputingActor from "+Thread.currentThread().getName());
            try {
				Thread.sleep(2000);  // imitate working hard
			} catch (InterruptedException e1) {
				ProxyActor.throwUncheked(e1);
			}
            try {
                Number num = Integer.valueOf(str);
                str = num.toString()+" is integer\n";
            } catch (NumberFormatException e) {
                try {
                    Number num = Double.valueOf(str);
                    str = num.toString()+" is double\n";
                } catch (NumberFormatException e2) {
                    str = str+" is not a number\n";
                }
            }
            // computing actor -> GUI (printing actor)
            pa.print(str); 
		}
    }
    
    /**
     * Processes messages on EDT.
     */
    class PrintingActor implements IPrint {

        @Override
		public void print(String str) {
	    	System.out.println("PrintingActor from "+Thread.currentThread().getName());
            jlist.append(str);
            workCount--;
            if (workCount==0) {
            	jLabel2.setText("");
            }
        }
    }

    public static void main(String[] args) throws Exception {
		SwingExecutor.invokeFirstTask(new Runnable() {
            public void run() {
                new SwingProxyActorTest().setVisible(true);
            }
        });
    }

}
