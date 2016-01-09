import javax.swing.*;
import java.awt.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class BeatBox implements Serializable {
	static BeatBox beatBox;
	JFrame frame;
	JPanel panel;
	ArrayList<JCheckBox> checkBoxList;
	transient Sequencer sequencer;
	transient Sequence sequence;
	transient Track track;

	String[] instrumentNames = {
		"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", 
		"Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", 
		"Whistle", "Low Conge", "Cowbell", "Vibraslap", "Low-Mid Tom", 
		"High Agogo", "Open High Conga" 
	};

	int[] instruments = {
		35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63
	};

	public static void main(String[] args) {
		beatBox = new BeatBox();

		try{ 

			FileInputStream f = new FileInputStream("beatbox.sav"); 
			ObjectInputStream os = new ObjectInputStream(f);

			beatBox = (BeatBox) os.readObject();
			os.close();
		}catch(Exception e) {
			e.printStackTrace();
		}

		beatBox.run();
	}

	public void run() {
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("Start");
		JButton stop = new JButton("Stop");
		JButton upTempo = new JButton("Tempo Up");
		JButton downTempo = new JButton("Tempo Down");
		JButton save = new JButton("Save");
		JButton restore = new JButton("Restore");

		
		frame = new JFrame("BeatBox by Matz");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		
		checkBoxList = new ArrayList<>();
		start.addActionListener(new MyStartListener());
		stop.addActionListener(new MyStopListener());
		upTempo.addActionListener(new MyUpTempoListener());
		downTempo.addActionListener(new MyDownTempoListener());
		save.addActionListener(new MySendListener());
		restore.addActionListener(new MyReadInListener());

		buttonBox.add(start);
		buttonBox.add(stop);
		buttonBox.add(upTempo);
		buttonBox.add(downTempo);
		buttonBox.add(save);
		buttonBox.add(restore);

		for(int i=0; i < instruments.length; i++) {
			nameBox.add(new JLabel(instrumentNames[i]));
		}

		background.add(BorderLayout.WEST, nameBox);
		background.add(BorderLayout.EAST, buttonBox);

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);

		JPanel mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		for(int i=0;i<256;i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkBoxList.add(c);
			mainPanel.add(c);

		}

		setUpMidi();

		frame.getContentPane().add(background);
		frame.setBounds(50, 50, 300, 300);
		frame.pack();
		frame.setVisible(true);


	}

	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();

			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void buildTrackAndStart() {
		int[] trackList = null;
		sequence.deleteTrack(track);

		track = sequence.createTrack();

		for(int i=0; i<16;i++) {
			trackList = new int[16];

			int key = instruments[i];

			for(int j=0; j<16; j++) {
				JCheckBox jc = (JCheckBox) checkBoxList.get(j + (16 * i));
				if(jc.isSelected()) {
					trackList[j]  = key;
				}else {
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);
			track.add(makeEvent(176,1,127,0,16));
		}

		track.add(makeEvent(129,9,1,0,15));

		try{ 
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	class MyStartListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			buildTrackAndStart();
		}
	}

	class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			sequencer.stop();
		}
	}

	class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 1.03));
		}
	}


	class MyDownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float) (tempoFactor * 0.97));
		}
	}

	public void makeTracks(int[] list) {
		for(int i=0;i<16;i++) {
			int key = list[i];

			if(key != 0) {
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;

		try {

			ShortMessage s = new ShortMessage();
			s.setMessage(comd, chan, one, two);
			event = new MidiEvent(s, tick);

		}catch(Exception e) {
			e.printStackTrace();
		}

		return event;
	}

	public class MySendListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = new boolean[256];

			for(int i=0;i<256;i++) {
				JCheckBox check = (JCheckBox) checkBoxList.get(i);
				if(check.isSelected()) { 
					checkboxState[i] = true;
				}	
			}

			try {
				FileOutputStream f = new FileOutputStream(new File("checkbox.ser"));
				ObjectOutputStream os = new ObjectOutputStream(f);

				os.writeObject(checkboxState);

				os.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public class MyReadInListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			boolean[] checkboxState = null;

			try {
				FileInputStream f = new FileInputStream(new File("checkbox.ser"));
				ObjectInputStream is = new ObjectInputStream(f);
				checkboxState = (boolean[]) is.readObject();
				is.close();
			}catch(Exception ex) {
				ex.printStackTrace();
			}

			for(int i=0;i<256;i++) {
				JCheckBox check = (JCheckBox) checkBoxList.get(i);

				if(checkboxState[i]) {
					check.setSelected(true);
				}else {
					check.setSelected(false);
				}
			}

			sequencer.stop();
			buildTrackAndStart();
		}
	}
}