/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.SwingSupport.EDTActor;
import com.github.rfqu.df4j.testutil.StringMessage;

/**
 * Interaction between GUI and Actors.
 * GUI (JTextField) -> Executor (computing actor) -> GUI (printing actor)
 */
@SuppressWarnings("serial")
public class SwingActorTest extends JFrame {
    JTextField jTextField = new javax.swing.JTextField();
    JTextArea jlist = new javax.swing.JTextArea();
    JLabel jLabel2 = new javax.swing.JLabel();
    int workCount;

    ComputingActor ca = new ComputingActor();
    PrintingActor pa = new PrintingActor();

    public SwingActorTest(DFContext context) {
    	boolean ok=(context == DFContext.getCurrentContext());
        this.setTitle("SwingActor Test: "+(ok?"ok":"bad"));
        this.setSize(360, 300);
        this.getContentPane().setLayout(null);

        JLabel jLabel = new javax.swing.JLabel();
        jLabel.setBounds(24, 40, 120, 18);
        jLabel.setText("Enter number:");
        this.add(jLabel, null);

        jTextField.setBounds(162, 40, 120, 20);
        jTextField.addActionListener(ca);
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

    class ComputingActor extends Actor<StringMessage> implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            // GUI (JTextField) -> computing actor 
        	workCount++;
            jLabel2.setText("working...");
            post(new StringMessage(jTextField.getText()));
        }

        /**
         * Processes message asynchronously using its ExecutorService.
         */
        @Override
        protected void act(StringMessage m) throws Exception {
            Thread.sleep(2000); // imitate working hard
            String str = m.getStr();
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
            m.setStr(str);
            // computing actor -> GUI (printing actor)
            pa.post(m); 
        }
    }

    /**
     * Processes messages on EDT.
     */
    class PrintingActor extends EDTActor<StringMessage> {
        @Override
        protected void act(StringMessage m) throws Exception {
            jlist.append(m.getStr());
            workCount--;
            if (workCount==0) {
            	jLabel2.setText("");
            }
        }
    }

    public static void main(String[] args) throws Exception {
		DFContext.setSingleThreadExecutor(); // for example
		final DFContext currentContext = DFContext.getCurrentContext();
		SwingSupport.setEDTDefaultContext(currentContext);
    	EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SwingActorTest(currentContext).setVisible(true);
            }
        });
    }

}
