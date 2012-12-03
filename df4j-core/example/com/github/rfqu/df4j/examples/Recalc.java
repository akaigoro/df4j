/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.ext.MultiPortActor;
import com.github.rfqu.df4j.ext.SwingSupport;
import com.github.rfqu.df4j.ext.SwingSupport.EDTActor;

/**
 * Interaction between GUI and Actors.
 * GUI (JTextField) -> Executor (computing actor) -> GUI (printing actor)
 */
@SuppressWarnings("serial")
public class Recalc extends JFrame {
    JTextField opnd1Field = new javax.swing.JTextField();
    JTextField opnd2Field = new javax.swing.JTextField();
    JTextField res1Field = new javax.swing.JTextField();
    JTextField res2Field = new javax.swing.JTextField();
    JLabel jLabel2 = new javax.swing.JLabel();
    int workCount;

    ComputingActor ca = new ComputingActor();
    PrintingActor pa1 = new PrintingActor(res1Field);
    PrintingActor pa2 = new PrintingActor(res2Field);

    public Recalc(DFContext context) {
    	boolean ok=(context == DFContext.getCurrentContext());
        this.setTitle("Recalculator: "+(ok?"ok":"bad"));
        this.setSize(360, 300);
        this.getContentPane().setLayout(null);

        jLabel2.setBounds(30, 40, 100, 20);
        jLabel2.setText("Enter numbers:   ");
        this.add(jLabel2, null);

        opnd1Field.setBounds(30, 80, 120, 20);
        opnd1Field.addActionListener(ca.input1);
        this.add(opnd1Field, null);

        opnd2Field.setBounds(30, 120, 120, 20);
        opnd2Field.addActionListener(ca.input2);
        this.add(opnd2Field, null);

        res1Field.setBounds(160, 80, 180, 20);
        res1Field.setEditable(false);
        this.add(res1Field, null);

        res2Field.setBounds(160, 120, 180, 20);
        res2Field.setEditable(false);
        this.add(res2Field, null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent winEvt) {
                System.exit(0);
            }
        });
    }

    class ComputingActor extends MultiPortActor {
        NumInput input1=new NumInput();
        NumInput input2=new NumInput();
        
        void print(String res1, String res2) {
            pa1.send(res1);
            pa2.send(res2);
        }

        void recalc() {
            Number opnd1=input1.opnd, opnd2=input2.opnd;
            String res1=null, res2=null;
            if (opnd1==null || opnd2==null) {
                res1=res2="N/A";
            } else 
            if ((opnd1 instanceof Double) || (opnd2 instanceof Double)) {
                double double1 = opnd1.doubleValue();
                double double2 = opnd2.doubleValue();
                double r1=double1+double2;
                double r2=double1*double2;
                res1=Double.toString(r1);
                res2=Double.toString(r2);
//                System.out.println("double: "+r1+",  "+r2);
            } else {
                int int1 = opnd1.intValue();
                int int2 = opnd2.intValue();
                int r1=int1+int2;
                int r2=int1*int2;
                res1=Integer.toString(r1);
                res2=Integer.toString(r2);
            }
            print(res1, res2);            
        }

        class NumInput extends PortHandler<String> implements ActionListener {
            Number opnd;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                send(e.getActionCommand());
            }

            /**
            * Processes message asynchronously using its ExecutorService.
            */
            @Override
            protected void act(String str) {
//                System.out.println("act: "+str+" in "+this);
                Number num;
                try {
                    num = Integer.valueOf(str);
                } catch (NumberFormatException e) {
                    try {
                        num = Double.valueOf(str);
                    } catch (NumberFormatException e2) {
                        if (opnd!=null) {
                            opnd=null;
                            recalc();
                        }
                        return;
                    }
                }
                if (!num.equals(opnd)) {
                    opnd=num;
                    recalc();
                }
            }
        }
    }

    /**
     * Processes messages on EDT.
     */
    class PrintingActor extends EDTActor<String> {
        JTextField resField;

        public PrintingActor(JTextField resField) {
            this.resField = resField;
        }

        @Override
        protected void act(String m) throws Exception {
            resField.setText(m);
        }
    }

    public static void main(String[] args) throws Exception {
		DFContext.setSingleThreadExecutor(); // for example
		final DFContext currentContext = DFContext.getCurrentContext();
		SwingSupport.setEDTDefaultContext(currentContext);
    	EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Recalc(currentContext).setVisible(true);
            }
        });
    }

}
