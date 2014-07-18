/* Brian Vermillion
 * COP 4600 Summer 2014
 * Program #1 - Uni-processor Scheduling Simulation
 * July 07 2014
 * 
 * Program simulates different uni-processor scheduling algorithms
 * then draws a gantt chart in a window
 */


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Thread.State;
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.swing.*;

/* Three different states for the Gantt Chart Tiles */
enum STATE { WAITING, RUNNING, EMPTY };

/* Stores the Gantt chart as a 2D array of STATES 
 * with the wait times, run times and turnaround 
 * times. Constructor takes in the number of processes. 
 */
class StateData {
	public STATE[][] s;
	public int[] run;
	public int[] wait;
	public int[] turn;
	
	public StateData(int n) {
		run = new int[n];
		wait = new int[n];
		turn = new int[n];
	}
}

/* Stores all the necessary information for a process to execute.
 * In addition, it includes a pointer to the next process and 
 * a function to clone the entire process list
 */
class Process {
	int id;
	int arrival;
	int etime;
	Process Next;
	
	Process cloneList() {
		Process p = new Process();
		p.id = id;
		p.arrival = arrival;
		p.etime = etime;
		
		if (Next != null)
			p.Next = Next.cloneList();
		else
			p.Next = null;
		
		return p;
	}
}

/* Processor class to simulate the different queues a processor has.
 * executing = currently running process (can only have one)
 * ready = queue to hold all the process ready to be executed
 * toReady = queue to hold all the process that have not arrived yet
 * dispatch() = takes the first process from the ready queue and executes it
 * *The scheduling algorithm is in charge of maintaining all of these queues*
 */
class Processor {
	Process executing;
	Process ready;
	Process toReady;
	boolean interupt;
	
	public boolean dispatch() {
		if (executing != null || ready == null)
			return false;
		
		executing = ready;
		ready = ready.Next;
		executing.Next = null;
		return true;
	}
	
	public Processor clone() {
		Processor p = new Processor();
				
		if (executing != null)
			p.executing = executing.cloneList();
		if (ready != null)
			p.ready = ready.cloneList();
		if (toReady != null)
			p.toReady = toReady.cloneList();		
		
		return p;
	}
}

public class Gantt implements ActionListener{
	///	GUI Variables 	///
	private JFrame window;
	private JPanel data;
	private Graph graph;
	private JTextField qnt;
	JComboBox<String> type;
	private int width = 900;
	private int height = 400;
	///////////////////////
	
	private int tq=1;		//"Time Quantum"
	private Processor proc;	//The processor.
	private int time;		//Total processor executing time.
	private int numProc;	//Number of processes.
	private final String[] types = {"FCFS", "Round Robin", "SPN"};
	
	Gantt() {
		inputFile();	
		BuildGUI();
		
		graph.setState(FCFS());
		graph.repaint();
	}

	public static void main(String[] args) {
		Gantt test = new Gantt();
	}
	
	//First Come First Serve
	private StateData FCFS() {
		StateData state = new StateData(numProc);
		state.s = new STATE[time][numProc];
		Processor cp = proc.clone();		//Creates a copy of the processor
		
		//increments through the time
		for (int i=0; i<time; i++) {
			Arrays.fill(state.s[i], STATE.EMPTY);
			Process tmp = null;
		
			/*Checks for process to ad to the ready queue, then
			 * adds them to the back of the queue
			 */
			if (cp.toReady != null && cp.toReady.arrival <= i) {				
				if (cp.ready == null) {
					cp.ready = cp.toReady;
					cp.toReady = cp.toReady.Next;
					cp.ready.Next = null;
				}
				else {
					tmp = returnTail(cp.ready);			
				
					tmp.Next = cp.toReady;
					cp.toReady = cp.toReady.Next;
					tmp.Next.Next = null;
				}
			}
			
			/*Continues the timer if there is nothing executing and
			 * the ready queue is empty
			 */
			if (cp.executing == null && cp.ready == null)
				continue;
			
			//If the processor is empty, add a process
			else if (cp.executing == null) {
				if (!cp.dispatch())
					continue;
			}
			//Removes a process when it's done executing
			else if (cp.executing.etime == 0) {
				cp.executing = null;
				if (!cp.dispatch())
					continue;
			}
			
			/* Runs through the ready queue and counts
			 * the number of waiting processes
			 */
			tmp = cp.ready;
			while (tmp != null) {
				state.s[i][tmp.id - 1] = STATE.WAITING;
				state.wait[tmp.id - 1]++;
				state.turn[tmp.id - 1]++;
				tmp = tmp.Next;
			}
			
			//Saves the state information
			state.s[i][cp.executing.id - 1] = STATE.RUNNING;
			state.run[cp.executing.id - 1]++;
			state.turn[cp.executing.id - 1]++;
			cp.executing.etime--;
				
		}
		
		return state;
	}
	
	private StateData RR(int quantum) {
		StateData state = new StateData(numProc);
		state.s = new STATE[time][numProc];
		Processor cp = proc.clone();

		
		for (int i=0; i<time; i++) {
			Arrays.fill(state.s[i], STATE.EMPTY);
			Process tmp = null;						
			
			/*Checks for process to ad to the ready queue, then
			 * adds them to the back of the queue
			 */
			if (cp.toReady != null && cp.toReady.arrival <= i) {
								
				if (cp.ready == null) {
					cp.ready = cp.toReady;
					cp.toReady = cp.toReady.Next;
					cp.ready.Next = null;
				}
				else {
					tmp = returnTail(cp.ready);	
				
					tmp.Next = cp.toReady;
					cp.toReady = cp.toReady.Next;
					tmp.Next.Next = null;
				}
			}
			
			/*Continues the timer if there is nothing executing and
			 * the ready queue is empty
			 */
			if (cp.executing == null && cp.ready == null) 
				continue;
			
			//Puts a process in the processor if it is empty
			else if (cp.executing == null) {
				if (!cp.dispatch())
					continue;
			}
			//Removes a process when it is done
			else if (cp.executing.etime == 0) {
				cp.executing = null;
				if (!cp.dispatch())
					continue;
			}
			/* Creates a interrupt at regular intervals which
			 * cause a executing process to be removed from 
			 * the processor and put at the back of the ready
			 * queue
			 */
			else if (i%quantum == 0) {
				tmp = returnTail(cp.ready);
				
				if (tmp != null) {
					tmp.Next = cp.executing;
					cp.executing = null;
					if (!cp.dispatch())
						continue;
				}
			}
			
			//Counts all the waiting processes
			tmp = cp.ready;
			while (tmp != null) {
				state.s[i][tmp.id - 1] = STATE.WAITING;
				state.wait[tmp.id - 1]++;
				state.turn[tmp.id - 1]++;
				tmp = tmp.Next;
			}
			
			//Continues timer if there is nothing to execute
			if (cp.executing == null)
				continue;
			
			//Saves State information
			state.s[i][cp.executing.id - 1] = STATE.RUNNING;
			state.run[cp.executing.id - 1]++;
			state.turn[cp.executing.id - 1]++;
			cp.executing.etime--;
				
		}
		
		return state;
	}
	
	private StateData SPN() {
		StateData state = new StateData(numProc);
		state.s = new STATE[time][numProc];
		Processor cp = proc.clone();
		
		for (int i=0; i<time; i++) {
			Arrays.fill(state.s[i], STATE.EMPTY);
			Process tmp = null;
		
			//Adds processes to the ready queue
			if (cp.toReady != null && cp.toReady.arrival <= i) {				
				if (cp.ready == null) {
					cp.ready = cp.toReady;
					cp.toReady = cp.toReady.Next;
					cp.ready.Next = null;
				}
				else {
					tmp = cp.toReady;
					cp.toReady = cp.toReady.Next;
					tmp.Next = null;
					
					/*If its execution time is less than the first process
					 * put it at the front
					 */
					if (tmp.etime < cp.ready.etime) {
						tmp.Next = cp.ready;
						cp.ready = tmp;
					}						
						
					
					/* Inserts process into ready queue in sorted
					 * order by execution time
					 */
					Process tmp2 = cp.ready;
					Process tmp3 = cp.ready.Next;												
						
					while (tmp2 != null) {
						if (tmp3 == null) {
							tmp2.Next = tmp;	
							break;
						}
							
						if (tmp.etime < tmp3.etime) {
							tmp2.Next = tmp;
							tmp.Next = tmp3;
							break;
						}
						tmp2 = tmp3;
						tmp3 = tmp3.Next;
							
					}
					

				}
			}
			
			//Continues if there is nothing left to execute
			if (cp.executing == null && cp.ready == null)
				continue;
			
			//Puts a process in a empty processor
			else if (cp.executing == null) {
				 if (!cp.dispatch())
					 continue;
			}
			
			//Removes a finished processor
			else if (cp.executing.etime == 0) {
				cp.executing = null;
				if (!cp.dispatch())
					continue;
			}
			
			//Counts waiting time
			tmp = cp.ready;
			while (tmp != null) {
				state.s[i][tmp.id - 1] = STATE.WAITING;
				state.wait[tmp.id - 1]++;
				state.turn[tmp.id - 1]++;
				tmp = tmp.Next;
			}
			
			
			//Saves states
			state.s[i][cp.executing.id - 1] = STATE.RUNNING;
			state.run[cp.executing.id - 1]++;
			state.turn[cp.executing.id - 1]++;
			cp.executing.etime--;
				
		}
		
		return state;
	}

	
	
	/* Returns the last Process in a process lost */
	private Process returnTail(Process p) {
		if (p == null)
			return null;
		
		while (p.Next != null) {
			p = p.Next;
		}
		
		return p;
	}
	

	
	/* Reads from file and puts all the process in to the toReady queue of the processor */
	private void inputFile() {
		BufferedReader r = null;
		proc = new Processor();
		
		try {
			r = new BufferedReader(new FileReader("schedule.dat"));
		} catch (FileNotFoundException e) {
			System.out.println("Failed to open file: schedule.dat");
			System.exit(-1);
		}
		
		try {
			numProc = Integer.valueOf(r.readLine());
			time = Integer.valueOf(r.readLine());
			Process tmp, last, head = new Process();
			tmp = head;
			
			for (int i=0; i<numProc; i++) {
				String[] s = r.readLine().split(" ");
				tmp.id = Integer.parseInt(s[0]);
				tmp.arrival = Integer.parseInt(s[1]);				
				tmp.etime = Integer.parseInt(s[2]);	
				
				if (i+1 == numProc)
					tmp.Next = null;
				else {
					tmp.Next = new Process();
					tmp = tmp.Next;
				}
			}
			
			proc.toReady = head;
						
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	
	/*****************************************************************
	 * 				Everything Below is GUI related                  *
	 *****************************************************************/
	private void BuildGUI() {
		createWindow();
		setPanels(window.getContentPane());
		setDataComponents();
		window.pack();
		window.setVisible(true);
	}
	
	private void createWindow() {
		window = new JFrame("Gantt chart");
		window.setLayout(new BorderLayout());
		window.setSize(width, height);
		window.setResizable(false);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
	}
	
	private void setPanels(Container pane) {
		data = new JPanel(new BorderLayout());
		data.setBackground(Color.lightGray);
		data.setLayout(new FlowLayout());
		
		graph = new Graph(numProc, time);
		graph.setLayout(null);
		graph.setPreferredSize(new Dimension(window.getWidth(), window.getHeight()*4/5));
		graph.setVisible(true);
		
		
		pane.add(data, BorderLayout.PAGE_END);
		pane.add(graph, BorderLayout.PAGE_START);
				
	}
	
	private void setDataComponents() {
		type = new JComboBox<String>(types);
		type.addActionListener(this);
		data.add(type);
		data.add(Box.createRigidArea(new Dimension(10, 0)));
		
		JLabel qText = new JLabel("(Q) Time Quantum:");
		data.add(qText);
		
		qnt = new JTextField(2);
		qnt.setText(Integer.toString(tq));
		data.add(qnt);
		
		data.add(Box.createRigidArea(new Dimension(10, 0)));
		JButton enter = new JButton("Update Q");
		enter.addActionListener(this);
		data.add(enter);
		
		data.add(Box.createRigidArea(new Dimension(20, 0)));
		JLabel shortcuts = new JLabel("( W = Wait   T = Turnaround   NT = Nomalized Turnaround   A = Average )");
		shortcuts.setFont(new Font("Arial", Font.PLAIN, 13));
		data.add(shortcuts);
		
	}
	
	

	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			try {
				tq = Integer.parseInt(qnt.getText());
			}
			catch (NumberFormatException e2) {
				
			}
		}
		
		
		String s = type.getSelectedItem().toString();
		
		switch (s) {
		case "FCFS":
			graph.setState(FCFS());			
			break;
		case "Round Robin":
			graph.setState(RR(tq));
			break;
		case "SPN":
			graph.setState(SPN());
			break;
		default:
			break;
		}
		
		graph.repaint();
		
		
	}

}


class Graph extends JPanel {
	private StateData data = null;
	private int time;
	private int count;
	private int fontSize;
	private Color[] choices = { Color.RED,
								Color.BLUE,
								Color.GREEN,
								Color.CYAN,
								Color.ORANGE,
								Color.PINK,
								Color.MAGENTA,
								Color.YELLOW };
	
	
	Graph(int c, int t ) {	
		time = t;
		count = c;
		fontSize=16;
		
	};	
	
	public void setState(StateData s) {
		this.data = s;
	}
	

	private void draw(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(
		        RenderingHints.KEY_TEXT_ANTIALIASING,
		        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		// Scales the font size as the number of process increase 
		// and the box sizes decrease
		
		g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
		while (g2.getFontMetrics().getHeight() >= (this.getPreferredSize().height - 40)/count) {
			g2.setFont(new Font("Arial", Font.PLAIN, --fontSize));
		}
		
		//Builds the box that holds wait time and turnaround time
		Dimension boxSize = dataBox(g2);
		//Starting point for the left box
		Point boxStart = new Point(10,10);
		
				
		//Makes the box size for the table
		int subBoxWidth = (boxSize.width - 20) / time;
		int subBoxHeight = (boxSize.height - 25) / count;
		boxStart.setLocation(boxStart.x + 20, boxStart.y + 25);
		
		//Draws the gnatt chart
		if (data != null)
			drawState(g2, boxStart, subBoxWidth, subBoxHeight);
		
		int i;
		g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
		
		//Draws the horizontal numbers on the gantt chart
		for (i=1; i<=time; i++) {
			g2.setColor(Color.GRAY);
			g2.drawLine(boxStart.x + subBoxWidth * i, boxStart.y - 25, boxStart.x + subBoxWidth * i, boxStart.y - 25 + boxSize.height);
			
			g2.setColor(Color.BLACK);
			String s = Integer.toString(i);
			int w = g2.getFontMetrics().stringWidth(s);
			w = (subBoxWidth - w) / 2;
			g2.drawString(Integer.toString(i), boxStart.x + subBoxWidth*(i-1) + w, boxStart.y - 5);
		}
		
		//Gets the pixel difference at the right end of the table so it can be cut off
		i--;
		boxStart.setLocation(boxStart.x - 20, boxStart.y - 25);
		int pixDiffx = (boxStart.x + boxSize.width)-(boxStart.x + i*subBoxWidth + 20);
		boxSize.setSize(boxSize.width - pixDiffx, boxSize.height);
		
		//Creates the Vertical lines for each process
		
		for (i=1; i<=count; i++) {
			g2.setColor(Color.GRAY);
			int y1 = boxStart.y + 25 +(i * subBoxHeight);
			g2.drawLine(boxStart.x, y1, boxStart.x + boxSize.width, y1);
	
			g2.setColor(Color.BLACK);
			String s = Integer.toString(i);
			int w = g2.getFontMetrics().stringWidth(s);
			w = (20 - w) / 2;
			int h = g2.getFontMetrics().getHeight();
			h = (subBoxHeight - h) / 2;
			g2.drawString(s, boxStart.x + w, y1 - h - 2);
		}
		
		//Draws the box, the row that holds the time and the column that holds the process number
		g2.draw3DRect(boxStart.x, boxStart.y, boxSize.width, boxSize.height, true);
		g2.drawLine(boxStart.x, boxStart.y+25, boxSize.width+10, boxStart.y+25);
		g2.drawLine(boxStart.x+20, boxStart.y, boxStart.x+20, boxStart.y+boxSize.height);

		
	}
	
	private Dimension dataBox(Graphics2D g2) {
		String Z = null;
		int width = 140;
		int height = this.getPreferredSize().height-20;
		int x = this.getPreferredSize().width - width - 10;
		int y = 10;
		int i;
		int fw = 0;
		int fh = 0;
		int subBoxHeight = (height - 20) / (count+1);
		int subBoxWidth = (width - 20) / 3;

		//Line for above text
		g2.drawLine(x, y+25, x+width, y+25);
		//Line for numbers
		g2.drawLine(x+20, y, x+20, y+height);
		
		//Gray lines to separate different data areas
		g2.setColor(Color.LIGHT_GRAY);
		for(i=1; i<=2; i++) {
			g2.drawLine(x+20+subBoxWidth*i, y, x+20+subBoxWidth*i, y+height);
		}
		
		
		g2.setColor(Color.BLACK);
		g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
		
		
		//Adds the characters to the top row of the data box
		int j = x+20;
		String[] s = {"W", "T", "NT"};
		for (i=1; i<=3; i++) {
			int SWidth = g2.getFontMetrics().stringWidth( s[i-1] );
			g2.drawString(s[i-1], j+(40-SWidth)/2, y+20);
			j += 40;
		}
		
		
		
		//Draws the text on the left row of the data box
		for (i=1; i<=count; i++) {
	
			
			Z = Integer.toString(i);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (20 - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + fw, y + 25 - fh + (subBoxHeight * i));
		}
		
		Z = "A";
		g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
		fw = g2.getFontMetrics().stringWidth(Z);
		fw = (20 - fw) / 2;
		fh = g2.getFontMetrics().getHeight();
		fh = (subBoxHeight - fh) / 2;
		g2.drawString(Z, x + fw, y + 23 - fh + (subBoxHeight * i));
		
		g2.setFont(new Font("Arial", Font.PLAIN, fontSize));
		
		//Draws all the data for wait times
		for (i=1; i<=count; i++) {
			
			Z = Integer.toString(data.wait[i-1]);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + fw, y + 25 - fh + (subBoxHeight * i));
		}
		
		//Draws all the data for turnarround times
		for (i=1; i<=count; i++) {
					
			Z = Integer.toString(data.turn[i-1]);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + subBoxWidth + fw, y + 25 - fh + (subBoxHeight * i));
		}
		
		//Draws all the data for normalized turnaroud times
		for (i=1; i<=count; i++) {
					
			Z = String.format("%.1f", (data.turn[i-1] / ( (float) data.run[i-1])));
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + subBoxWidth*2 + fw, y + 25 - fh + (subBoxHeight * i));
		}
		
		//Computes the averages and prints
		{	int base = i;
			g2.setColor(Color.GREEN);
			Z = String.format("%.1f", sum(data.wait)/(float)count);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + fw, y + 23 - fh + (subBoxHeight * base));
			
			Z = String.format("%.1f", sum(data.turn)/(float)count);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + subBoxWidth + fw, y + 23 - fh + (subBoxHeight * base));
			
			float sum = 0;
			for (i=0; i<count; i++)
				sum += data.turn[i] / (float) data.run[i];
			
			Z = String.format("%.1f", sum/count);
			fw = g2.getFontMetrics().stringWidth(Z);
			fw = (subBoxWidth - fw) / 2;
			fh = g2.getFontMetrics().getHeight();
			fh = (subBoxHeight - fh) / 2;
			g2.drawString(Z, x + 20 + subBoxWidth*2 + fw, y + 23 - fh + (subBoxHeight * base));
		}
		//Draws the outer borders
		g2.setColor(Color.BLACK);
		g2.draw3DRect(x, y, width, height, true);
		
		g2.setFont(new Font("Arial", Font.PLAIN, 16));
		return new Dimension(this.getPreferredSize().width - width - 30, height);
	}
	
	/* Draws in all the boxes of the graphs with different colors for 
	 * each process and a shaded gray box for wait time
	 */
	
	public void drawState(Graphics2D g2, Point point, int w, int h) {
		int x = point.x;
		int y = point.y;
				
		for (int i=0; i<data.s.length; i++) {
			for (int j=0; j<data.s[i].length; j++) {
				if (data.s[i][j] != STATE.EMPTY && data.s[i][j] != null) {
					if (data.s[i][j] == STATE.RUNNING)
						g2.setColor(choices[j%8]);
					else
						g2.setColor(Color.LIGHT_GRAY);
			
					g2.fillRect(x +(i * w), y + (j * h), w, h);
				}
				
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw(g);
	}
	
	public int sum(int[] n) {
		int sum = 0;
		for (int i=0; i<n.length; i++)
			sum += n[i];
		return sum;
	}
	
}


