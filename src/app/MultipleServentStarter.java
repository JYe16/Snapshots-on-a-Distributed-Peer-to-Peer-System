package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This class implements the logic for starting multiple servent instances.
 * 
 * To use it, invoke startServentTest with a directory name as parameter.
 * This directory should include:
 * <ul>
 * <li>A <code>servent_list.properties</code> file (explained in {@link AppConfig} class</li>
 * <li>A directory called <code>output</code> </li>
 * <li>A directory called <code>error</code> </li>
 * <li>A directory called <code>input</code> with text files called
 * <code> servent0_in.txt </code>, <code>servent1_in.txt</code>, ... and so on for each servent.
 * These files should contain the commands for each servent, as they would be entered in console.</li>
 * </ul>
 *
 * @author stefanGT44
 */
public class MultipleServentStarter {

	/**
	 * We will wait for user stop in a separate thread.
	 * The main thread is waiting for processes to end naturally.
	 */
	private static class ServentCLI implements Runnable {
		
		private List<Process> serventProcesses;
		
		public ServentCLI(List<Process> serventProcesses) {
			this.serventProcesses = serventProcesses;
		}
		
		@Override
		public void run() {
			long startTime = System.nanoTime();
			long endTime = startTime + (AppConfig.timeLimit * 1000000000L);
			while(true) {
				if (System.nanoTime() >= endTime) {
					for (Process process : serventProcesses) {
						process.destroy();
					}
					break;
				}
			}
			//File folder = new File("D:\\Code\\COP-5611\\Spezialetti-Kearns\\ly_snapshot\\error");
			File folder = new File("C:\\Code\\Snapshots-on-a-Distributed-Peer-to-Peer-System\\ly_snapshot\\error");
			File[] files = folder.listFiles();
			double collectTimeSum = 0.0;
			double collectCount = 0.0;
			int snapshotSizeSum = 0;
			
			for (File file: files) {
				try {
					Scanner sc = new Scanner(file);
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						if (line.indexOf("Snapshot collecting time:") != -1) {
							collectTimeSum += Double.parseDouble(line.substring(37, line.length()));
							collectCount += 1.0;
						}
						else if (line.indexOf("Size for snapshot") != 1) {
							String[] tempArr = line.split(" ");
							snapshotSizeSum += Integer.parseInt(tempArr[tempArr.length - 2]);
						}
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println("Average collcting time: " + (collectTimeSum / collectCount) + " seconds.");
			System.out.println("Average snapshot size: " + (snapshotSizeSum / AppConfig.getServentCount()) + " bytes.");
		}
	}
	
	/**
	 * The parameter for this function should be the name of a directory that
	 * contains a servent_list.properties file which will describe our distributed system.
	 */
	private static void startServentTest(String testName) {
		List<Process> serventProcesses = new ArrayList<>();
		
		AppConfig.readConfig(testName+"/servent_list.properties");
		
		AppConfig.timestampedStandardPrint("Starting multiple servent runner. Time Limit: " + AppConfig.timeLimit + " seconds.");
		
		int serventCount = AppConfig.getServentCount();
		
		for(int i = 0; i < serventCount; i++) {
			try {
				ProcessBuilder builder = new ProcessBuilder("java", "-cp", "bin/", "app.ServentMain",
						testName+"/servent_list.properties", String.valueOf(i));
				
				//We use files to read and write.
				//System.out, System.err and System.in will point to these files.
				builder.redirectOutput(new File(testName+"/output/servent" + i + "_out.txt"));
				builder.redirectError(new File(testName+"/error/servent" + i + "_err.txt"));
				builder.redirectInput(new File(testName+"/input/servent" + i + "_in.txt"));
				
				//Starts the servent as a completely separate process.
				Process p = builder.start();
				serventProcesses.add(p);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Thread t = new Thread(new ServentCLI(serventProcesses));
		
		t.start(); //CLI thread waiting for user to type "stop".
		
		for (Process process : serventProcesses) {
			try {
				process.waitFor(); //Wait for graceful process finish.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		AppConfig.timestampedStandardPrint("All servent processes finished. Type \"stop\" to exit.");
	}
	
	public static void main(String[] args) {
		startServentTest("ly_snapshot");
		
	}

}
