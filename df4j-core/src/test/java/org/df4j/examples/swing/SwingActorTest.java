/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.examples.swing;

import org.df4j.core.node.Actor1;

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

/**
 * Interaction between GUI and Actors.
 * EDT (JTextField) -> Executor (computing actor) -> EDT (printing actor)
 */
@SuppressWarnings("serial")
public class SwingActorTest extends JFrame {
    JTextField jTextField = new javax.swing.JTextField();
    JTextArea jlist = new javax.swing.JTextArea();
    JLabel jLabel2 = new javax.swing.JLabel();
    int workCount;

    ComputingActor ca = new ComputingActor();
    PrintingActor pa = new PrintingActor();

    public SwingActorTest() {
        this.setTitle("SwingActor Test");
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

    class ComputingActor extends Actor1<String> implements ActionListener{
        {
            start();
        }
        /**
         * handles messages on EDT.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // GUI (JTextField) -> computing actor 
        	workCount++;
            jLabel2.setText("working...");
            this.post(jTextField.getText());
        }

        /**
         * Processes message asynchronously out of EDT.
         */
        @Override
        protected void act(String str) throws Exception {
            Thread.sleep(2000); // imitate working hard
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
            pa.post(str); 
        }
    }

    /**
     * Processes messages on EDT.
     */
    class PrintingActor extends SwingActor1<String> {

        @Override
        protected void act(String m) throws Exception {
            jlist.append(m);
            workCount--;
            if (workCount==0) {
            	jLabel2.setText("");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(()->new SwingActorTest().setVisible(true));
    }

}
