package com.github.rfqu.df4j.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

public class JavaAppLauncher extends Thread {
	static class StreamPrinter extends Thread {
		BufferedReader in;
		PrintStream out;

		public StreamPrinter(InputStream in, PrintStream out) {
			this.in = new BufferedReader(new InputStreamReader(in));
			this.out = out;
			super.setName("StreamPrinter");
			super.setDaemon(true);
		}

		public void run() {
			for (;;) {
				try {
					String line = in.readLine();
					if (line == null) {
						// out.println("StreamPrinter exit");
						return;
					}
					out.println("server: " + line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static Process startJavaApp(String className, String... args)
			throws IOException
    {
		PrintStream out = System.out;
		PrintStream err = System.err;
		String javaHome = System.getProperty("java.home");
		String java = javaHome + File.separator + "bin" + File.separator
				+ "java"; // java
		out.println("java=" + java);
		String classPath = System.getProperty("java.class.path");
		out.println("classPath=" + classPath);
		String[] newArgs = new String[args.length + 2];
		newArgs[0] = java;
		newArgs[1] = className;
		System.arraycopy(args, 0, newArgs, 2, args.length);
		ProcessBuilder pb = new ProcessBuilder();
		Map<String, String> env = pb.environment();
		env.put("CLASSPATH", classPath);
		pb.command(newArgs);
		Process pr = pb.start();
		new StreamPrinter(pr.getErrorStream(), err).start();
		new StreamPrinter(pr.getInputStream(), out).start();
		return pr;
	}
}